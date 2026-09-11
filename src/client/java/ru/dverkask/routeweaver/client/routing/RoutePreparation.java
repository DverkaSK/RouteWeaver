package ru.dverkask.routeweaver.client.routing;

public record RoutePreparation(RoutingDecision decision, boolean agentSynced) {
    public boolean shouldRedirectToAgentTunnel() {
        return decision != null && decision.shouldUseProxy() && agentSynced;
    }
}
