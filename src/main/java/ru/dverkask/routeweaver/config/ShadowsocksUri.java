package ru.dverkask.routeweaver.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A parsed {@code ss://} link, in either the SIP002 or the legacy all-base64 form.
 *
 * @see <a href="https://shadowsocks.org/doc/sip002.html">SIP002 URI scheme</a>
 */
public record ShadowsocksUri(String name, String host, int port, String method, String password) {
    private static final String SCHEME = "ss://";

    public static ShadowsocksUri parse(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.EMPTY);
        }
        String rest = input.trim();
        if (!rest.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.NOT_SHADOWSOCKS);
        }
        rest = rest.substring(SCHEME.length());

        String name = "";
        int hash = rest.indexOf('#');
        if (hash >= 0) {
            name = percentDecode(rest.substring(hash + 1));
            rest = rest.substring(0, hash);
        }

        int query = rest.indexOf('?');
        if (query >= 0) {
            rejectPlugins(rest.substring(query + 1));
            rest = rest.substring(0, query);
        }

        String userInfo;
        String hostPort;
        int at = rest.lastIndexOf('@');
        if (at >= 0) {
            userInfo = rest.substring(0, at);
            hostPort = stripTrailingSlash(rest.substring(at + 1));
            // SIP002 allows either base64url("method:password") or percent-encoded plain text.
            userInfo = userInfo.contains(":") ? percentDecode(userInfo) : decodeBase64(userInfo);
        } else {
            String decoded = decodeBase64(rest);
            int split = decoded.lastIndexOf('@');
            if (split < 0) {
                throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.MALFORMED);
            }
            userInfo = decoded.substring(0, split);
            hostPort = decoded.substring(split + 1);
        }

        int colon = userInfo.indexOf(':');
        if (colon <= 0 || colon == userInfo.length() - 1) {
            throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.MISSING_CREDENTIALS);
        }

        int portSeparator = hostPort.lastIndexOf(':');
        if (portSeparator <= 0) {
            throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.MISSING_PORT);
        }
        String host = hostPort.substring(0, portSeparator);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        return new ShadowsocksUri(
                name,
                host,
                parsePort(hostPort.substring(portSeparator + 1)),
                userInfo.substring(0, colon),
                userInfo.substring(colon + 1));
    }

    private static void rejectPlugins(String query) {
        for (String parameter : query.split("&")) {
            if (parameter.startsWith("plugin=") && parameter.length() > "plugin=".length()) {
                throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.PLUGIN_UNSUPPORTED);
            }
        }
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.INVALID_PORT);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String decodeBase64(String value) {
        String normalized = value.trim().replace('-', '+').replace('_', '/');
        int remainder = normalized.length() % 4;
        if (remainder != 0) {
            normalized += "=".repeat(4 - remainder);
        }
        try {
            return new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.MALFORMED);
        }
    }

    /**
     * Unlike {@link URLDecoder} alone, keeps {@code +} literal: base64 passwords contain it.
     */
    private static String percentDecode(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new InvalidShadowsocksUriException(InvalidShadowsocksUriException.Reason.MALFORMED);
        }
    }
}
