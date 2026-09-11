package ru.dverkask.routeweaver.routing;

import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RoutingRule;

/**
 * Outcome of {@link Router#resolve}. {@code rule} and {@code profile} are set for every kind
 * except {@link Kind#DIRECT}.
 */
public record RouteDecision(Kind kind, @Nullable RoutingRule rule, @Nullable ProxyProfile profile) {
    public enum Kind {
        /** No enabled rule matches: connect without a proxy. */
        DIRECT,
        /** Connect through {@link #profile()}. */
        PROXY,
        /** A rule matches but its profile is switched off, so the connection goes direct. */
        PROXY_DISABLED
    }

    public static final RouteDecision DIRECT = new RouteDecision(Kind.DIRECT, null, null);

    public static RouteDecision proxy(RoutingRule rule, ProxyProfile profile) {
        return new RouteDecision(Kind.PROXY, rule, profile);
    }

    public static RouteDecision proxyDisabled(RoutingRule rule, ProxyProfile profile) {
        return new RouteDecision(Kind.PROXY_DISABLED, rule, profile);
    }

    public boolean usesProxy() {
        return kind == Kind.PROXY;
    }
}
