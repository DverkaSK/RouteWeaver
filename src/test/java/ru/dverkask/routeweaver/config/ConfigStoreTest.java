package ru.dverkask.routeweaver.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {
    @TempDir
    Path dir;

    @Test
    void createsADefaultFileOnFirstLoad() {
        Path file = dir.resolve("routeweaver/config.json");

        RouteWeaverConfig config = new ConfigStore(file).load();

        assertTrue(Files.exists(file));
        assertTrue(config.getProfiles().isEmpty());
        assertEquals(RouteWeaverConfig.DEFAULT_TUNNEL_PORT, config.getTunnelPort());
    }

    @Test
    void roundTripsProfilesAndRules() {
        Path file = dir.resolve("config.json");
        ConfigStore store = new ConfigStore(file);
        RouteWeaverConfig config = new RouteWeaverConfig();
        ProxyProfile profile = new ProxyProfile();
        profile.setId("nl");
        profile.setHost("1.2.3.4");
        profile.setPort(8388);
        profile.setMethod("aes-256-gcm");
        profile.setPassword("pw");
        config.getProfiles().add(profile);
        RoutingRule rule = new RoutingRule();
        rule.setId("hypixel");
        rule.setServerPattern("*.hypixel.net");
        rule.setProxyProfileId("nl");
        config.getRules().add(rule);

        store.save(config);
        RouteWeaverConfig loaded = store.load();

        assertEquals("1.2.3.4", loaded.findProfile("nl").getHost());
        assertEquals("*.hypixel.net", loaded.findRule("hypixel").getServerPattern());
        assertFalse(Files.exists(dir.resolve("config.json.tmp")));
    }

    @Test
    void readsFilesWrittenByEarlierBuilds() throws IOException {
        Path file = dir.resolve("config.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
                  "tunnelPort": 39753,
                  "profiles": [{"id": "old", "name": null, "mode": "SHADOWSOCKS", "host": "h", "port": 1,
                                "method": "aes-256-gcm", "password": "p", "uuid": "", "vlessFlow": ""}],
                  "rules": [{"id": "r", "serverPattern": "mc.example.net", "port": -1, "proxyProfileId": "old"}]
                }
                """, StandardCharsets.UTF_8);

        RouteWeaverConfig config = new ConfigStore(file).load();

        assertEquals("", config.findProfile("old").getName());
        assertEquals("old", config.findProfile("old").displayName());
        assertFalse(config.findRule("r").hasPortFilter());
    }

    @Test
    void keepsAnUnreadableFileAsABackup() throws IOException {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{ not json", StandardCharsets.UTF_8);

        RouteWeaverConfig config = new ConfigStore(file).load();

        assertTrue(config.getRules().isEmpty());
        assertEquals("{ not json", Files.readString(dir.resolve("config.json.broken"), StandardCharsets.UTF_8));
    }
}
