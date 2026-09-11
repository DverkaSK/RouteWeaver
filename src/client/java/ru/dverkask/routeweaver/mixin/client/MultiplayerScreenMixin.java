package ru.dverkask.routeweaver.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dverkask.routeweaver.client.ProxyClient;
import ru.dverkask.routeweaver.client.gui.ProxyConfigScreen;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void proxy$addConfigButton(CallbackInfo ci) {
        if (this.client == null || ProxyClient.getRuntime() == null) {
            return;
        }

        int btnWidth = 80;
        int x = this.width - btnWidth - 8;
        int y = 8;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("RouteWeaver"), button ->
                        this.client.setScreen(new ProxyConfigScreen((Screen) (Object) this, ProxyClient.getRuntime())))
                .dimensions(x, y, btnWidth, 20)
                .build());
    }

    @Inject(method = "refreshWidgetPositions", at = @At("TAIL"))
    private void proxy$fixButtonPosition(CallbackInfo ci) {
        this.children().stream()
                .filter(child -> child instanceof ButtonWidget)
                .map(child -> (ButtonWidget) child)
                .filter(btn -> btn.getMessage().getString().equals("RouteWeaver"))
                .findFirst()
                .ifPresent(btn -> {
                    btn.setX(this.width - btn.getWidth() - 8);
                });
    }
}
