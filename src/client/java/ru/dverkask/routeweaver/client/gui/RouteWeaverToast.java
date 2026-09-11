package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Custom toast notification for RouteWeaver.
 * Features: mod color scheme, thin progress bar at bottom, close on hover+click.
 */
public class RouteWeaverToast implements Toast {

    private static final int  WIDTH    = 210;
    private static final int  HEIGHT   = 52;
    private static final long DURATION = 5_000L; // ms

    // Colors matching ProxyConfigScreen palette
    private static final int BG        = 0xFF111214;
    private static final int OUTLINE   = 0xFF2A2F31;
    private static final int ACCENT    = 0xFFC9F24A;
    private static final int C_TEXT    = 0xFFF5F5F2;
    private static final int C_MUTED   = 0xFF999B99;
    private static final int C_CLOSE   = 0xFFFF8E8E;
    private static final int BAR_TRACK = 0xFF1A1C20;

    private final Text title;
    private final Text description;

    private boolean dismissed  = false;
    private boolean wasPressed = false;
    private boolean hovered    = false;
    private float   progress   = 1f;

    public RouteWeaverToast(Text title, Text description) {
        this.title       = title;
        this.description = description;
    }

    // ─── Toast interface ──────────────────────────────────────────────────────

    @Override public int getWidth()  { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    public Toast.Visibility getVisibility() {
        return dismissed ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void update(ToastManager manager, long startTime) {
        if (startTime >= DURATION) {
            dismissed = true;
            progress  = 0f;
            return;
        }
        progress = 1f - (startTime / (float) DURATION);

        // ── Hover + click detection ────────────────────────────────────────────
        MinecraftClient mc    = manager.getClient();
        double          scale = mc.getWindow().getScaleFactor();
        double          mx    = mc.mouse.getX() / scale;
        double          my    = mc.mouse.getY() / scale;
        int             sw    = mc.getWindow().getScaledWidth();

        // Toast is in top-right corner; use generous X range to account for slide
        hovered = mx > sw - WIDTH - 20
               && mx < sw + 4
               && my >= 0 && my < HEIGHT + 8;

        boolean pressed = GLFW.glfwGetMouseButton(
                mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (hovered && pressed && !wasPressed) {
            dismissed = true;
        }
        wasPressed = pressed;
    }

    @Override
    public void draw(DrawContext ctx, TextRenderer tr, long startTime) {
        // NOTE: do NOT return early when dismissed — ToastManager Entry still
        // animates the slide-out, and draw() is called during that phase.
        // Returning early would make it invisible while still animating.

        // ── Background ─────────────────────────────────────────────────────────
        ctx.fill(0, 0, WIDTH, HEIGHT, BG);

        // Gray outline (1px border, same as other UI cards)
        ctx.fill(0, 0, WIDTH, 1,              OUTLINE); // top
        ctx.fill(0, HEIGHT - 1, WIDTH, HEIGHT, OUTLINE); // bottom
        ctx.fill(0, 0, 1, HEIGHT,             OUTLINE); // left
        ctx.fill(WIDTH - 1, 0, WIDTH, HEIGHT, OUTLINE); // right

        // ── Text ───────────────────────────────────────────────────────────────
        ctx.drawText(tr, title,       8, 10, C_TEXT,  false);
        ctx.drawText(tr, description, 8, 24, C_MUTED, false);

        // ── Progress bar — 2px strip just above the bottom outline ────────────────
        ctx.fill(1, HEIGHT - 3, WIDTH - 1, HEIGHT - 1, BAR_TRACK);
        int barW = (int) ((WIDTH - 2) * progress);
        if (barW > 0) {
            ctx.fill(1, HEIGHT - 3, 1 + barW, HEIGHT - 1, ACCENT);
        }

        // ── Close hint (shown on hover) — simple "x" in top-right ────────────────
        if (hovered) {
            ctx.drawText(tr, Text.literal("x"), WIDTH - 11, 4, C_CLOSE, false);
        }
    }

    // ─── Factory ───────────────────────────────────────────────────────────────

    public static void show(Text title, Text description) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getToastManager() == null) return;
        // Skip if an identical toast is already on screen
        if (mc.getToastManager().getToast(RouteWeaverToast.class, Toast.TYPE) != null) return;
        mc.getToastManager().add(new RouteWeaverToast(title, description));
    }
}
