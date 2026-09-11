package agent

import (
	"context"
	"errors"
	"log"
	"net"
	"time"
)

type TunnelServer struct {
	listenAddress string
	manager       *TransportManager
}

func NewTunnelServer(listenAddress string, manager *TransportManager) *TunnelServer {
	return &TunnelServer{
		listenAddress: listenAddress,
		manager:       manager,
	}
}

func (s *TunnelServer) ListenAndServe(ctx context.Context) error {
	listener, err := net.Listen("tcp", s.listenAddress)
	if err != nil {
		return err
	}
	defer listener.Close()

	go func() {
		<-ctx.Done()
		_ = listener.Close()
	}()

	log.Printf("routeweaver-agent tunnel listening on %s", s.listenAddress)
	for {
		conn, err := listener.Accept()
		if err != nil {
			if ctx.Err() != nil {
				return nil
			}
			var netErr net.Error
			if errors.As(err, &netErr) && netErr.Temporary() {
				continue
			}
			return err
		}

		go s.handleConn(conn)
	}
}

func (s *TunnelServer) handleConn(clientConn net.Conn) {
	defer clientConn.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	upstreamConn, route, err := s.manager.DialActiveTarget(ctx, "tcp")
	if err != nil {
		log.Printf("tunnel dial failed via active transport: %v", err)
		return
	}
	defer upstreamConn.Close()

	log.Printf("tunnel proxy %s <-> %s:%d", clientConn.RemoteAddr(), route.TargetServer.Host, route.TargetServer.Port)
	if err := relay(clientConn, upstreamConn); err != nil {
		log.Printf("tunnel relay error: %v", err)
	}
}
