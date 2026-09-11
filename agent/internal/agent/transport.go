package agent

import (
	"context"
	"errors"
	"fmt"
	"net"
	"strings"
	"sync"
	"time"

	"github.com/shadowsocks/go-shadowsocks2/core"
	ssSocks "github.com/shadowsocks/go-shadowsocks2/socks"
)

type TransportManager struct {
	mu                sync.RWMutex
	localSocksListen  string
	localTunnelListen string
	activeRoute       *ActiveRoute
	dialer            Dialer
}

type Dialer interface {
	DialContext(ctx context.Context, network string, address string) (net.Conn, error)
}

func NewTransportManager(localSocksListen string, localTunnelListen string) *TransportManager {
	return &TransportManager{
		localSocksListen:  localSocksListen,
		localTunnelListen: localTunnelListen,
	}
}

func (m *TransportManager) ActiveRoute() *ActiveRoute {
	m.mu.RLock()
	defer m.mu.RUnlock()

	if m.activeRoute == nil {
		return nil
	}

	route := *m.activeRoute
	return &route
}

func (m *TransportManager) ApplyRoute(ctx context.Context, request SyncRequest) (*ActiveRoute, error) {
	route := &ActiveRoute{
		TargetServer:      request.TargetServer,
		Profile:           request.Profile,
		UpdatedAt:         time.Now().UTC(),
		Applied:           false,
		LocalSocksListen:  m.localSocksListen,
		LocalTunnelListen: m.localTunnelListen,
		Transport: TransportInfo{
			Type:    strings.ToLower(request.Profile.Mode),
			Status:  "pending",
			Message: "Configuring transport",
		},
	}

	if err := validateRouteRequest(request); err != nil {
		route.Transport.Status = "rejected"
		route.Transport.Message = err.Error()
		m.storeRoute(route, nil)
		return route, err
	}

	switch strings.ToUpper(strings.TrimSpace(request.Profile.Mode)) {
	case "SHADOWSOCKS":
		dialer, err := newShadowsocksDialer(request.Profile)
		if err != nil {
			route.Transport.Status = "error"
			route.Transport.Message = err.Error()
			m.storeRoute(route, nil)
			return route, err
		}

		select {
		case <-ctx.Done():
			route.Transport.Status = "cancelled"
			route.Transport.Message = ctx.Err().Error()
			m.storeRoute(route, nil)
			return route, ctx.Err()
		default:
		}

		route.Applied = true
		route.Transport.Type = "shadowsocks"
		route.Transport.Status = "ready"
		route.Transport.Message = "Local SOCKS5 and tunnel listeners are bound to the selected Shadowsocks profile."
		m.storeRoute(route, dialer)
		return route, nil
	default:
		err := fmt.Errorf("proxy mode %q is not supported yet", request.Profile.Mode)
		route.Transport.Status = "unsupported"
		route.Transport.Message = err.Error()
		m.storeRoute(route, nil)
		return route, err
	}
}

func (m *TransportManager) DialContext(ctx context.Context, network string, address string) (net.Conn, error) {
	m.mu.RLock()
	dialer := m.dialer
	m.mu.RUnlock()

	if dialer == nil {
		return nil, errors.New("no active transport is configured")
	}

	return dialer.DialContext(ctx, network, address)
}

func (m *TransportManager) DialActiveTarget(ctx context.Context, network string) (net.Conn, *ActiveRoute, error) {
	m.mu.RLock()
	dialer := m.dialer
	activeRoute := m.activeRoute
	m.mu.RUnlock()

	if dialer == nil || activeRoute == nil {
		return nil, nil, errors.New("no active transport is configured")
	}

	address := net.JoinHostPort(activeRoute.TargetServer.Host, fmt.Sprintf("%d", activeRoute.TargetServer.Port))
	conn, err := dialer.DialContext(ctx, network, address)
	if err != nil {
		return nil, activeRoute, err
	}
	return conn, activeRoute, nil
}

func (m *TransportManager) ClearRoute() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.activeRoute = nil
	m.dialer = nil
}

func (m *TransportManager) storeRoute(route *ActiveRoute, dialer Dialer) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.activeRoute = route
	m.dialer = dialer
}

func validateRouteRequest(request SyncRequest) error {
	if strings.TrimSpace(request.TargetServer.Host) == "" {
		return errors.New("targetServer.host is required")
	}
	if request.TargetServer.Port <= 0 {
		return errors.New("targetServer.port must be positive")
	}
	if strings.TrimSpace(request.Profile.ID) == "" {
		return errors.New("profile.id is required")
	}
	if !request.Profile.Enabled {
		return errors.New("profile is disabled")
	}
	return nil
}

type shadowsocksDialer struct {
	server string
	cipher core.Cipher
}

func newShadowsocksDialer(profile ProxyProfile) (Dialer, error) {
	if strings.TrimSpace(profile.Host) == "" {
		return nil, errors.New("profile.host is required for Shadowsocks")
	}
	if profile.Port <= 0 {
		return nil, errors.New("profile.port must be positive for Shadowsocks")
	}
	if strings.TrimSpace(profile.Method) == "" {
		return nil, errors.New("profile.method is required for Shadowsocks")
	}
	if strings.TrimSpace(profile.Password) == "" {
		return nil, errors.New("profile.password is required for Shadowsocks")
	}

	cipher, err := core.PickCipher(profile.Method, nil, profile.Password)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize Shadowsocks cipher: %w", err)
	}

	return &shadowsocksDialer{
		server: net.JoinHostPort(profile.Host, fmt.Sprintf("%d", profile.Port)),
		cipher: cipher,
	}, nil
}

func (d *shadowsocksDialer) DialContext(ctx context.Context, network string, address string) (net.Conn, error) {
	if network != "tcp" {
		return nil, fmt.Errorf("unsupported network %q", network)
	}

	var base net.Dialer
	conn, err := base.DialContext(ctx, "tcp", d.server)
	if err != nil {
		return nil, fmt.Errorf("failed to reach Shadowsocks server %s: %w", d.server, err)
	}

	shadowConn := d.cipher.StreamConn(conn)
	target := ssSocks.ParseAddr(address)
	if target == nil {
		shadowConn.Close()
		return nil, fmt.Errorf("invalid target address %q", address)
	}

	if _, err := shadowConn.Write(target); err != nil {
		shadowConn.Close()
		return nil, fmt.Errorf("failed to send Shadowsocks target address: %w", err)
	}

	return shadowConn, nil
}
