package ru.dverkask.routeweaver.proxy

import java.security.spec.AlgorithmParameterSpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

/**
 * The Shadowsocks AEAD ciphers this client can speak. Salt length always equals key length.
 */
enum class CipherMethod(
    val id: String,
    val keySize: Int,
    internal val transformation: String,
    internal val keyAlgorithm: String,
) {
    CHACHA20_IETF_POLY1305("chacha20-ietf-poly1305", 32, "ChaCha20-Poly1305", "ChaCha20"),
    AES_256_GCM("aes-256-gcm", 32, "AES/GCM/NoPadding", "AES"),
    AES_128_GCM("aes-128-gcm", 16, "AES/GCM/NoPadding", "AES");

    val saltSize: Int get() = keySize

    internal fun parameterSpec(nonce: ByteArray): AlgorithmParameterSpec =
        if (this == CHACHA20_IETF_POLY1305) IvParameterSpec(nonce) else GCMParameterSpec(TAG_SIZE * 8, nonce)

    companion object {
        const val NONCE_SIZE = 12
        const val TAG_SIZE = 16

        @JvmField
        val DEFAULT = CHACHA20_IETF_POLY1305

        /** Accepts the canonical id in any case, with `_` in place of `-`. Returns null if unsupported. */
        @JvmStatic
        fun fromId(id: String): CipherMethod? {
            val normalized = id.trim().lowercase().replace('_', '-')
            return entries.firstOrNull { it.id == normalized }
        }
    }
}
