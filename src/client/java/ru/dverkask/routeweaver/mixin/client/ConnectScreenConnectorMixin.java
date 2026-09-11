package ru.dverkask.routeweaver.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ru.dverkask.routeweaver.client.ProxyRuntime;
import ru.dverkask.routeweaver.client.RouteWeaverClient;

import java.net.InetSocketAddress;

/**
 * Swaps the socket address inside the connector thread that {@code ConnectScreen} starts. Only
 * joining a server goes through here; the server-list pinger opens its sockets elsewhere.
 */
@Mixin(targets = "net.minecraft.client.gui.screen.multiplayer.ConnectScreen$1")
public abstract class ConnectScreenConnectorMixin {
    @ModifyArg(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Lnet/minecraft/network/NetworkingBackend;Lnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;"
            ),
            index = 0
    )
    private InetSocketAddress routeweaver$redirectThroughTunnel(InetSocketAddress address) {
        ProxyRuntime runtime = RouteWeaverClient.runtime();
        return runtime == null ? address : runtime.redirectConnection(address);
    }
}
