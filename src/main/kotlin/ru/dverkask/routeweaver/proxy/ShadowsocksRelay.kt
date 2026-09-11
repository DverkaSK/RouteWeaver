package ru.dverkask.routeweaver.proxy

import ru.dverkask.routeweaver.RouteWeaver
import ru.dverkask.routeweaver.config.ProxyProfile
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import kotlin.concurrent.thread

/**
 * Relays one local connection to [targetHost]:[targetPort] through a Shadowsocks server.
 */
class ShadowsocksRelay(
    private val profile: ProxyProfile,
    private val targetHost: String,
    private val targetPort: Int,
) {
    private val method = CipherMethod.fromId(profile.method)
        ?: throw IllegalArgumentException("Unsupported cipher: ${profile.method}")

    /**
     * Blocks until both directions are finished. Each direction half-closes the opposite socket
     * when its source ends, so either peer disconnecting tears the whole relay down.
     */
    fun relay(client: Socket) {
        val masterKey = ShadowsocksKeys.masterKey(profile.password, method.keySize)
        Socket().use { server ->
            server.tcpNoDelay = true
            server.connect(InetSocketAddress(profile.host, profile.port), CONNECT_TIMEOUT_MS)

            val downstream = thread(name = "routeweaver-relay-down", isDaemon = true) {
                try {
                    copyServerToClient(server, client, masterKey)
                } catch (e: IOException) {
                    RouteWeaver.LOGGER.debug("Relay downstream ended: {}", e.message)
                } finally {
                    runCatching { client.shutdownOutput() }
                }
            }

            try {
                copyClientToServer(client, server, masterKey)
            } catch (e: IOException) {
                RouteWeaver.LOGGER.debug("Relay upstream ended: {}", e.message)
            } finally {
                runCatching { server.shutdownOutput() }
            }
            // A server that never closes its side must not pin this thread forever.
            downstream.join(HALF_CLOSE_GRACE_MS)
        }
    }

    private fun copyClientToServer(client: Socket, server: Socket, masterKey: ByteArray) {
        val salt = ByteArray(method.saltSize).also(RANDOM::nextBytes)
        val out = BufferedOutputStream(server.getOutputStream(), BUFFER_SIZE)
        out.write(salt)
        val writer = AeadChunkWriter(out, method, ShadowsocksKeys.sessionSubkey(masterKey, salt, method.keySize))
        writer.write(SocksAddress.encode(targetHost, targetPort))

        val input = client.getInputStream()
        val buffer = ByteArray(MAX_CHUNK_PAYLOAD)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return
            writer.write(buffer, 0, read)
        }
    }

    private fun copyServerToClient(server: Socket, client: Socket, masterKey: ByteArray) {
        val input = DataInputStream(server.getInputStream())
        val salt = ByteArray(method.saltSize).also(input::readFully)
        val reader = AeadChunkReader(input, method, ShadowsocksKeys.sessionSubkey(masterKey, salt, method.keySize))

        val out = client.getOutputStream()
        while (true) {
            val chunk = reader.read() ?: return
            out.write(chunk)
            out.flush()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val HALF_CLOSE_GRACE_MS = 30_000L
        const val BUFFER_SIZE = 64 * 1024
        val RANDOM = SecureRandom()
    }
}
