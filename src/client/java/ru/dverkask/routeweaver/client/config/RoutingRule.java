package ru.dverkask.routeweaver.client.config;

public class RoutingRule {
    private String id = "";
    private String label = "";
    private String serverPattern = "";
    private int port = -1;
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
}
