package agent

import "os"

type Config struct {
	Host       string
	Port       string
	SocksHost  string
	SocksPort  string
	TunnelHost string
	TunnelPort string
}

func LoadConfigFromEnv() Config {
	host := os.Getenv("PROXY_AGENT_HOST")
	if host == "" {
		host = "127.0.0.1"
	}

	port := os.Getenv("PROXY_AGENT_PORT")
	if port == "" {
		port = "39751"
	}

	socksHost := os.Getenv("PROXY_AGENT_SOCKS_HOST")
	if socksHost == "" {
		socksHost = "127.0.0.1"
	}

	socksPort := os.Getenv("PROXY_AGENT_SOCKS_PORT")
	if socksPort == "" {
		socksPort = "39752"
	}

	tunnelHost := os.Getenv("PROXY_AGENT_TUNNEL_HOST")
	if tunnelHost == "" {
		tunnelHost = "127.0.0.1"
	}

	tunnelPort := os.Getenv("PROXY_AGENT_TUNNEL_PORT")
	if tunnelPort == "" {
		tunnelPort = "39753"
	}

	return Config{
		Host:       host,
		Port:       port,
		SocksHost:  socksHost,
		SocksPort:  socksPort,
		TunnelHost: tunnelHost,
		TunnelPort: tunnelPort,
	}
}

func (c Config) ListenAddress() string {
	return c.Host + ":" + c.Port
}

func (c Config) SocksListenAddress() string {
	return c.SocksHost + ":" + c.SocksPort
}

func (c Config) TunnelListenAddress() string {
	return c.TunnelHost + ":" + c.TunnelPort
}
