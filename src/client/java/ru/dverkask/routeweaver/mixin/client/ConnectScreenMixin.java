package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dverkask.routeweaver.client.ProxyRuntime;
import ru.dverkask.routeweaver.client.RouteWeaverClient;

/**
 * Decides the route while the address is still exactly what the player typed.
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(
            method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V",
            at = @At("HEAD")
    )
    private void routeweaver$prepareRoute(MinecraftClient client, ServerAddress address, ServerInfo info,
                                          CookieStorage cookieStorage, CallbackInfo ci) {
        ProxyRuntime runtime = RouteWeaverClient.runtime();
        if (runtime != null) {
            runtime.prepareConnection(new ru.dverkask.routeweaver.routing.ServerAddress(
                    address.getAddress(), address.getPort()));
        }
    }
}
