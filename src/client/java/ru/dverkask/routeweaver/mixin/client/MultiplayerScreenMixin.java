package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Unique
    private static final int BUTTON_WIDTH = 80;
    @Unique
    private static final int MARGIN = 8;

    @Unique
    private ButtonWidget routeweaver$openButton;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void routeweaver$addOpenButton(CallbackInfo ci) {
        ProxyRuntime runtime = RouteWeaverClient.runtime();
        if (runtime == null) {
            return;
        }
        Screen multiplayerScreen = this;
        routeweaver$openButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("routeweaver.button.open"),
                        button -> client.setScreen(new RouteWeaverScreen(multiplayerScreen, runtime)))
                .dimensions(width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, 20)
                .build());
    }

    @Inject(method = "refreshWidgetPositions", at = @At("TAIL"))
    private void routeweaver$keepButtonInCorner(CallbackInfo ci) {
        if (routeweaver$openButton != null) {
            routeweaver$openButton.setX(width - BUTTON_WIDTH - MARGIN);
        }
    }
}
