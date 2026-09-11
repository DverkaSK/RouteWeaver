package ru.dverkask.routeweaver.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.dverkask.routeweaver.RouteWeaver;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProxyConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(RouteWeaver.MOD_ID)
            .resolve("config.json");

    public ProxyConfig load() {
        if (Files.notExists(CONFIG_PATH)) {
            ProxyConfig config = ProxyConfig.createDefault();
            save(config);
            return config;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ProxyConfig config = GSON.fromJson(reader, ProxyConfig.class);
            return normalize(config);
        } catch (IOException exception) {
            RouteWeaver.LOGGER.error("Failed to read proxy config from {}", CONFIG_PATH, exception);
            return ProxyConfig.createDefault();
        }
    }

    public void save(ProxyConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            RouteWeaver.LOGGER.error("Failed to write proxy config to {}", CONFIG_PATH, exception);
        }
    }

    public Path getConfigPath() {
        return CONFIG_PATH;
    }

    private ProxyConfig normalize(ProxyConfig config) {
        ProxyConfig resolved = config != null ? config : ProxyConfig.createDefault();
        if (resolved.getProfiles() == null) {
            resolved.setProfiles(new java.util.ArrayList<>());
        }
        if (resolved.getRules() == null) {
            resolved.setRules(new java.util.ArrayList<>());
        }
        return resolved;
    }
}
