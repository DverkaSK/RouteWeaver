package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
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

    private final Text title;
    private final Text description;

    private boolean dismissed;
    private boolean mouseWasDown;
    private boolean hovered;
    private float remaining = 1f;

    private RouteWeaverToast(Text title, Text description) {
        this.title = title;
        this.description = description;
    }

    /**
     * Shows the toast unless one is already on screen.
     */
    public static void show(Text title, Text description) {
        ToastManager toasts = MinecraftClient.getInstance().getToastManager();
        if (toasts.getToast(RouteWeaverToast.class, Toast.TYPE) == null) {
            toasts.add(new RouteWeaverToast(title, description));
        }
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Toast.Visibility getVisibility() {
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
        MinecraftClient client = manager.getClient();
        double scale = client.getWindow().getScaleFactor();
        double mouseX = client.mouse.getX() / scale;
        double mouseY = client.mouse.getY() / scale;
        int screenWidth = client.getWindow().getScaledWidth();
        hovered = mouseX > screenWidth - WIDTH - 20 && mouseY >= 0 && mouseY < HEIGHT + 8;

        boolean mouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
        if (hovered && mouseDown && !mouseWasDown) {
            dismissed = true;
        }
        mouseWasDown = mouseDown;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long elapsedMs) {
        // Keep drawing after dismissal: the toast manager still animates the slide-out.
        context.fill(0, 0, WIDTH, HEIGHT, BACKGROUND);
        context.fill(0, 0, WIDTH, 1, Theme.OUTLINE);
        context.fill(0, HEIGHT - 1, WIDTH, HEIGHT, Theme.OUTLINE);
        context.fill(0, 0, 1, HEIGHT, Theme.OUTLINE);
        context.fill(WIDTH - 1, 0, WIDTH, HEIGHT, Theme.OUTLINE);

        context.drawText(textRenderer, title, 8, 10, Theme.TEXT.argb(), false);
        context.drawText(textRenderer, description, 8, 24, Theme.MUTED.argb(), false);

        context.fill(1, HEIGHT - 3, WIDTH - 1, HEIGHT - 1, PROGRESS_TRACK);
        int progressWidth = (int) ((WIDTH - 2) * remaining);
        if (progressWidth > 0) {
            context.fill(1, HEIGHT - 3, 1 + progressWidth, HEIGHT - 1, Theme.ACCENT.argb());
        }

        if (hovered) {
            context.drawText(textRenderer, Text.literal("x"), WIDTH - 11, 4, Theme.ERROR.argb(), false);
        }
    }
}
