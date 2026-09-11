package ru.dverkask.routeweaver.client.config;

public class ProxyProfile {
    private String id = "";
    private String name = "";
    private ProxyMode mode = ProxyMode.SHADOWSOCKS;
    private String host = "";
    private int port = 0;
    private String username = "";
    private String password = "";
    private String method = "";
    private String vlessFlow = "";
    private String uuid = "";
    private String serverName = "";
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

    public ProxyMode getMode() {
        return mode;
    }

    public void setMode(ProxyMode mode) {
        this.mode = mode;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getVlessFlow() {
        return vlessFlow;
    }

    public void setVlessFlow(String vlessFlow) {
        this.vlessFlow = vlessFlow;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
