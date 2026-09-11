package ru.dverkask.routeweaver.config;

import java.util.Locale;
import java.util.Set;

/**
 * Generates readable, unique ids such as {@code hypixel} or {@code hypixel-2}.
 */
public final class ConfigIds {
    private ConfigIds() {
    }

    public static String uniqueId(String preferred, String fallback, Set<String> taken) {
        String base = slugify(preferred);
        if (base.isEmpty()) {
            base = fallback;
        }

        String candidate = base;
        for (int suffix = 2; taken.contains(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
