package ru.dverkask.routeweaver.proxy

import ru.dverkask.routeweaver.RouteWeaver
import ru.dverkask.routeweaver.config.ProxyProfile
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * A loopback listener that Minecraft connects to instead of the real server. Every accepted
 * connection is relayed through the Shadowsocks profile of the current [TunnelRoute].
 */
class LocalTunnel(private val preferredPort: Int) : Closeable {
    private val route = AtomicReference<TunnelRoute?>()

    @Volatile
    private var serverSocket: ServerSocket? = null

    /** The loopback address Minecraft should connect to, or null while the tunnel is closed. */
    val address: InetSocketAddress?
        get() = serverSocket?.let { InetSocketAddress(InetAddress.getLoopbackAddress(), it.localPort) }

    /** Binds the listener: [preferredPort] if free, otherwise any free port. */
    @Throws(IOException::class)
    fun start() {
        check(serverSocket == null) { "Tunnel already started" }
        val socket = bind()
        serverSocket = socket
        thread(name = "routeweaver-tunnel-accept", isDaemon = true) { acceptLoop(socket) }
        RouteWeaver.LOGGER.info("Tunnel listening on {}", address)
    }

    /** Routes connections accepted from now on; null makes the tunnel refuse them. */
    fun setRoute(route: TunnelRoute?) {
        this.route.set(route)
    }

    override fun close() {
        runCatching { serverSocket?.close() }
    }

    private fun bind(): ServerSocket {
        val loopback = InetAddress.getLoopbackAddress()
        return try {
            ServerSocket(preferredPort, BACKLOG, loopback)
        } catch (e: IOException) {
            RouteWeaver.LOGGER.warn("Tunnel port {} is unavailable ({}), using a random port", preferredPort, e.message)
            ServerSocket(0, BACKLOG, loopback)
        }
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                if (!socket.isClosed) RouteWeaver.LOGGER.warn("Tunnel accept failed: {}", e.message)
                continue
            }
            val current = route.get()
            if (current == null) {
                client.close()
                continue
            }
            thread(name = "routeweaver-relay", isDaemon = true) { client.use { relay(it, current) } }
        }
    }

    private fun relay(client: Socket, route: TunnelRoute) {
        RouteWeaver.LOGGER.info("Relaying {}:{} via {}", route.targetHost, route.targetPort, route.profile.displayName())
        try {
            ShadowsocksRelay(route.profile, route.targetHost, route.targetPort).relay(client)
        } catch (e: Exception) {
            RouteWeaver.LOGGER.warn("Relay to {}:{} failed: {}", route.targetHost, route.targetPort, e.toString())
        }
    }

    private companion object {
        const val BACKLOG = 16
    }
}

/** Where the tunnel forwards connections, and through which proxy. */
data class TunnelRoute(val profile: ProxyProfile, val targetHost: String, val targetPort: Int)
