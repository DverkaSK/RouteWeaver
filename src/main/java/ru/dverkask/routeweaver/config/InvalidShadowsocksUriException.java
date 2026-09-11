package ru.dverkask.routeweaver.config;

/**
 * Thrown by {@link ShadowsocksUri#parse(String)}; {@link #reason()} lets the UI show a
 * translated message.
 */
public final class InvalidShadowsocksUriException extends IllegalArgumentException {
    public enum Reason {
        EMPTY,
        NOT_SHADOWSOCKS,
        MALFORMED,
        MISSING_CREDENTIALS,
        MISSING_PORT,
        INVALID_PORT,
        PLUGIN_UNSUPPORTED
    }

    private final Reason reason;

    public InvalidShadowsocksUriException(Reason reason) {
        super("Invalid ss:// link: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
