package agent

import (
	"context"
	"errors"
	"io"
	"log"
	"net"
	"os"
	"sync"
	"time"

	ssSocks "github.com/shadowsocks/go-shadowsocks2/socks"
)

type Socks5Server struct {
	listenAddress string
	manager       *TransportManager
}

func NewSocks5Server(listenAddress string, manager *TransportManager) *Socks5Server {
	return &Socks5Server{
		listenAddress: listenAddress,
		manager:       manager,
	}
}

func (s *Socks5Server) ListenAndServe(ctx context.Context) error {
	listener, err := net.Listen("tcp", s.listenAddress)
	if err != nil {
		return err
	}
	defer listener.Close()

	go func() {
		<-ctx.Done()
		_ = listener.Close()
	}()

	log.Printf("routeweaver-agent SOCKS5 listening on %s", s.listenAddress)
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

func (s *Socks5Server) handleConn(clientConn net.Conn) {
	defer clientConn.Close()

	targetAddr, err := ssSocks.Handshake(clientConn)
	if err != nil {
		log.Printf("socks handshake failed from %s: %v", clientConn.RemoteAddr(), err)
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	upstreamConn, err := s.manager.DialContext(ctx, "tcp", targetAddr.String())
	if err != nil {
		log.Printf("socks dial failed for %s via active transport: %v", targetAddr.String(), err)
		return
	}
	defer upstreamConn.Close()

	log.Printf("socks proxy %s <-> %s", clientConn.RemoteAddr(), targetAddr.String())
	if err := relay(clientConn, upstreamConn); err != nil {
		log.Printf("socks relay error: %v", err)
	}
}

func relay(left net.Conn, right net.Conn) error {
	var err error
	var err1 error
	var wg sync.WaitGroup
	const wait = 5 * time.Second

	wg.Add(1)
	go func() {
		defer wg.Done()
		_, err1 = io.Copy(right, left)
		_ = right.SetReadDeadline(time.Now().Add(wait))
	}()

	_, err = io.Copy(left, right)
	_ = left.SetReadDeadline(time.Now().Add(wait))
	wg.Wait()

	if err1 != nil && !errors.Is(err1, os.ErrDeadlineExceeded) {
		return err1
	}
	if err != nil && !errors.Is(err, os.ErrDeadlineExceeded) {
		return err
	}
	return nil
}
