package ru.dverkask.routeweaver.client.routing;

public final class ServerPatternMatcher {
    private ServerPatternMatcher() {
    }

    public static boolean matches(String pattern, String host) {
        if (pattern == null || pattern.isBlank() || host == null || host.isBlank()) {
            return false;
        }

        String normalizedPattern = pattern.trim().toLowerCase();
        String normalizedHost = host.trim().toLowerCase();
        if ("*".equals(normalizedPattern)) {
            return true;
        }

        if (normalizedPattern.startsWith("*.")) {
            String suffix = normalizedPattern.substring(1);
            return normalizedHost.endsWith(suffix);
        }

        return normalizedHost.equals(normalizedPattern);
    }
}
