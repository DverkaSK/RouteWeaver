package ru.dverkask.routeweaver.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.RouteWeaver;

import java.nio.file.Path;

public final class RouteWeaverClient implements ClientModInitializer {
    private static @Nullable ProxyRuntime runtime;

    @Override
    public void onInitializeClient() {
        Path configFile = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(RouteWeaver.MOD_ID)
                .resolve("config.json");
        ProxyRuntime started = ProxyRuntime.start(configFile);
        runtime = started;
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> started.close());
    }

    /**
     * Null only before the client entrypoint has run.
     */
    public static @Nullable ProxyRuntime runtime() {
        return runtime;
    }
}
