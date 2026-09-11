package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
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
            method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V",
            at = @At("HEAD")
    )
    private void routeweaver$prepareRoute(Minecraft minecraft, ServerAddress address, ServerData serverData,
                                          TransferState transferState, CallbackInfo ci) {
        ProxyRuntime runtime = RouteWeaverClient.runtime();
        if (runtime != null) {
            runtime.prepareConnection(new ru.dverkask.routeweaver.routing.ServerAddress(
                    address.getHost(), address.getPort()));
        }
    }
}
