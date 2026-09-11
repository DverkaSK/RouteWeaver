package ru.dverkask.routeweaver.config;

/**
 * A Shadowsocks server that connections can be tunnelled through.
 */
public final class ProxyProfile {
    private String id = "";
    private String name = "";
    private String host = "";
    private int port;
    private String method = "";
    private String password = "";
    private boolean enabled = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * The name to show in the UI; unnamed profiles fall back to their id.
     */
    public String displayName() {
        return name.isBlank() ? id : name;
    }

    /**
     * Whether both profiles point at the same server with the same credentials.
     */
    public boolean hasSameEndpoint(ProxyProfile other) {
        return host.equalsIgnoreCase(other.host)
                && port == other.port
                && method.equalsIgnoreCase(other.method)
                && password.equals(other.password);
    }

    void normalize() {
        id = nullToEmpty(id);
        name = nullToEmpty(name);
        host = nullToEmpty(host);
        method = nullToEmpty(method);
        password = nullToEmpty(password);
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
