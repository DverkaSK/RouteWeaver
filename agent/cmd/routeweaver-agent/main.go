// Command routeweaver-agent runs the standalone RouteWeaver proxy agent.
//
// Deprecated: the agent is superseded by the proxy engine embedded in the
// RouteWeaver mod and is kept for reference only.
package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"routeweaver-agent/internal/agent"
	"syscall"
	"time"
)

func main() {
	cfg := agent.LoadConfigFromEnv()
	server := agent.NewServer(cfg)
	socksServer := agent.NewSocks5Server(cfg.SocksListenAddress(), server.TransportManager())
	tunnelServer := agent.NewTunnelServer(cfg.TunnelListenAddress(), server.TransportManager())

	httpServer := &http.Server{
		Addr:              cfg.ListenAddress(),
		Handler:           server.Routes(),
		ReadHeaderTimeout: 5 * time.Second,
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	go func() {
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := httpServer.Shutdown(shutdownCtx); err != nil {
			log.Printf("agent shutdown error: %v", err)
		}
	}()

	go func() {
		if err := socksServer.ListenAndServe(ctx); err != nil {
			log.Printf("socks server failed: %v", err)
			stop()
		}
	}()

	go func() {
		if err := tunnelServer.ListenAndServe(ctx); err != nil {
			log.Printf("tunnel server failed: %v", err)
			stop()
		}
	}()

	log.Printf("routeweaver-agent listening on %s", cfg.ListenAddress())
	err := httpServer.ListenAndServe()
	if err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("agent failed: %v", err)
	}
}
