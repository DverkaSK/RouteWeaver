package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.dverkask.routeweaver.RouteWeaver;
import ru.dverkask.routeweaver.client.ProxyClient;
import ru.dverkask.routeweaver.client.runtime.ProxyRuntime;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @ModifyVariable(
            method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/network/NetworkingBackend;Lnet/minecraft/util/profiler/MultiValueDebugSampleLogImpl;)Lnet/minecraft/network/ClientConnection;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static InetSocketAddress proxy$redirectConnectTargetForPinger(InetSocketAddress address,
                                                                          InetSocketAddress originalAddress,
                                                                          NetworkingBackend networkingBackend,
                                                                          MultiValueDebugSampleLogImpl debugSampleLog) {
        return proxy$resolveRedirect(address);
    }

    @ModifyVariable(
            method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/network/NetworkingBackend;Lnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static InetSocketAddress proxy$redirectConnectTargetForLogin(InetSocketAddress address,
                                                                         InetSocketAddress originalAddress,
                                                                         NetworkingBackend networkingBackend,
                                                                         ClientConnection connection) {
        return proxy$resolveRedirect(address);
    }

    private static InetSocketAddress proxy$resolveRedirect(InetSocketAddress address) {
        ProxyRuntime runtime = ProxyClient.getRuntime();
        if (runtime == null || address == null) {
            return address;
        }

        InetSocketAddress redirected = runtime.resolveConnectTarget(address.getHostString(), address.getPort());
        if (redirected != null) {
            RouteWeaver.LOGGER.info("Redirecting connection {}:{} to agent tunnel {}:{}",
                    address.getHostString(), address.getPort(), redirected.getHostString(), redirected.getPort());
            return redirected;
        }
        return address;
    }
}
