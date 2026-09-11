package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dverkask.routeweaver.client.ProxyClient;
import ru.dverkask.routeweaver.client.routing.ServerAddress;
import ru.dverkask.routeweaver.client.runtime.ProxyRuntime;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(
            method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V",
            at = @At("HEAD")
    )
    private static void proxy$prepareRoute(MinecraftClient client,
                                           net.minecraft.client.network.ServerAddress address,
                                           ServerInfo info,
                                           CookieStorage cookieStorage,
                                           CallbackInfo ci) {
        ProxyRuntime runtime = ProxyClient.getRuntime();
        if (runtime == null || address == null) {
            return;
        }

        runtime.prepareServerRoute(new ServerAddress(address.getAddress(), address.getPort()));
    }
}
