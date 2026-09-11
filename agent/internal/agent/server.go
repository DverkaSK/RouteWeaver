package agent

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"time"
)

type Server struct {
	config     Config
	transports *TransportManager
}

func NewServer(config Config) *Server {
	return &Server{
		config:     config,
		transports: NewTransportManager(config.SocksListenAddress(), config.TunnelListenAddress()),
	}
}

func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/health", s.handleHealth)
	mux.HandleFunc("/routes/active", s.handleActiveRoute)
	mux.HandleFunc("/myip", s.handleMyIp)
	return loggingMiddleware(mux)
}

func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method_not_allowed", "Only GET is supported")
		return
	}

	writeJSON(w, http.StatusOK, map[string]any{
		"status":           "ok",
		"listen":           s.config.ListenAddress(),
		"socksListen":      s.config.SocksListenAddress(),
		"tunnelListen":     s.config.TunnelListenAddress(),
		"activeRoute":      s.transports.ActiveRoute(),
		"supportedModes":   []string{"SHADOWSOCKS"},
		"unsupportedModes": []string{"VLESS"},
	})
}

func (s *Server) handleActiveRoute(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		activeRoute := s.transports.ActiveRoute()
		if activeRoute == nil {
			writeJSON(w, http.StatusOK, map[string]any{
				"activeRoute": nil,
			})
			return
		}
		writeJSON(w, http.StatusOK, map[string]any{
			"activeRoute": activeRoute,
		})
	case http.MethodPost:
		var request SyncRequest
		if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
			writeError(w, http.StatusBadRequest, "invalid_json", "Malformed request body")
			return
		}

		if strings.TrimSpace(request.TargetServer.Host) == "" {
			writeError(w, http.StatusBadRequest, "missing_target_host", "targetServer.host is required")
			return
		}
		if request.TargetServer.Port <= 0 {
			writeError(w, http.StatusBadRequest, "missing_target_port", "targetServer.port must be positive")
			return
		}
		if strings.TrimSpace(request.Profile.ID) == "" {
			writeError(w, http.StatusBadRequest, "missing_profile_id", "profile.id is required")
			return
		}

		activeRoute, err := s.transports.ApplyRoute(r.Context(), request)
		if err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]any{
				"activeRoute": activeRoute,
				"error": map[string]string{
					"code":    "route_rejected",
					"message": err.Error(),
				},
			})
			return
		}
		writeJSON(w, http.StatusAccepted, map[string]any{
			"activeRoute": activeRoute,
		})
	case http.MethodDelete:
		s.transports.ClearRoute()
		writeJSON(w, http.StatusOK, map[string]any{
			"activeRoute": nil,
			"cleared":     true,
		})
	default:
		writeError(w, http.StatusMethodNotAllowed, "method_not_allowed", "Only GET, POST and DELETE are supported")
	}
}

func (s *Server) handleMyIp(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method_not_allowed", "Only GET is supported")
		return
	}

	if s.transports.ActiveRoute() == nil {
		writeError(w, http.StatusServiceUnavailable, "no_active_route", "No active proxy route")
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()

	httpClient := &http.Client{
		Transport: &http.Transport{
			DialContext: s.transports.DialContext,
		},
		Timeout: 10 * time.Second,
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, "http://checkip.amazonaws.com/", nil)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "request_build_failed", err.Error())
		return
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		writeError(w, http.StatusServiceUnavailable, "ip_fetch_failed", err.Error())
		return
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(io.LimitReader(resp.Body, 64))
	if err != nil {
		writeError(w, http.StatusInternalServerError, "read_failed", err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"ip": strings.TrimSpace(string(body))})
}

func loggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		_ = start
	})
}

func writeJSON(w http.ResponseWriter, statusCode int, payload any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	_ = json.NewEncoder(w).Encode(payload)
}

func writeError(w http.ResponseWriter, statusCode int, code string, message string) {
	writeJSON(w, statusCode, map[string]any{
		"error": map[string]string{
			"code":    code,
			"message": message,
		},
	})
}
