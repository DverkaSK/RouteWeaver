package ru.dverkask.routeweaver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import ru.dverkask.routeweaver.RouteWeaver;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes {@link RouteWeaverConfig} as pretty-printed JSON.
 */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;

    public ConfigStore(Path path) {
        this.path = path;
    }

    /**
     * Loads the config, creating a default file on first run. A file that cannot be parsed is
     * kept next to the original as {@code .broken} so the next save does not destroy it.
     */
    public RouteWeaverConfig load() {
        if (Files.notExists(path)) {
            RouteWeaverConfig config = new RouteWeaverConfig();
            save(config);
            return config;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            RouteWeaverConfig config = GSON.fromJson(reader, RouteWeaverConfig.class);
            if (config == null) {
                config = new RouteWeaverConfig();
            }
            config.normalize();
            return config;
        } catch (IOException | JsonParseException exception) {
            RouteWeaver.LOGGER.error("Failed to read {}, starting with an empty config", path, exception);
            backUpUnreadableFile();
            return new RouteWeaverConfig();
        }
    }

    /**
     * Writes through a temporary file so a crash mid-write cannot truncate the config.
     */
    public void save(RouteWeaverConfig config) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            moveReplacing(temp, path);
        } catch (IOException exception) {
            RouteWeaver.LOGGER.error("Failed to write {}", path, exception);
        }
    }

    private void backUpUnreadableFile() {
        Path backup = path.resolveSibling(path.getFileName() + ".broken");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            RouteWeaver.LOGGER.warn("Kept the unreadable config as {}", backup);
        } catch (IOException exception) {
            RouteWeaver.LOGGER.error("Failed to back up {}", path, exception);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
