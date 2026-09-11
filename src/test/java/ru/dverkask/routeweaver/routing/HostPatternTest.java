package ru.dverkask.routeweaver.routing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostPatternTest {
    @ParameterizedTest
    @CsvSource({
            "hypixel.net,      hypixel.net,     true",
            "Hypixel.NET,      hypixel.net,     true",
            "hypixel.net,      mc.hypixel.net,  false",
            "*.hypixel.net,    mc.hypixel.net,  true",
            "*.hypixel.net,    hypixel.net,     false",
            "*.hypixel.net,    evilhypixel.net, false",
            "*,                anything.at.all, true",
            "'',               hypixel.net,     false",
    })
    void matches(String pattern, String host, boolean expected) {
        assertEquals(expected, HostPattern.matches(pattern, host));
    }
}
