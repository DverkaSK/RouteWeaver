package ru.dverkask.routeweaver.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigIdsTest {
    @Test
    void slugifiesToLowercaseDashes() {
        assertEquals("my-cool-server-2", ConfigIds.slugify("  My Cool   Server #2! "));
        assertEquals("", ConfigIds.slugify("Сервер"));
        assertEquals("", ConfigIds.slugify(null));
    }

    @Test
    void appendsASuffixUntilTheIdIsFree() {
        assertEquals("hypixel", ConfigIds.uniqueId("Hypixel", "route", Set.of()));
        assertEquals("hypixel-3", ConfigIds.uniqueId("Hypixel", "route", Set.of("hypixel", "hypixel-2")));
    }

    @Test
    void fallsBackWhenNothingSurvivesSlugifying() {
        assertEquals("profile", ConfigIds.uniqueId("Прокси", "profile", Set.of()));
        assertEquals("profile-2", ConfigIds.uniqueId("", "profile", Set.of("profile")));
    }
}
