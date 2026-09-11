package ru.dverkask.routeweaver.proxy

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Key derivation from the Shadowsocks AEAD spec.
 *
 * @see <a href="https://shadowsocks.org/doc/aead.html">Shadowsocks AEAD</a>
 */
object ShadowsocksKeys {
    private val SUBKEY_INFO = "ss-subkey".toByteArray(Charsets.US_ASCII)

    /** OpenSSL `EVP_BytesToKey` with MD5 and no salt, as every Shadowsocks implementation uses. */
    @JvmStatic
    fun masterKey(password: String, keySize: Int): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        val key = ByteArrayOutputStream(keySize + 16)
        var block = ByteArray(0)
        while (key.size() < keySize) {
            md5.update(block)
            md5.update(password.toByteArray(Charsets.UTF_8))
            block = md5.digest()
            key.write(block)
        }
        return key.toByteArray().copyOf(keySize)
    }

    /** Per-connection subkey: HKDF-SHA1 over the master key, salted with the random salt. */
    @JvmStatic
    fun sessionSubkey(masterKey: ByteArray, salt: ByteArray, keySize: Int): ByteArray =
        hkdfSha1(masterKey, salt, SUBKEY_INFO, keySize)

    /** RFC 5869 HKDF with HMAC-SHA1. */
    @JvmStatic
    fun hkdfSha1(inputKey: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA1")
        // RFC 5869: an absent salt is a string of HashLen zeros.
        val effectiveSalt = if (salt.isEmpty()) ByteArray(mac.macLength) else salt
        mac.init(SecretKeySpec(effectiveSalt, "HmacSHA1"))
        val pseudoRandomKey = mac.doFinal(inputKey)

        mac.init(SecretKeySpec(pseudoRandomKey, "HmacSHA1"))
        val output = ByteArrayOutputStream(length + mac.macLength)
        var block = ByteArray(0)
        var counter = 1
        while (output.size() < length) {
            mac.update(block)
            mac.update(info)
            mac.update(counter.toByte())
            block = mac.doFinal()
            output.write(block)
            counter++
        }
        return output.toByteArray().copyOf(length)
    }
}
