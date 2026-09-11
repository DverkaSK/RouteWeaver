package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A toast in the RouteWeaver palette with a countdown bar; clicking it dismisses it early.
 */
public final class RouteWeaverToast implements Toast {
    private static final int WIDTH = 210;
    private static final int HEIGHT = 52;
    private static final long DURATION_MS = 5_000L;
    private static final int BACKGROUND = 0xFF111214;
    private static final int PROGRESS_TRACK = 0xFF1A1C20;

    private final Component title;
    private final Component description;

    private boolean dismissed;
    private boolean mouseWasDown;
    private boolean hovered;
    private float remaining = 1f;

    private RouteWeaverToast(Component title, Component description) {
        this.title = title;
        this.description = description;
    }

    /**
     * Shows the toast unless one is already on screen.
     */
    public static void show(Component title, Component description) {
        ToastManager toasts = Minecraft.getInstance().getToastManager();
        if (toasts.getToast(RouteWeaverToast.class, Toast.NO_TOKEN) == null) {
            toasts.addToast(new RouteWeaverToast(title, description));
        }
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return dismissed ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void update(ToastManager manager, long elapsedMs) {
        if (elapsedMs >= DURATION_MS) {
            dismissed = true;
            remaining = 0f;
            return;
        }
        remaining = 1f - elapsedMs / (float) DURATION_MS;

        // Toasts get no input events, so poll the mouse. The hit box is generous to cover the
        // slide-in animation in the top-right corner.
        Minecraft minecraft = manager.getMinecraft();
        double scale = minecraft.getWindow().getGuiScale();
        double mouseX = minecraft.mouseHandler.xpos() / scale;
        double mouseY = minecraft.mouseHandler.ypos() / scale;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        hovered = mouseX > screenWidth - WIDTH - 20 && mouseY >= 0 && mouseY < HEIGHT + 8;

        boolean mouseDown = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
        if (hovered && mouseDown && !mouseWasDown) {
            dismissed = true;
        }
        mouseWasDown = mouseDown;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, long elapsedMs) {
        // Keep drawing after dismissal: the toast manager still animates the slide-out.
        graphics.fill(0, 0, WIDTH, HEIGHT, BACKGROUND);
        graphics.fill(0, 0, WIDTH, 1, Theme.OUTLINE);
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, Theme.OUTLINE);
        graphics.fill(0, 0, 1, HEIGHT, Theme.OUTLINE);
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, Theme.OUTLINE);

        graphics.drawString(font, title, 8, 10, Theme.TEXT.argb(), false);
        graphics.drawString(font, description, 8, 24, Theme.MUTED.argb(), false);

        graphics.fill(1, HEIGHT - 3, WIDTH - 1, HEIGHT - 1, PROGRESS_TRACK);
        int progressWidth = (int) ((WIDTH - 2) * remaining);
        if (progressWidth > 0) {
            graphics.fill(1, HEIGHT - 3, 1 + progressWidth, HEIGHT - 1, Theme.ACCENT.argb());
        }

        if (hovered) {
            graphics.drawString(font, Component.literal("x"), WIDTH - 11, 4, Theme.ERROR.argb(), false);
        }
    }
}
