package agent

func (s *Server) TransportManager() *TransportManager {
	return s.transports
}
