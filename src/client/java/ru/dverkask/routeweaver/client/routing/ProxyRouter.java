package ru.dverkask.routeweaver.client.routing;

import ru.dverkask.routeweaver.client.config.ProxyConfig;
import ru.dverkask.routeweaver.client.config.ProxyProfile;
import ru.dverkask.routeweaver.client.config.RoutingRule;

import java.util.HashMap;
import java.util.Map;

public class ProxyRouter {
    public RoutingDecision decide(ProxyConfig config, ServerAddress serverAddress) {
        if (config == null || serverAddress == null) {
            return new RoutingDecision(serverAddress, null, null);
        }

        Map<String, ProxyProfile> profilesById = new HashMap<>();
        for (ProxyProfile profile : config.getProfiles()) {
            if (profile != null && profile.getId() != null && !profile.getId().isBlank() && profile.isEnabled()) {
                profilesById.put(profile.getId(), profile);
            }
        }

        for (RoutingRule rule : config.getRules()) {
            if (rule == null || !rule.isEnabled()) {
                continue;
            }
            if (!ServerPatternMatcher.matches(rule.getServerPattern(), serverAddress.host())) {
                continue;
            }
            if (rule.getPort() > 0 && rule.getPort() != serverAddress.port()) {
                continue;
            }

            ProxyProfile profile = profilesById.get(rule.getProxyProfileId());
            if (profile != null) {
                return new RoutingDecision(serverAddress, rule, profile);
            }
        }

        return new RoutingDecision(serverAddress, null, null);
    }
}
