package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.WorldIcon;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.RouteWeaver;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Uploads server favicons as textures once per game session, the same way vanilla does.
 */
final class ServerFavicons {
    private static final Map<String, Entry> CACHE = new HashMap<>();

    private ServerFavicons() {
    }

    /**
     * The texture of the server's favicon, or null if the server has not been pinged yet.
     */
    static @Nullable Identifier textureOf(ServerInfo server) {
        byte[] favicon = server.getFavicon();
        if (favicon == null || favicon.length == 0) {
            return null;
        }

        Entry cached = CACHE.get(server.address);
        if (cached != null && Arrays.equals(cached.favicon, favicon)) {
            return cached.icon.getTextureId();
        }

        try {
            WorldIcon icon = cached != null
                    ? cached.icon
                    : WorldIcon.forServer(MinecraftClient.getInstance().getTextureManager(), server.address);
            icon.load(NativeImage.read(favicon));
            CACHE.put(server.address, new Entry(icon, favicon));
            return icon.getTextureId();
        } catch (IOException | RuntimeException exception) {
            RouteWeaver.LOGGER.warn("Failed to load the favicon of {}: {}", server.address, exception.getMessage());
            return null;
        }
    }

    private record Entry(WorldIcon icon, byte[] favicon) {
    }
}
