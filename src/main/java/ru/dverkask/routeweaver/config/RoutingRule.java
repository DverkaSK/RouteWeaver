package ru.dverkask.routeweaver.config;

import static ru.dverkask.routeweaver.config.ProxyProfile.nullToEmpty;

/**
 * Sends connections to servers matching {@link #getServerPattern()} through a proxy profile.
 */
public final class RoutingRule {
    /**
     * Port value meaning "match every port".
     */
    public static final int ANY_PORT = 0;

    private String id = "";
    private String label = "";
    private String serverPattern = "";
    private int port = ANY_PORT;
    private String proxyProfileId = "";
    private boolean enabled = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getServerPattern() {
        return serverPattern;
    }

    public void setServerPattern(String serverPattern) {
        this.serverPattern = serverPattern;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean hasPortFilter() {
        return port != ANY_PORT;
    }

    public String getProxyProfileId() {
        return proxyProfileId;
    }

    public void setProxyProfileId(String proxyProfileId) {
        this.proxyProfileId = proxyProfileId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void normalize() {
        id = nullToEmpty(id);
        label = nullToEmpty(label);
        serverPattern = nullToEmpty(serverPattern);
        proxyProfileId = nullToEmpty(proxyProfileId);
        // Early builds stored "any port" as -1.
        if (port < 0) {
            port = ANY_PORT;
        }
    }
}
