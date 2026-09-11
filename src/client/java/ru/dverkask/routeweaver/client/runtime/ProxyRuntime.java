package ru.dverkask.routeweaver.client.runtime;

import ru.dverkask.routeweaver.RouteWeaver;
import ru.dverkask.routeweaver.client.config.ProxyConfig;
import ru.dverkask.routeweaver.client.config.ProxyConfigStore;
import ru.dverkask.routeweaver.client.proxy.EngineSnapshot;
import ru.dverkask.routeweaver.client.proxy.ProxyEngine;
import ru.dverkask.routeweaver.client.routing.ProxyRouter;
import ru.dverkask.routeweaver.client.routing.RoutePreparation;
import ru.dverkask.routeweaver.client.routing.RoutingDecision;
import ru.dverkask.routeweaver.client.routing.ServerAddress;

import java.net.InetSocketAddress;

public class ProxyRuntime {
    private final ProxyConfigStore configStore;
    private final ProxyRouter      router;

    private ProxyConfig config;
    private ProxyEngine engine;
    private volatile RoutePreparation pendingConnectionPreparation;

    public ProxyRuntime(ProxyConfigStore configStore, ProxyRouter router) {
        this.configStore = configStore;
        this.router      = router;
    }

    public void initialize() {
        config = configStore.load();
        engine = new ProxyEngine(config.getTunnelPort());
        engine.start();
        RouteWeaver.LOGGER.info("ProxyRuntime initialized (tunnel :{})", engine.getTunnelPort());
    }

    public void shutdown() {
        engine.stop();
    }

    public ProxyConfig getConfig() { return config; }

    public void saveConfig() {
        if (config != null) {
            configStore.save(config);
            engine.clearRoute();
        }
    }

    public RoutePreparation prepareServerRoute(ServerAddress serverAddress) {
        RoutingDecision decision = router.decide(config, serverAddress);
        if (!decision.shouldUseProxy()) {
            pendingConnectionPreparation = null;
            RouteWeaver.LOGGER.info("Direct route for {}:{}", serverAddress.host(), serverAddress.port());
            return new RoutePreparation(decision, false);
        }

        engine.clearRoute();

        RoutePreparation prep = new RoutePreparation(decision, true);
        pendingConnectionPreparation = prep;

        RouteWeaver.LOGGER.info("Proxy route prepared for {}:{} via {}", serverAddress.host(), serverAddress.port(), decision.profile().getName());
        return prep;
    }

    public EngineSnapshot getEngineSnapshot() {
        return engine.getSnapshot();
    }

    public InetSocketAddress resolveConnectTarget(String host, int port) {
        String threadName = Thread.currentThread().getName();
        if (threadName != null && threadName.startsWith("Server Pinger")) return null;

        RoutePreparation pending = pendingConnectionPreparation;
        pendingConnectionPreparation = null;
        if (pending == null) return null;

        RoutingDecision decision = pending.decision();
        if (decision == null || !decision.shouldUseProxy()) return null;

        // Update the tunnel route with the actual network-resolved address.
        // prepareServerRoute() used the pre-SRV hostname (e.g. "vnlla.ru"), but
        // AllowedAddressResolver may have resolved it to a different host via SRV
        // (e.g. "connect.vnlla.ru.").  If the game server only listens on the
        // SRV-resolved name, forwarding to the original name silently fails.
        engine.applyRoute(decision.profile(), host, port);

        RouteWeaver.LOGGER.info("Redirecting {}:{} → tunnel :{}", host, port, engine.getTunnelPort());
        return new InetSocketAddress("127.0.0.1", engine.getTunnelPort());
    }
}
