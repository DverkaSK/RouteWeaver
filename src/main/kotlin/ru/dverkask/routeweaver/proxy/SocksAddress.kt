package ru.dverkask.routeweaver.proxy

import java.io.ByteArrayOutputStream
import java.net.InetAddress

/**
 * Encodes the target in the SOCKS5 address format that opens every Shadowsocks stream.
 */
object SocksAddress {
    private const val TYPE_IPV4 = 0x01
    private const val TYPE_DOMAIN = 0x03
    private const val TYPE_IPV6 = 0x04

    private val IPV4 = Regex("""(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})""")

    @JvmStatic
    fun encode(host: String, port: Int): ByteArray {
        require(port in 1..65535) { "Port out of range: $port" }
        val out = ByteArrayOutputStream()
        val ipv4 = IPV4.matchEntire(host)?.groupValues?.drop(1)?.map { it.toInt() }
        when {
            ipv4 != null && ipv4.all { it <= 255 } -> {
                out.write(TYPE_IPV4)
                ipv4.forEach { out.write(it) }
            }
            ':' in host -> {
                // Only IPv6 literals contain ':', and getByName parses literals without DNS.
                out.write(TYPE_IPV6)
                out.write(InetAddress.getByName(host.removeSurrounding("[", "]")).address)
            }
            else -> {
                val domain = host.toByteArray(Charsets.US_ASCII)
                require(domain.size in 1..255) { "Host name must be 1-255 bytes: $host" }
                out.write(TYPE_DOMAIN)
                out.write(domain.size)
                out.write(domain)
            }
        }
        out.write(port ushr 8)
        out.write(port and 0xFF)
        return out.toByteArray()
    }
}
