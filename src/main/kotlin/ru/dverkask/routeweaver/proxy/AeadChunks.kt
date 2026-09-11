package ru.dverkask.routeweaver.proxy

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** Largest payload a single Shadowsocks AEAD chunk may carry. */
const val MAX_CHUNK_PAYLOAD = 0x3FFF

private const val LENGTH_SIZE = 2

/**
 * Encrypts a byte stream into Shadowsocks AEAD chunks: `[encrypted length][encrypted payload]`.
 * Callers should hand in a buffered stream; each [write] flushes once at the end.
 */
class AeadChunkWriter(private val out: OutputStream, method: CipherMethod, key: ByteArray) {
    private val cipher = NonceSequenceCipher(method, key, Cipher.ENCRYPT_MODE)
    private val lengthBuffer = ByteArray(LENGTH_SIZE)

    @JvmOverloads
    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        var position = offset
        val end = offset + length
        while (position < end) {
            val chunk = minOf(end - position, MAX_CHUNK_PAYLOAD)
            lengthBuffer[0] = (chunk ushr 8).toByte()
            lengthBuffer[1] = chunk.toByte()
            out.write(cipher.process(lengthBuffer, 0, LENGTH_SIZE))
            out.write(cipher.process(data, position, chunk))
            position += chunk
        }
        out.flush()
    }
}

/**
 * Decrypts Shadowsocks AEAD chunks produced by the peer's [AeadChunkWriter].
 */
class AeadChunkReader(private val input: InputStream, method: CipherMethod, key: ByteArray) {
    private val cipher = NonceSequenceCipher(method, key, Cipher.DECRYPT_MODE)

    /** Returns the next payload, or null when the peer closed the stream between chunks. */
    fun read(): ByteArray? {
        val encryptedLength = readFully(LENGTH_SIZE + CipherMethod.TAG_SIZE, allowCleanEof = true) ?: return null
        val lengthBytes = cipher.process(encryptedLength, 0, encryptedLength.size)
        val payloadLength = ((lengthBytes[0].toInt() and 0xFF) shl 8) or (lengthBytes[1].toInt() and 0xFF)
        if (payloadLength > MAX_CHUNK_PAYLOAD) {
            throw IOException("Chunk length $payloadLength exceeds the protocol limit")
        }
        val encryptedPayload = readFully(payloadLength + CipherMethod.TAG_SIZE, allowCleanEof = false)!!
        return cipher.process(encryptedPayload, 0, encryptedPayload.size)
    }

    private fun readFully(size: Int, allowCleanEof: Boolean): ByteArray? {
        val buffer = ByteArray(size)
        var filled = 0
        while (filled < size) {
            val read = input.read(buffer, filled, size - filled)
            if (read < 0) {
                if (filled == 0 && allowCleanEof) return null
                throw EOFException("Stream ended inside an AEAD chunk")
            }
            filled += read
        }
        return buffer
    }
}

/**
 * One AEAD cipher whose little-endian nonce starts at zero and increments after every operation.
 */
private class NonceSequenceCipher(private val method: CipherMethod, key: ByteArray, private val mode: Int) {
    private val cipher = Cipher.getInstance(method.transformation)
    private val key = SecretKeySpec(key, method.keyAlgorithm)
    private val nonce = ByteArray(CipherMethod.NONCE_SIZE)

    fun process(input: ByteArray, offset: Int, length: Int): ByteArray {
        cipher.init(mode, key, method.parameterSpec(nonce))
        val output = cipher.doFinal(input, offset, length)
        incrementNonce()
        return output
    }

    private fun incrementNonce() {
        for (i in nonce.indices) {
            nonce[i]++
            if (nonce[i] != 0.toByte()) return
        }
    }
}
