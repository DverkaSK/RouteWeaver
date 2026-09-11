package ru.dverkask.routeweaver.proxy;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocksAddressTest {
    private static String encode(String host, int port) {
        return HexFormat.of().formatHex(SocksAddress.encode(host, port));
    }

    @Test
    void encodesIpv4Literals() {
        assertEquals("01" + "7f000001" + "63dd", encode("127.0.0.1", 25565));
    }

    @Test
    void encodesDomains() {
        assertEquals("03" + "07" + "6d632e74657374" + "63dd", encode("mc.test", 25565));
    }

    @Test
    void treatsOutOfRangeOctetsAsADomain() {
        assertEquals("03", encode("999.1.1.1", 1).substring(0, 2));
    }

    @Test
    void encodesIpv6Literals() {
        assertEquals("04" + "20010db8000000000000000000000001" + "0001", encode("2001:db8::1", 1));
        assertEquals("04", encode("[::1]", 1).substring(0, 2));
    }

    @Test
    void rejectsInvalidPorts() {
        assertThrows(IllegalArgumentException.class, () -> SocksAddress.encode("mc.test", 0));
    }
}
