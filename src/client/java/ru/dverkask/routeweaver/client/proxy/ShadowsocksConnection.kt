package ru.dverkask.routeweaver.client.proxy

import ru.dverkask.routeweaver.RouteWeaver
import ru.dverkask.routeweaver.client.config.ProxyProfile
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch

class ShadowsocksConnection(
    private val profile: ProxyProfile,
    private val targetHost: String,
    private val targetPort: Int
) {
    fun relay(clientIn: InputStream, clientOut: OutputStream) {
        val method    = CipherMethod.from(profile.method)
        val masterKey = evpBytesToKey(profile.password.toByteArray(Charsets.UTF_8), method.keySize)

        val ssSocket = Socket(profile.host, profile.port).apply { tcpNoDelay = true }
        try {
            val ssIn  = ssSocket.inputStream
            val ssOut = ssSocket.outputStream

            // Send client salt + derive client subkey
            val clientSalt = ByteArray(method.keySize).also { SecureRandom().nextBytes(it) }
            ssOut.write(clientSalt)
            val clientSubkey = hkdfSha1(masterKey, clientSalt, SS_SUBKEY_INFO, method.keySize)

            // Write target address as first AEAD payload
            val writer = AeadWriter(ssOut, clientSubkey, method)
            writer.write(buildTargetAddress(targetHost, targetPort))

            val latch = CountDownLatch(2)

            // MC → SS server (encrypt and forward)
            Thread({
                try {
                    val buf = ByteArray(MAX_BUF)
                    while (true) {
                        val n = clientIn.read(buf)
                        if (n < 0) break
                        writer.write(buf.copyOf(n))
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { ssSocket.shutdownOutput() }
                    latch.countDown()
                }
            }, "rw-ss-up").apply { isDaemon = true; start() }

            // SS server → MC (decrypt and forward)
            Thread({
                try {
                    // Read server salt first
                    val serverSalt = ByteArray(method.keySize)
                    var off = 0
                    while (off < serverSalt.size) {
                        val r = ssIn.read(serverSalt, off, serverSalt.size - off)
                        if (r < 0) return@Thread
                        off += r
                    }
                    val serverSubkey = hkdfSha1(masterKey, serverSalt, SS_SUBKEY_INFO, method.keySize)
                    val reader = AeadReader(ssIn, serverSubkey, method)
                    while (true) {
                        val chunk = reader.read()
                        clientOut.write(chunk)
                        clientOut.flush()
                    }
                } catch (_: Exception) {
                } finally {
                    runCatching { ssSocket.shutdownInput() }
                    latch.countDown()
                }
            }, "rw-ss-down").apply { isDaemon = true; start() }

            latch.await()
        } finally {
            runCatching { ssSocket.close() }
        }
    }

    private companion object {
        val SS_SUBKEY_INFO = "ss-subkey".toByteArray()
        const val MAX_BUF  = 0x3FFF

        fun buildTargetAddress(host: String, port: Int): ByteArray {
            val portBytes = byteArrayOf((port shr 8).toByte(), (port and 0xFF).toByte())
            val ipv4 = host.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
            return if (ipv4) {
                val parts = host.split(".").map { it.toInt().toByte() }.toByteArray()
                byteArrayOf(0x01) + parts + portBytes
            } else {
                val h = host.toByteArray(Charsets.UTF_8)
                byteArrayOf(0x03, h.size.toByte()) + h + portBytes
            }
        }
    }
}
