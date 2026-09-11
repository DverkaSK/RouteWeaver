package ru.dverkask.routeweaver.proxy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AeadChunksTest {
    private static final int TAG = CipherMethod.TAG_SIZE;
    private static final int MAX_PAYLOAD = AeadChunksKt.MAX_CHUNK_PAYLOAD;

    private static byte[] key(CipherMethod method) {
        byte[] key = new byte[method.getKeySize()];
        Arrays.fill(key, (byte) 7);
        return key;
    }

    @ParameterizedTest
    @EnumSource(CipherMethod.class)
    void roundTripsPayloadsLargerThanOneChunk(CipherMethod method) throws Exception {
        byte[] payload = new byte[MAX_PAYLOAD * 2 + 123];
        new Random(42).nextBytes(payload);
        ByteArrayOutputStream wire = new ByteArrayOutputStream();

        new AeadChunkWriter(wire, method, key(method)).write(payload);

        // Three chunks, each an encrypted 2-byte length plus the encrypted payload.
        assertEquals(3 * (2 + TAG) + payload.length + 3 * TAG, wire.size());
        AeadChunkReader reader = new AeadChunkReader(new ByteArrayInputStream(wire.toByteArray()), method, key(method));
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        for (byte[] chunk = reader.read(); chunk != null; chunk = reader.read()) {
            decoded.write(chunk);
        }
        assertArrayEquals(payload, decoded.toByteArray());
    }

    @ParameterizedTest
    @EnumSource(CipherMethod.class)
    void usesACountingLittleEndianNonce(CipherMethod method) throws Exception {
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        new AeadChunkWriter(wire, method, key(method)).write(new byte[]{1, 2, 3});
        byte[] bytes = wire.toByteArray();

        byte[] length = decrypt(method, 0, Arrays.copyOfRange(bytes, 0, 2 + TAG));
        byte[] payload = decrypt(method, 1, Arrays.copyOfRange(bytes, 2 + TAG, bytes.length));

        assertArrayEquals(new byte[]{0, 3}, length);
        assertArrayEquals(new byte[]{1, 2, 3}, payload);
    }

    @ParameterizedTest
    @EnumSource(CipherMethod.class)
    void rejectsTamperedChunks(CipherMethod method) {
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        new AeadChunkWriter(wire, method, key(method)).write(new byte[]{1, 2, 3});
        byte[] bytes = wire.toByteArray();
        bytes[bytes.length - 1] ^= 1;

        AeadChunkReader reader = new AeadChunkReader(new ByteArrayInputStream(bytes), method, key(method));
        assertThrows(Exception.class, reader::read);
    }

    @ParameterizedTest
    @EnumSource(CipherMethod.class)
    void distinguishesCleanEndFromTruncation(CipherMethod method) {
        assertNull(new AeadChunkReader(new ByteArrayInputStream(new byte[0]), method, key(method)).read());

        AeadChunkReader truncated = new AeadChunkReader(new ByteArrayInputStream(new byte[5]), method, key(method));
        assertThrows(EOFException.class, truncated::read);
    }

    /** Decrypts one AEAD block with plain JCE, independently of the production cipher setup. */
    private static byte[] decrypt(CipherMethod method, int counter, byte[] ciphertext) throws Exception {
        byte[] nonce = new byte[CipherMethod.NONCE_SIZE];
        nonce[0] = (byte) counter;
        Cipher cipher;
        if (method == CipherMethod.CHACHA20_IETF_POLY1305) {
            cipher = Cipher.getInstance("ChaCha20-Poly1305");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(method), "ChaCha20"), new IvParameterSpec(nonce));
        } else {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(method), "AES"), new GCMParameterSpec(TAG * 8, nonce));
        }
        return cipher.doFinal(ciphertext);
    }
}
