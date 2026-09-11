package ru.dverkask.routeweaver.client.proxy

import ru.dverkask.routeweaver.RouteWeaver
import ru.dverkask.routeweaver.client.config.ProxyProfile
import java.io.Closeable
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference

class ProxyTunnel(preferredPort: Int = 39753) : Closeable {
    private val serverSocket = try {
        ServerSocket(preferredPort)
    } catch (_: Exception) {
        RouteWeaver.LOGGER.warn("Tunnel port {} is busy, falling back to random port", preferredPort)
        ServerSocket(0)
    }
    val port: Int get() = serverSocket.localPort

    private val activeRoute = AtomicReference<TunnelRoute?>(null)
    @Volatile private var running = false

    fun start() {
        running = true
        Thread({
            RouteWeaver.LOGGER.info("ProxyTunnel listening on :{}", port)
            while (running) {
                try {
                    val client = serverSocket.accept()
                    val route  = activeRoute.get()
                    if (route == null) { client.close(); continue }
                    Thread({
                        client.use {
                            try {
                                RouteWeaver.LOGGER.info(
                                    "Tunnel {} → {}:{} via {}",
                                    it.remoteSocketAddress, route.targetHost,
                                    route.targetPort, route.profile.name
                                )
                                ShadowsocksConnection(route.profile, route.targetHost, route.targetPort)
                                    .relay(it.inputStream, it.outputStream)
                            } catch (e: Exception) {
                                RouteWeaver.LOGGER.debug("Tunnel conn error: {}", e.message)
                            }
                        }
                    }, "rw-tunnel-conn").apply { isDaemon = true; start() }
                } catch (e: Exception) {
                    if (running) RouteWeaver.LOGGER.warn("Tunnel accept error: {}", e.message)
                }
            }
        }, "rw-tunnel-accept").apply { isDaemon = true; start() }
    }

    fun setRoute(profile: ProxyProfile, targetHost: String, targetPort: Int) {
        activeRoute.set(TunnelRoute(profile, targetHost, targetPort))
    }

    fun clearRoute() { activeRoute.set(null) }
    fun hasRoute(): Boolean = activeRoute.get() != null

    override fun close() {
        running = false
        runCatching { serverSocket.close() }
    }
}

data class TunnelRoute(val profile: ProxyProfile, val targetHost: String, val targetPort: Int)
