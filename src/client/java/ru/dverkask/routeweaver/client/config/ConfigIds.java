package ru.dverkask.routeweaver.client.config;

import java.util.Locale;
import java.util.Set;

public final class ConfigIds {
    private ConfigIds() {
    }

    public static String uniqueId(String preferred, String fallbackPrefix, Set<String> taken) {
        String base = slugify(preferred);
        if (base.isBlank()) {
            base = fallbackPrefix;
        }

        String candidate = base;
        int suffix = 2;
        while (taken.contains(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return normalized;
    }
}
