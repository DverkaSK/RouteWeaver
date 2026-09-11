package ru.dverkask.routeweaver.proxy;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowsocksKeysTest {
    private static final HexFormat HEX = HexFormat.of();

    @Test
    void masterKeyMatchesOpenSslEvpBytesToKey() {
        // Reference values from Python: md5 chaining as in EVP_BytesToKey with no salt.
        assertEquals("b3adc47839e047eb228870526dc8fc30b347287ffca3045dcea06b3fdf090acb",
                HEX.formatHex(ShadowsocksKeys.masterKey("barfoo!", 32)));
        assertEquals("b3adc47839e047eb228870526dc8fc30",
                HEX.formatHex(ShadowsocksKeys.masterKey("barfoo!", 16)));
    }

    @Test
    void hkdfMatchesRfc5869TestCase4() {
        byte[] okm = ShadowsocksKeys.hkdfSha1(
                HEX.parseHex("0b0b0b0b0b0b0b0b0b0b0b"),
                HEX.parseHex("000102030405060708090a0b0c"),
                HEX.parseHex("f0f1f2f3f4f5f6f7f8f9"),
                42);

        assertEquals("085a01ea1b10f36933068b56efa5ad81a4f14b822f5b091568a9cdd4f155fda2c22e422478d305f3f896",
                HEX.formatHex(okm));
    }
}
