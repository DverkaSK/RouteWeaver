package ru.dverkask.routeweaver.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
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
    static @Nullable Identifier textureOf(ServerData server) {
        byte[] favicon = server.getIconBytes();
        if (favicon == null || favicon.length == 0) {
            return null;
        }

        Entry cached = CACHE.get(server.ip);
        if (cached != null && Arrays.equals(cached.favicon, favicon)) {
            return cached.icon.textureLocation();
        }

        try {
            FaviconTexture icon = cached != null
                    ? cached.icon
                    : FaviconTexture.forServer(Minecraft.getInstance().getTextureManager(), server.ip);
            icon.upload(NativeImage.read(favicon));
            CACHE.put(server.ip, new Entry(icon, favicon));
            return icon.textureLocation();
        } catch (IOException | RuntimeException exception) {
            RouteWeaver.LOGGER.warn("Failed to load the favicon of {}: {}", server.ip, exception.getMessage());
            return null;
        }
    }

    private record Entry(FaviconTexture icon, byte[] favicon) {
    }
}
