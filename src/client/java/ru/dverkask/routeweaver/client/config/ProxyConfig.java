package ru.dverkask.routeweaver.client.config;

import java.util.ArrayList;
import java.util.List;

public class ProxyConfig {
    private int schemaVersion = 1;
    private int tunnelPort = 39753;
    private List<ProxyProfile> profiles = new ArrayList<>();
    private List<RoutingRule> rules = new ArrayList<>();

    public static ProxyConfig createDefault() {
        return new ProxyConfig();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public int getTunnelPort() {
        return tunnelPort;
    }

    public void setTunnelPort(int tunnelPort) {
        this.tunnelPort = tunnelPort;
    }

    public List<ProxyProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<ProxyProfile> profiles) {
        this.profiles = profiles;
    }

    public List<RoutingRule> getRules() {
        return rules;
    }

    public void setRules(List<RoutingRule> rules) {
        this.rules = rules;
    }
}
