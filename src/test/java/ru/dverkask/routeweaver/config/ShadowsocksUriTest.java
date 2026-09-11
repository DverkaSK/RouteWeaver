package ru.dverkask.routeweaver.config;

import org.junit.jupiter.api.Test;
import ru.dverkask.routeweaver.config.InvalidShadowsocksUriException.Reason;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShadowsocksUriTest {
    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesSip002WithBase64UserInfo() {
        String link = "ss://" + base64Url("chacha20-ietf-poly1305:s3cr3t+/=") + "@1.2.3.4:8388#My%20Server";

        ShadowsocksUri uri = ShadowsocksUri.parse(link);

        assertEquals(new ShadowsocksUri("My Server", "1.2.3.4", 8388, "chacha20-ietf-poly1305", "s3cr3t+/="), uri);
    }

    @Test
    void parsesSip002WithPercentEncodedUserInfoAndKeepsPlusSigns() {
        ShadowsocksUri uri = ShadowsocksUri.parse("ss://aes-256-gcm:pa%3Ass+word@example.com:443");

        assertEquals("aes-256-gcm", uri.method());
        assertEquals("pa:ss+word", uri.password());
        assertEquals("example.com", uri.host());
        assertEquals("", uri.name());
    }

    @Test
    void parsesLegacyFullyEncodedForm() {
        String link = "ss://" + Base64.getEncoder().encodeToString(
                "aes-128-gcm:hunter2@example.org:8443".getBytes(StandardCharsets.UTF_8)) + "#legacy";

        ShadowsocksUri uri = ShadowsocksUri.parse(link);

        assertEquals(new ShadowsocksUri("legacy", "example.org", 8443, "aes-128-gcm", "hunter2"), uri);
    }

    @Test
    void acceptsTrailingSlashAndEmptyQuery() {
        ShadowsocksUri uri = ShadowsocksUri.parse("ss://" + base64Url("aes-256-gcm:pw") + "@host.net:1234/?");

        assertEquals("host.net", uri.host());
        assertEquals(1234, uri.port());
    }

    @Test
    void stripsIpv6Brackets() {
        ShadowsocksUri uri = ShadowsocksUri.parse("ss://" + base64Url("aes-256-gcm:pw") + "@[2001:db8::1]:8388");

        assertEquals("2001:db8::1", uri.host());
        assertEquals(8388, uri.port());
    }

    @Test
    void rejectsPlugins() {
        assertReason(Reason.PLUGIN_UNSUPPORTED,
                "ss://" + base64Url("aes-256-gcm:pw") + "@host:1/?plugin=obfs-local%3Bobfs%3Dhttp");
    }

    @Test
    void reportsWhatIsWrong() {
        assertReason(Reason.EMPTY, "  ");
        assertReason(Reason.NOT_SHADOWSOCKS, "vless://abc@host:1");
        assertReason(Reason.MALFORMED, "ss://%%%");
        assertReason(Reason.MISSING_CREDENTIALS, "ss://" + base64Url("aes-256-gcm") + "@host:1");
        assertReason(Reason.MISSING_PORT, "ss://" + base64Url("aes-256-gcm:pw") + "@host");
        assertReason(Reason.INVALID_PORT, "ss://" + base64Url("aes-256-gcm:pw") + "@host:70000");
    }

    private static void assertReason(Reason expected, String link) {
        InvalidShadowsocksUriException exception =
                assertThrows(InvalidShadowsocksUriException.class, () -> ShadowsocksUri.parse(link));
        assertEquals(expected, exception.reason());
    }
}
