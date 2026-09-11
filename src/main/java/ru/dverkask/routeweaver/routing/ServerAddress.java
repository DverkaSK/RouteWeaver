package ru.dverkask.routeweaver.routing;

/**
 * A server as the player entered it, before any SRV lookup.
 */
public record ServerAddress(String host, int port) {
    @Override
    public String toString() {
        return host + ":" + port;
    }
}
