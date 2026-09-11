package ru.dverkask.routeweaver.client.proxy

import ru.dverkask.routeweaver.RouteWeaver
import ru.dverkask.routeweaver.client.config.ProxyProfile

enum class EngineStatus { RUNNING, STOPPED, ERROR }

data class EngineSnapshot(
    val running: Boolean,
    val tunnelAddress: String,
    val error: String = ""
)

class ProxyEngine(preferredTunnelPort: Int = 39753) {
    private val tunnel = ProxyTunnel(preferredTunnelPort)

    @Volatile var status: EngineStatus = EngineStatus.STOPPED; private set
    @Volatile var error:  String       = "";                    private set

    val tunnelPort: Int get() = tunnel.port

    fun start() {
        try {
            tunnel.start()
            status = EngineStatus.RUNNING
            RouteWeaver.LOGGER.info("ProxyEngine started on tunnel port {}", tunnel.port)
        } catch (e: Exception) {
            status = EngineStatus.ERROR
            error  = e.message ?: "Unknown error"
            RouteWeaver.LOGGER.error("ProxyEngine failed to start", e)
        }
    }

    fun stop() {
        tunnel.close()
        status = EngineStatus.STOPPED
    }

    fun applyRoute(profile: ProxyProfile, targetHost: String, targetPort: Int): Boolean = try {
        tunnel.setRoute(profile, targetHost, targetPort)
        true
    } catch (e: Exception) {
        RouteWeaver.LOGGER.error("Failed to apply route", e)
        false
    }

    fun clearRoute() = tunnel.clearRoute()

    fun getSnapshot() = EngineSnapshot(
        running       = status == EngineStatus.RUNNING,
        tunnelAddress = if (status == EngineStatus.RUNNING) "127.0.0.1:$tunnelPort" else "",
        error         = error
    )
}
