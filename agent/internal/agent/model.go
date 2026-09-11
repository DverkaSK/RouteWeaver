package agent

import "time"

type SyncRequest struct {
	TargetServer TargetServer `json:"targetServer"`
	Profile      ProxyProfile `json:"profile"`
}

type TargetServer struct {
	Host string `json:"host"`
	Port int    `json:"port"`
}

type ProxyProfile struct {
	ID         string `json:"id"`
	Name       string `json:"name"`
	Mode       string `json:"mode"`
	Host       string `json:"host"`
	Port       int    `json:"port"`
	Username   string `json:"username"`
	Password   string `json:"password"`
	Method     string `json:"method"`
	VlessFlow  string `json:"vlessFlow"`
	UUID       string `json:"uuid"`
	ServerName string `json:"serverName"`
	Enabled    bool   `json:"enabled"`
}

type ActiveRoute struct {
	TargetServer      TargetServer  `json:"targetServer"`
	Profile           ProxyProfile  `json:"profile"`
	UpdatedAt         time.Time     `json:"updatedAt"`
	Applied           bool          `json:"applied"`
	Transport         TransportInfo `json:"transport"`
	LocalSocksListen  string        `json:"localSocksListen"`
	LocalTunnelListen string        `json:"localTunnelListen"`
}

type TransportInfo struct {
	Type    string `json:"type"`
	Status  string `json:"status"`
	Message string `json:"message"`
}
