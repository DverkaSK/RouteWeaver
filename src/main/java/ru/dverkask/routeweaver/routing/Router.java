package ru.dverkask.routeweaver.routing;

import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;
import ru.dverkask.routeweaver.config.RoutingRule;

/**
 * Decides how a connection to a server is routed.
 */
public final class Router {
    private Router() {
    }

    /**
     * Walks the enabled rules in order; the first one whose profile is enabled wins. A matching
     * rule with a disabled profile is remembered so the UI can explain why the route is direct.
     */
    public static RouteDecision resolve(RouteWeaverConfig config, ServerAddress target) {
        RouteDecision fallback = RouteDecision.DIRECT;
        for (RoutingRule rule : config.getRules()) {
            if (!rule.isEnabled() || !matches(rule, target)) {
                continue;
            }
            ProxyProfile profile = config.findProfile(rule.getProxyProfileId());
            if (profile == null) {
                continue;
            }
            if (profile.isEnabled()) {
                return RouteDecision.proxy(rule, profile);
            }
            if (fallback == RouteDecision.DIRECT) {
                fallback = RouteDecision.proxyDisabled(rule, profile);
            }
        }
        return fallback;
    }

    public static boolean matches(RoutingRule rule, ServerAddress target) {
        return HostPattern.matches(rule.getServerPattern(), target.host()) && portMatches(rule, target);
    }

    /**
     * The rule created by "Save route" for exactly this server, enabled or not.
     */
    public static @Nullable RoutingRule findExactRule(RouteWeaverConfig config, ServerAddress target) {
        for (RoutingRule rule : config.getRules()) {
            if (rule.getServerPattern().equalsIgnoreCase(target.host()) && portMatches(rule, target)) {
                return rule;
            }
        }
        return null;
    }

    private static boolean portMatches(RoutingRule rule, ServerAddress target) {
        return !rule.hasPortFilter() || rule.getPort() == target.port();
    }
}
