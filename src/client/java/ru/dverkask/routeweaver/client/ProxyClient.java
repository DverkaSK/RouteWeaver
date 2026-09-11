package ru.dverkask.routeweaver.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import ru.dverkask.routeweaver.RouteWeaver;
import ru.dverkask.routeweaver.client.config.ProxyConfigStore;
import ru.dverkask.routeweaver.client.routing.ProxyRouter;
import ru.dverkask.routeweaver.client.runtime.ProxyRuntime;

public class ProxyClient implements ClientModInitializer {
    private static ProxyRuntime runtime;

    @Override public void onInitializeClient() {
        runtime = new ProxyRuntime(new ProxyConfigStore(), new ProxyRouter());
        runtime.initialize();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runtime.shutdown());
        RouteWeaver.LOGGER.info("Client runtime initialized");
    }

    public static ProxyRuntime getRuntime() { return runtime; }
}
