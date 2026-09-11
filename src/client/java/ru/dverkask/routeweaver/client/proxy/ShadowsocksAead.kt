package ru.dverkask.routeweaver.client.proxy

import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG_SIZE    = 16
private const val MAX_PAYLOAD = 0x3FFF

enum class CipherMethod(val keySize: Int, val nonceSize: Int) {
    CHACHA20_IETF_POLY1305(32, 12),
    AES_256_GCM(32, 12),
    AES_128_GCM(16, 12);

    companion object {
        fun from(method: String): CipherMethod = when (
            method.trim().lowercase().replace("-", "_")
        ) {
            "chacha20_ietf_poly1305" -> CHACHA20_IETF_POLY1305
            "aes_256_gcm"            -> AES_256_GCM
            "aes_128_gcm"            -> AES_128_GCM
            else -> throw IllegalArgumentException("Unsupported cipher: $method")
        }
    }
}

class AeadWriter(
    private val out: OutputStream,
    private val key: ByteArray,
    private val method: CipherMethod
) {
    private val nonce = ByteArray(method.nonceSize)

    fun write(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val len = minOf(data.size - offset, MAX_PAYLOAD)
            val lenBuf = byteArrayOf((len shr 8).toByte(), (len and 0xFF).toByte())
            out.write(seal(lenBuf))
            out.write(seal(data.copyOfRange(offset, offset + len)))
            offset += len
        }
        out.flush()
    }

    private fun seal(plaintext: ByteArray): ByteArray {
        val ct = aeadEncrypt(key, nonce.clone(), plaintext, method)
        incrementNonce(nonce)
        return ct
    }
}

class AeadReader(
    private val input: InputStream,
    private val key: ByteArray,
    private val method: CipherMethod
) {
    private val nonce = ByteArray(method.nonceSize)

    fun read(): ByteArray {
        val lenCt     = readExact(2 + TAG_SIZE)
        val lenPt     = open(lenCt)
        val payloadLen = ((lenPt[0].toInt() and 0xFF) shl 8) or (lenPt[1].toInt() and 0xFF)
        val payloadCt = readExact(payloadLen + TAG_SIZE)
        return open(payloadCt)
    }

    private fun open(ct: ByteArray): ByteArray {
        val pt = aeadDecrypt(key, nonce.clone(), ct, method)
        incrementNonce(nonce)
        return pt
    }

    private fun readExact(n: Int): ByteArray {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = input.read(buf, off, n - off)
            if (r < 0) throw java.io.EOFException("Connection closed by SS server")
            off += r
        }
        return buf
    }
}

private fun incrementNonce(nonce: ByteArray) {
    for (i in nonce.indices) { if (++nonce[i] != 0.toByte()) break }
}

private fun aeadEncrypt(key: ByteArray, nonce: ByteArray, pt: ByteArray, m: CipherMethod) =
    when (m) {
        CipherMethod.CHACHA20_IETF_POLY1305 ->
            Cipher.getInstance("ChaCha20-Poly1305").also {
                it.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce))
            }.doFinal(pt)
        CipherMethod.AES_256_GCM, CipherMethod.AES_128_GCM ->
            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            }.doFinal(pt)
    }

private fun aeadDecrypt(key: ByteArray, nonce: ByteArray, ct: ByteArray, m: CipherMethod) =
    when (m) {
        CipherMethod.CHACHA20_IETF_POLY1305 ->
            Cipher.getInstance("ChaCha20-Poly1305").also {
                it.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce))
            }.doFinal(ct)
        CipherMethod.AES_256_GCM, CipherMethod.AES_128_GCM ->
            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            }.doFinal(ct)
    }