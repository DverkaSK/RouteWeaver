package ru.dverkask.routeweaver.client;

import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.RouteWeaver;
import ru.dverkask.routeweaver.config.ConfigStore;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;
import ru.dverkask.routeweaver.proxy.LocalTunnel;
import ru.dverkask.routeweaver.proxy.TunnelRoute;
import ru.dverkask.routeweaver.routing.RouteDecision;
import ru.dverkask.routeweaver.routing.Router;
import ru.dverkask.routeweaver.routing.ServerAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the loaded config and the local tunnel, and hands the routing decision made when the
 * player clicks "Join" over to the connector thread that opens the socket.
 */
public final class ProxyRuntime implements AutoCloseable {
    private final ConfigStore store;
    private final RouteWeaverConfig config;
    private final LocalTunnel tunnel;
    private final AtomicReference<ProxyProfile> pendingProfile = new AtomicReference<>();
    private volatile String tunnelError = "";

    private ProxyRuntime(ConfigStore store) {
        this.store = store;
        this.config = store.load();
        this.tunnel = new LocalTunnel(config.getTunnelPort());
    }

    public static ProxyRuntime start(Path configFile) {
        ProxyRuntime runtime = new ProxyRuntime(new ConfigStore(configFile));
        try {
            runtime.tunnel.start();
        } catch (IOException exception) {
            runtime.tunnelError = String.valueOf(exception.getMessage());
            RouteWeaver.LOGGER.error("Failed to start the proxy tunnel; every server will connect directly", exception);
        }
        return runtime;
    }

    /**
     * The live config; the UI edits it in place and then calls {@link #saveConfig()}.
     */
    public RouteWeaverConfig config() {
        return config;
    }

    public void saveConfig() {
        store.save(config);
    }

    public @Nullable InetSocketAddress tunnelAddress() {
        return tunnel.getAddress();
    }

    public String tunnelError() {
        return tunnelError;
    }

    /**
     * Called on the render thread when a connection to {@code target} starts. The decision has
     * to be made here: the connector thread only sees the address after SRV resolution.
     */
    public void prepareConnection(ServerAddress target) {
        RouteDecision decision = Router.resolve(config, target);
        if (decision.usesProxy()) {
            pendingProfile.set(decision.profile());
            RouteWeaver.LOGGER.info("Routing {} via proxy {}", target, decision.profile().displayName());
        } else {
            pendingProfile.set(null);
            RouteWeaver.LOGGER.info("Connecting to {} directly", target);
        }
    }

    /**
     * Called on the connector thread with the resolved server address; returns the address the
     * socket should actually connect to.
     */
    public InetSocketAddress redirectConnection(InetSocketAddress resolved) {
        ProxyProfile profile = pendingProfile.getAndSet(null);
        InetSocketAddress tunnelAddress = tunnel.getAddress();
        if (profile == null) {
            tunnel.setRoute(null);
            return resolved;
        }
        if (tunnelAddress == null) {
            RouteWeaver.LOGGER.warn("Tunnel is not running, connecting to {} directly", resolved);
            return resolved;
        }

        // Forward to the SRV target rather than the typed host: some servers only accept
        // connections on the address their SRV record points at.
        tunnel.setRoute(new TunnelRoute(profile, resolved.getHostString(), resolved.getPort()));
        RouteWeaver.LOGGER.info("Redirecting {} through tunnel {}", resolved, tunnelAddress);
        return tunnelAddress;
    }

    @Override
    public void close() {
        tunnel.close();
    }
}
