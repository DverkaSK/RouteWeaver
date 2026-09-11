package ru.dverkask.routeweaver.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.dverkask.routeweaver.config.ProxyProfile;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class LocalTunnelTest {
    private static final CipherMethod METHOD = CipherMethod.CHACHA20_IETF_POLY1305;
    private static final String PASSWORD = "correct horse battery staple";

    @Test
    void relaysThroughShadowsocksAndPropagatesTheServerClosing() throws Exception {
        try (FakeShadowsocksServer server = new FakeShadowsocksServer();
             LocalTunnel tunnel = new LocalTunnel(0)) {
            tunnel.start();
            assertTrue(tunnel.getAddress().getAddress().isLoopbackAddress());
            tunnel.setRoute(new TunnelRoute(profileFor(server), "mc.example.net", 25565));

            try (Socket client = new Socket()) {
                client.connect(tunnel.getAddress(), 5_000);
                client.getOutputStream().write("ping".getBytes(StandardCharsets.UTF_8));

                byte[] reply = client.getInputStream().readNBytes(4);

                assertEquals("PING", new String(reply, StandardCharsets.UTF_8));
                assertEquals("mc.example.net:25565", server.requestedTarget.get(5, TimeUnit.SECONDS));
                assertEquals(-1, client.getInputStream().read(), "server close must reach the client");
            }
        }
    }

    @Test
    void refusesConnectionsWithoutARoute() throws Exception {
        try (LocalTunnel tunnel = new LocalTunnel(0)) {
            tunnel.start();

            try (Socket client = new Socket()) {
                client.connect(tunnel.getAddress(), 5_000);
                assertEquals(-1, client.getInputStream().read());
            }
        }
    }

    private static ProxyProfile profileFor(FakeShadowsocksServer server) {
        ProxyProfile profile = new ProxyProfile();
        profile.setId("test");
        profile.setHost("127.0.0.1");
        profile.setPort(server.socket.getLocalPort());
        profile.setMethod(METHOD.getId());
        profile.setPassword(PASSWORD);
        return profile;
    }

    /**
     * Accepts one connection, records the requested target, answers the first payload in upper
     * case and hangs up.
     */
    private static final class FakeShadowsocksServer implements AutoCloseable {
        final ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        final CompletableFuture<String> requestedTarget = new CompletableFuture<>();

        FakeShadowsocksServer() throws IOException {
            Thread thread = new Thread(this::serveOne, "fake-shadowsocks");
            thread.setDaemon(true);
            thread.start();
        }

        private void serveOne() {
            try (Socket connection = socket.accept()) {
                byte[] masterKey = ShadowsocksKeys.masterKey(PASSWORD, METHOD.getKeySize());
                DataInputStream in = new DataInputStream(connection.getInputStream());
                byte[] clientSalt = new byte[METHOD.getSaltSize()];
                in.readFully(clientSalt);
                AeadChunkReader reader = new AeadChunkReader(in, METHOD,
                        ShadowsocksKeys.sessionSubkey(masterKey, clientSalt, METHOD.getKeySize()));

                requestedTarget.complete(decodeDomainTarget(reader.read()));
                String payload = new String(reader.read(), StandardCharsets.UTF_8);

                byte[] serverSalt = new byte[METHOD.getSaltSize()];
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                out.write(serverSalt);
                new AeadChunkWriter(out, METHOD, ShadowsocksKeys.sessionSubkey(masterKey, serverSalt, METHOD.getKeySize()))
                        .write(payload.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            } catch (Exception exception) {
                requestedTarget.completeExceptionally(exception);
            }
        }

        private static String decodeDomainTarget(byte[] address) {
            assertEquals(0x03, address[0]);
            int length = address[1];
            String host = new String(address, 2, length, StandardCharsets.US_ASCII);
            int port = ((address[2 + length] & 0xFF) << 8) | (address[3 + length] & 0xFF);
            return host + ":" + port;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
