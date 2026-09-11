package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dverkask.routeweaver.client.ProxyRuntime;
import ru.dverkask.routeweaver.client.RouteWeaverClient;
import ru.dverkask.routeweaver.client.gui.RouteWeaverScreen;

/**
 * Adds the "RouteWeaver" button to the top-right corner of the multiplayer screen.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Unique
    private static final int BUTTON_WIDTH = 80;
    @Unique
    private static final int MARGIN = 8;

    @Unique
    private Button routeweaver$openButton;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void routeweaver$addOpenButton(CallbackInfo ci) {
        ProxyRuntime runtime = RouteWeaverClient.runtime();
        if (runtime == null) {
            return;
        }
        Screen multiplayerScreen = this;
        routeweaver$openButton = addRenderableWidget(Button.builder(
                        Component.translatable("routeweaver.button.open"),
                        button -> minecraft.setScreen(new RouteWeaverScreen(multiplayerScreen, runtime)))
                .bounds(width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, 20)
                .build());
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void routeweaver$keepButtonInCorner(CallbackInfo ci) {
        if (routeweaver$openButton != null) {
            routeweaver$openButton.setX(width - BUTTON_WIDTH - MARGIN);
        }
    }
}
