package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.dverkask.routeweaver.client.config.ProxyProfile;
import ru.dverkask.routeweaver.client.config.RoutingRule;
import ru.dverkask.routeweaver.client.config.ConfigIds;
import ru.dverkask.routeweaver.client.runtime.ProxyRuntime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class RoutingRuleEditScreen extends Screen {
    private static final int PANEL_WIDTH   = 440;
    private static final int FIELD_HEIGHT  = 20;
    private static final int ROW_GAP       = 32;

    // Colours (must match vanilla integers since this is a vanilla Screen)
    private static final int COL_BG        = 0xC0101010;
    private static final int COL_PANEL     = 0x70181818;
    private static final int COL_FIELD_BG  = 0x60303030;
    private static final int COL_ACCENT    = 0xFFC9F24A;
    private static final int COL_MUTED     = 0xFF888888;
    private static final int COL_SEL_BG    = 0xFF1C2B1C;
    private static final int COL_ROW_HOV   = 0xFF222B22;
    private static final int COL_ERROR     = 0xFFFF8080;

    private final Screen parent;
    private final ProxyRuntime runtime;
    private final Consumer<RoutingRule> onSave;
    private final RoutingRule editing;

    private TextFieldWidget idField;
    private TextFieldWidget labelField;
    private TextFieldWidget patternField;
    private TextFieldWidget portField;

    // Profile picker state (replaces cycling button)
    private boolean pickerOpen = false;
    private int hoveredProfileIndex = -1;

    private ButtonWidget enabledButton;
    private String validationError = "";

    private String selectedProfileId;
    private boolean enabled;

    public RoutingRuleEditScreen(Screen parent, ProxyRuntime runtime,
                                 RoutingRule original, Consumer<RoutingRule> onSave) {
        super(Text.literal(original == null ? "Add rule" : "Edit rule"));
        this.parent  = parent;
        this.runtime = runtime;
        this.onSave  = onSave;
        this.editing = copyOf(original);
        this.enabled = editing.isEnabled();
        this.selectedProfileId = resolveProfileId(
                runtime.getConfig().getProfiles(), editing.getProxyProfileId());
    }

    @Override
    protected void init() {
        int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        int left      = panelLeft + 14;
        int right     = panelLeft + PANEL_WIDTH / 2 + 10;
        int fieldW    = PANEL_WIDTH / 2 - 24;
        int y         = 80;

        this.idField    = addField(left,  y, fieldW, editing.getId(),    "rule-id");
        this.labelField = addField(right, y, fieldW, editing.getLabel(), "My target server");
        y += ROW_GAP + 8;

        this.patternField = addField(left,  y, fieldW, editing.getServerPattern(),
                "mc.example.net  or  *.example.net");
        this.portField    = addField(right, y, fieldW,
                editing.getPort() > 0 ? String.valueOf(editing.getPort()) : "",
                "leave blank or 25565");
        y += ROW_GAP + 8;

        // The profile picker button just opens/closes the dropdown
        // (rendered manually in render())

        int enabledY = profilePickerBottom() + 8;
        this.enabledButton = addDrawableChild(ButtonWidget.builder(enabledText(), btn -> {
            this.enabled = !this.enabled;
            btn.setMessage(enabledText());
        }).dimensions(left, enabledY, fieldW, FIELD_HEIGHT).build());

        int footerY = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"),   b -> save())
                .dimensions(this.width / 2 - 75, footerY, 70, FIELD_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(this.width / 2 + 5,  footerY, 70, FIELD_HEIGHT).build());
    }

    // ─── layout helpers ───────────────────────────────────────────────────────

    /** Top-left Y of the profile picker widget. */
    private int pickerY() {
        // Below the two field rows
        int panelTop = 36;
        return panelTop + 80 + ROW_GAP + 8 + ROW_GAP + 10;
    }

    private int pickerX() { return this.width / 2 - PANEL_WIDTH / 2 + 14; }

    private int pickerW() { return PANEL_WIDTH / 2 - 24; }

    /** Y coordinate just below the profile picker (collapsed or expanded). */
    private int profilePickerBottom() {
        List<ProxyProfile> profiles = runtime.getConfig().getProfiles();
        int rows = pickerOpen ? Math.min(profiles.size(), 5) : 1;
        return pickerY() + rows * (FIELD_HEIGHT + 2) + 4;
    }

    // ─── rendering ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int panelLeft  = this.width / 2 - PANEL_WIDTH / 2;
        int panelRight = this.width / 2 + PANEL_WIDTH / 2;

        ctx.fill(0, 0, this.width, this.height, COL_BG);
        ctx.fill(panelLeft - 8, 26, panelRight + 8, this.height - 36, COL_PANEL);
        ctx.fill(panelLeft - 4, 30, panelRight + 4, 76, 0x60303030);
        ctx.fill(panelLeft - 4, 78, panelRight + 4, this.height - 70, 0x40242424);

        super.render(ctx, mx, my, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                "Advanced routing rule — pattern matching and port filtering",
                panelLeft, 22, 0xFFFFFFFF);

        drawFieldLabel(ctx, "Rule ID",        idField);
        drawFieldLabel(ctx, "Rule Name",      labelField);
        drawFieldLabel(ctx, "Server Pattern", patternField);
        drawFieldLabel(ctx, "Port Filter",    portField);
        drawPlaceholder(ctx, idField,      "rule-id");
        drawPlaceholder(ctx, labelField,   "My target server");
        drawPlaceholder(ctx, patternField, "mc.example.net  or  *.example.net");
        drawPlaceholder(ctx, portField,    "leave blank or 25565");

        // ── Proxy profile picker ──────────────────────────────────────────────
        int px = pickerX();
        int py = pickerY();
        int pw = pickerW();

        ctx.drawTextWithShadow(textRenderer, "Proxy profile", px, py - 12, COL_ACCENT);

        List<ProxyProfile> profiles = runtime.getConfig().getProfiles();
        if (profiles.isEmpty()) {
            ctx.fill(px, py, px + pw, py + FIELD_HEIGHT, 0x40333333);
            ctx.drawTextWithShadow(textRenderer, "No profiles — add one in Proxies tab",
                    px + 4, py + 6, COL_MUTED);
        } else if (!pickerOpen) {
            // Collapsed: show selected profile
            boolean hovHeader = mx >= px && mx <= px + pw && my >= py && my <= py + FIELD_HEIGHT;
            ctx.fill(px, py, px + pw, py + FIELD_HEIGHT, hovHeader ? COL_ROW_HOV : 0xFF1C1C1C);
            ctx.fill(px, py, px + 1, py + FIELD_HEIGHT, COL_ACCENT);           // accent left bar
            ctx.fill(px, py, px + pw, py + 1, 0xFF333333);
            ctx.fill(px, pw + px - 1, px + pw, py + FIELD_HEIGHT, 0xFF333333);
            String name = selectedProfileName(profiles);
            ctx.drawTextWithShadow(textRenderer, name, px + 6, py + 6, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "▾", px + pw - 10, py + 6, COL_ACCENT);
        } else {
            // Expanded: show list
            int visibleCount = Math.min(profiles.size(), 5);
            for (int i = 0; i < visibleCount; i++) {
                ProxyProfile p  = profiles.get(i);
                boolean isSel   = p.getId().equals(selectedProfileId);
                boolean isHov   = (i == hoveredProfileIndex);
                int rowY = py + i * (FIELD_HEIGHT + 2);
                int bg   = isSel ? COL_SEL_BG : (isHov ? COL_ROW_HOV : 0xFF1A1A1A);
                ctx.fill(px, rowY, px + pw, rowY + FIELD_HEIGHT, bg);
                if (isSel) ctx.fill(px, rowY, px + 1, rowY + FIELD_HEIGHT, COL_ACCENT);
                ctx.fill(px, rowY, px + pw, rowY + 1, 0xFF333333);
                ctx.drawTextWithShadow(textRenderer,
                        trim(p.getName().isBlank() ? p.getId() : p.getName(), 36),
                        px + 6, rowY + 6,
                        isSel ? COL_ACCENT : 0xFFDDDDDD);
                if (isSel) {
                    ctx.drawTextWithShadow(textRenderer, "✓", px + pw - 14, rowY + 6, COL_ACCENT);
                }
                // enabled dot
                int dotColor = p.isEnabled() ? 0xFF7BDF91 : 0xFFFF8E8E;
                ctx.fill(px + pw - 24, rowY + 7, px + pw - 18, rowY + 13, dotColor);
            }
            // Bottom border
            int listBottom = py + visibleCount * (FIELD_HEIGHT + 2);
            ctx.fill(px, listBottom, px + pw, listBottom + 1, 0xFF333333);
        }

        // ── Enabled label ─────────────────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, "State",
                pickerX(), profilePickerBottom() - 4, COL_ACCENT);

        // ── Footer ────────────────────────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer,
                "Examples: hypixel.net,  1.2.3.4,  *.example.net",
                panelLeft, this.height - 46, COL_MUTED);
        if (!validationError.isBlank()) {
            ctx.drawTextWithShadow(textRenderer, validationError,
                    panelLeft, this.height - 58, COL_ERROR);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean consumed) {
        double mx = click.x();
        double my = click.y();
        int px = pickerX();
        int py = pickerY();
        int pw = pickerW();
        List<ProxyProfile> profiles = runtime.getConfig().getProfiles();

        if (!profiles.isEmpty()) {
            if (!pickerOpen) {
                if (mx >= px && mx <= px + pw && my >= py && my <= py + FIELD_HEIGHT) {
                    pickerOpen = true;
                    return true;
                }
            } else {
                int visibleCount = Math.min(profiles.size(), 5);
                for (int i = 0; i < visibleCount; i++) {
                    int rowY = py + i * (FIELD_HEIGHT + 2);
                    if (mx >= px && mx <= px + pw && my >= rowY && my <= rowY + FIELD_HEIGHT) {
                        selectedProfileId = profiles.get(i).getId();
                        pickerOpen = false;
                        return true;
                    }
                }
                pickerOpen = false;
            }
        }

        return super.mouseClicked(click, consumed);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (pickerOpen) {
            int px = pickerX();
            int py = pickerY();
            int pw = pickerW();
            List<ProxyProfile> profiles = runtime.getConfig().getProfiles();
            int visibleCount = Math.min(profiles.size(), 5);
            hoveredProfileIndex = -1;
            for (int i = 0; i < visibleCount; i++) {
                int rowY = py + i * (FIELD_HEIGHT + 2);
                if (mx >= px && mx <= px + pw && my >= rowY && my <= rowY + FIELD_HEIGHT) {
                    hoveredProfileIndex = i;
                    break;
                }
            }
        }
        super.mouseMoved(mx, my);
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    private void save() {
        String pattern = patternField.getText().trim();
        if (pattern.isBlank()) {
            validationError = "Server pattern is required."; return;
        }
        if (selectedProfileId == null || selectedProfileId.isBlank()) {
            validationError = "Select a proxy profile for the rule."; return;
        }

        Set<String> takenIds = new HashSet<>();
        if (parent instanceof ProxyConfigScreen cfg) {
            takenIds.addAll(cfg.ruleIdsExcluding(editing.getId()));
        }

        editing.setId(ConfigIds.uniqueId(
                idField.getText().trim().isBlank() ? labelField.getText() : idField.getText().trim(),
                "rule", takenIds));
        editing.setLabel(labelField.getText().trim());
        editing.setServerPattern(pattern);
        editing.setPort(parsePort(portField.getText()));
        editing.setProxyProfileId(selectedProfileId);
        editing.setEnabled(enabled);
        validationError = "";
        onSave.accept(editing);
        this.client.setScreen(parent);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private String selectedProfileName(List<ProxyProfile> profiles) {
        for (ProxyProfile p : profiles) {
            if (p.getId().equals(selectedProfileId)) {
                return trim(p.getName().isBlank() ? p.getId() : p.getName(), 36) + "  ▾";
            }
        }
        return profiles.isEmpty() ? "—" : "Select proxy…";
    }

    private int parsePort(String value) {
        try { return (value == null || value.isBlank()) ? -1 : Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private Text enabledText() {
        return Text.literal(enabled ? "Enabled" : "Disabled");
    }

    private TextFieldWidget addField(int x, int y, int width, String value, String placeholder) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, width, FIELD_HEIGHT, Text.empty());
        f.setText(value == null ? "" : value);
        f.setMaxLength(256);
        addDrawableChild(f);
        return f;
    }

    private void drawFieldLabel(DrawContext ctx, String label, TextFieldWidget field) {
        ctx.drawTextWithShadow(textRenderer, label, field.getX(), field.getY() - 12, 0xFFFFFFFF);
    }

    private void drawPlaceholder(DrawContext ctx, TextFieldWidget field, String placeholder) {
        if (!field.getText().isEmpty() || this.getFocused() == field) return;
        ctx.drawTextWithShadow(textRenderer, placeholder, field.getX() + 4, field.getY() + 6, 0xFF808080);
    }

    private String trim(String v, int max) {
        if (v == null) return "";
        return v.length() <= max ? v : v.substring(0, max - 2) + "…";
    }

    @Override
    public void close() { this.client.setScreen(parent); }

    private static String resolveProfileId(List<ProxyProfile> profiles, String id) {
        for (ProxyProfile p : profiles) {
            if (p.getId().equals(id)) return id;
        }
        return profiles.isEmpty() ? "" : profiles.get(0).getId();
    }

    private static RoutingRule copyOf(RoutingRule src) {
        RoutingRule t = new RoutingRule();
        if (src == null) return t;
        t.setId(src.getId());
        t.setLabel(src.getLabel());
        t.setServerPattern(src.getServerPattern());
        t.setPort(src.getPort());
        t.setProxyProfileId(src.getProxyProfileId());
        t.setEnabled(src.isEnabled());
        return t;
    }
}
