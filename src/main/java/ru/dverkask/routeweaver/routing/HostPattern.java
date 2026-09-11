package ru.dverkask.routeweaver.routing;

import java.util.Locale;

/**
 * Case-insensitive host patterns: an exact host, {@code *.example.net} for any subdomain,
 * or {@code *} for everything.
 */
public final class HostPattern {
    private HostPattern() {
    }

    public static boolean matches(String pattern, String host) {
        if (pattern == null || pattern.isBlank() || host == null || host.isBlank()) {
            return false;
        }

        String normalizedPattern = pattern.trim().toLowerCase(Locale.ROOT);
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        if (normalizedPattern.equals("*")) {
            return true;
        }
        if (normalizedPattern.startsWith("*.")) {
            return normalizedHost.endsWith(normalizedPattern.substring(1));
        }
        return normalizedHost.equals(normalizedPattern);
    }
}
