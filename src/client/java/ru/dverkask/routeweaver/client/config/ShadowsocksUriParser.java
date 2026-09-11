package ru.dverkask.routeweaver.client.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ShadowsocksUriParser {
    private ShadowsocksUriParser() {
    }

    public static ParsedShadowsocksUri parse(String uri) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Shadowsocks link is empty.");
        }
        if (!uri.startsWith("ss://")) {
            throw new IllegalArgumentException("Link must start with ss://");
        }

        String payload = uri.substring("ss://".length()).trim();
        String name = "";
        int hashIndex = payload.indexOf('#');
        if (hashIndex >= 0) {
            name = decodeUrlComponent(payload.substring(hashIndex + 1));
            payload = payload.substring(0, hashIndex);
        }

        int atIndex = payload.lastIndexOf('@');
        String userInfo;
        String hostPort;
        if (atIndex >= 0) {
            userInfo = payload.substring(0, atIndex);
            hostPort = payload.substring(atIndex + 1);
            if (!userInfo.contains(":")) {
                userInfo = decodeBase64(userInfo);
            }
        } else {
            String decoded = decodeBase64(payload);
            int splitIndex = decoded.lastIndexOf('@');
            if (splitIndex < 0) {
                throw new IllegalArgumentException("Invalid Shadowsocks link format.");
            }
            userInfo = decoded.substring(0, splitIndex);
            hostPort = decoded.substring(splitIndex + 1);
        }

        int colonIndex = userInfo.indexOf(':');
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Missing method or password in Shadowsocks link.");
        }
        String method = userInfo.substring(0, colonIndex);
        String password = userInfo.substring(colonIndex + 1);

        int portIndex = hostPort.lastIndexOf(':');
        if (portIndex < 0) {
            throw new IllegalArgumentException("Missing host or port in Shadowsocks link.");
        }

        String host = hostPort.substring(0, portIndex);
        int port;
        try {
            port = Integer.parseInt(hostPort.substring(portIndex + 1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Port in Shadowsocks link is invalid.");
        }

        return new ParsedShadowsocksUri(name, host, port, method, password);
    }

    private static String decodeBase64(String value) {
        String normalized = value.trim().replace('-', '+').replace('_', '/');
        int remainder = normalized.length() % 4;
        if (remainder != 0) {
            normalized = normalized + "=".repeat(4 - remainder);
        }
        return new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);
    }

    private static String decodeUrlComponent(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public record ParsedShadowsocksUri(String name, String host, int port, String method, String password) {
    }
}
