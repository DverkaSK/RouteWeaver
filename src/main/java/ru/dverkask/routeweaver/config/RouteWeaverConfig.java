package ru.dverkask.routeweaver.config;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Root of {@code config/routeweaver/config.json}.
 */
public final class RouteWeaverConfig {
    public static final int SCHEMA_VERSION = 1;
    public static final int DEFAULT_TUNNEL_PORT = 39753;

    private int schemaVersion = SCHEMA_VERSION;
    private int tunnelPort = DEFAULT_TUNNEL_PORT;
    private List<ProxyProfile> profiles = new ArrayList<>();
    private List<RoutingRule> rules = new ArrayList<>();

    public int getTunnelPort() {
        return tunnelPort;
    }

    /**
     * Mutable, in display order.
     */
    public List<ProxyProfile> getProfiles() {
        return profiles;
    }

    /**
     * Mutable, in priority order: the first matching rule wins.
     */
    public List<RoutingRule> getRules() {
        return rules;
    }

    public @Nullable ProxyProfile findProfile(String id) {
        for (ProxyProfile profile : profiles) {
            if (profile.getId().equals(id)) {
                return profile;
            }
        }
        return null;
    }

    public @Nullable RoutingRule findRule(String id) {
        for (RoutingRule rule : rules) {
            if (rule.getId().equals(id)) {
                return rule;
            }
        }
        return null;
    }

    public int countRulesUsing(String profileId) {
        int count = 0;
        for (RoutingRule rule : rules) {
            if (rule.getProxyProfileId().equals(profileId)) {
                count++;
            }
        }
        return count;
    }

    public int countEnabledRules() {
        int count = 0;
        for (RoutingRule rule : rules) {
            if (rule.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public Set<String> profileIds() {
        Set<String> ids = new HashSet<>();
        profiles.forEach(profile -> ids.add(profile.getId()));
        return ids;
    }

    public Set<String> ruleIds() {
        Set<String> ids = new HashSet<>();
        rules.forEach(rule -> ids.add(rule.getId()));
        return ids;
    }

    /**
     * Repairs whatever Gson left null or out of range in a hand-edited file.
     */
    void normalize() {
        schemaVersion = SCHEMA_VERSION;
        if (tunnelPort < 0 || tunnelPort > 65535) {
            tunnelPort = DEFAULT_TUNNEL_PORT;
        }
        profiles = profiles == null ? new ArrayList<>() : new ArrayList<>(profiles);
        rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
        profiles.removeIf(Objects::isNull);
        rules.removeIf(Objects::isNull);
        profiles.forEach(ProxyProfile::normalize);
        rules.forEach(RoutingRule::normalize);
    }
}
