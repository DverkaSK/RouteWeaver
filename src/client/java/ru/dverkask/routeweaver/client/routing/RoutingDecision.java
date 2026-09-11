package ru.dverkask.routeweaver.client.routing;

import ru.dverkask.routeweaver.client.config.ProxyProfile;
import ru.dverkask.routeweaver.client.config.RoutingRule;

public record RoutingDecision(ServerAddress serverAddress, RoutingRule rule, ProxyProfile profile) {
    public boolean shouldUseProxy() {
        return rule != null && profile != null;
    }
}
