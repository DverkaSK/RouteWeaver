package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.dverkask.routeweaver.client.config.ProxyMode;
import ru.dverkask.routeweaver.client.config.ProxyProfile;
import ru.dverkask.routeweaver.client.config.ConfigIds;
import ru.dverkask.routeweaver.client.config.ShadowsocksUriParser;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ProxyProfileEditScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int IMPORT_ROW_Y = 50;
    private static final int IMPORT_FIELD_WIDTH = 290;
    private static final int IMPORT_BUTTON_WIDTH = 100;

    private final Screen parent;
    private final Consumer<ProxyProfile> onSave;
    private final ProxyProfile editing;

    private TextFieldWidget idField;
    private TextFieldWidget nameField;
    private TextFieldWidget importField;
    private TextFieldWidget hostField;
    private TextFieldWidget portField;
    private TextFieldWidget methodField;
    private TextFieldWidget passwordField;
    private TextFieldWidget uuidField;
    private TextFieldWidget sniField;
    private ButtonWidget modeButton;
    private ButtonWidget enabledButton;
    private String validationError = "";

    private ProxyMode mode;
    private boolean enabled;

    public ProxyProfileEditScreen(Screen parent, ProxyProfile original, Consumer<ProxyProfile> onSave) {
        super(Text.literal(original == null ? "Add profile" : "Edit profile"));
        this.parent = parent;
        this.onSave = onSave;
        this.editing = copyOf(original);
        this.mode = editing.getMode();
        this.enabled = editing.isEnabled();
    }

    @Override
    protected void init() {
        int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        int left = panelLeft + 14;
        int right = panelLeft + 220;
        int y = 96;
        int fieldWidth = 186;

        this.importField = addField(left, IMPORT_ROW_Y, IMPORT_FIELD_WIDTH, "", "ss://your-link-here");
        addDrawableChild(ButtonWidget.builder(Text.literal("Import SS"), button -> importShadowsocksLink())
                .dimensions(left + IMPORT_FIELD_WIDTH + 10, IMPORT_ROW_Y, IMPORT_BUTTON_WIDTH, 20)
                .build());

        this.idField = addField(left, y, fieldWidth, editing.getId(), "profile-id");
        this.nameField = addField(right, y, fieldWidth, editing.getName(), "Netherlands SS 1");
        y += 52;
        this.hostField = addField(left, y, fieldWidth, editing.getHost(), "203.0.113.10");
        this.portField = addField(right, y, fieldWidth, editing.getPort() > 0 ? String.valueOf(editing.getPort()) : "", "8388");
        y += 52;
        this.methodField = addField(left, y, fieldWidth, editing.getMethod(), "chacha20-ietf-poly1305");
        this.passwordField = addField(right, y, fieldWidth, editing.getPassword(), "password-from-ss-link");
        y += 52;
        this.uuidField = addField(left, y, fieldWidth, editing.getUuid(), "leave empty for Shadowsocks");
        this.sniField = addField(right, y, fieldWidth, editing.getServerName(), "leave empty for Shadowsocks");
        y += 52;

        this.modeButton = addDrawableChild(ButtonWidget.builder(modeText(), button -> {
        }).dimensions(left, y, fieldWidth, 20).build());
        this.modeButton.active = false;

        this.enabledButton = addDrawableChild(ButtonWidget.builder(enabledText(), button -> {
            this.enabled = !this.enabled;
            button.setMessage(enabledText());
        }).dimensions(right, y, fieldWidth, 20).build());

        int footerY = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(this.width / 2 - 75, footerY, 70, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(this.width / 2 + 5, footerY, 70, 20)
                .build());
    }

    private TextFieldWidget addField(int x, int y, int width, String value, String placeholder) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, width, 20, Text.empty());
        field.setText(value == null ? "" : value);
        field.setMaxLength(256);
        addDrawableChild(field);
        return field;
    }

    private void save() {
        String host = hostField.getText().trim();
        int port = parsePort(portField.getText());
        String method = methodField.getText().trim();
        String password = passwordField.getText().trim();
        Set<String> takenIds = new HashSet<>();
        if (parent instanceof ProxyConfigScreen configScreen) {
            takenIds.addAll(configScreen.profileIdsExcluding(editing.getId()));
        }

        if (host.isBlank()) {
            validationError = "Host is required.";
            return;
        }
        if (port <= 0) {
            validationError = "Port must be a positive number.";
            return;
        }
        if (method.isBlank()) {
            validationError = "Method is required for Shadowsocks.";
            return;
        }
        if (password.isBlank()) {
            validationError = "Password is required for Shadowsocks.";
            return;
        }

        editing.setId(ConfigIds.uniqueId(idField.getText().trim().isBlank() ? nameField.getText() : idField.getText().trim(), "profile", takenIds));
        editing.setName(nameField.getText().trim());
        editing.setHost(host);
        editing.setPort(port);
        editing.setMethod(method);
        editing.setPassword(password);
        editing.setUuid(uuidField.getText().trim());
        editing.setServerName(sniField.getText().trim());
        editing.setMode(ProxyMode.SHADOWSOCKS);
        editing.setEnabled(enabled);
        validationError = "";
        onSave.accept(editing);
        this.client.setScreen(parent);
    }

    private void importShadowsocksLink() {
        try {
            ShadowsocksUriParser.ParsedShadowsocksUri parsed = ShadowsocksUriParser.parse(importField.getText().trim());
            if (!parsed.name().isBlank() && nameField.getText().isBlank()) {
                nameField.setText(parsed.name());
            } else if (nameField.getText().isBlank()) {
                nameField.setText("Imported SS");
            }
            hostField.setText(parsed.host());
            portField.setText(String.valueOf(parsed.port()));
            methodField.setText(parsed.method());
            passwordField.setText(parsed.password());
            validationError = "";
        } catch (IllegalArgumentException exception) {
            validationError = exception.getMessage();
        }
    }

    private int parsePort(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Text modeText() {
        return Text.literal("Mode: " + mode.name());
    }

    private Text enabledText() {
        return Text.literal(enabled ? "Enabled" : "Disabled");
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        int panelRight = this.width / 2 + PANEL_WIDTH / 2;
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        context.fill(panelLeft - 8, 30, panelRight + 8, this.height - 36, 0x70181818);
        context.fill(panelLeft - 4, 34, panelRight + 4, 82, 0x60303030);
        context.fill(panelLeft - 4, 86, panelRight + 4, this.height - 70, 0x40242424);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "1. Paste your full ss:// link here, then press Import SS", panelLeft, 24, 0xFFFFFFFF);
        drawFieldLabel(context, "Profile ID", this.idField);
        drawFieldLabel(context, "Profile Name", this.nameField);
        drawFieldLabel(context, "Proxy Host", this.hostField);
        drawFieldLabel(context, "Proxy Port", this.portField);
        drawFieldLabel(context, "Encryption Method", this.methodField);
        drawFieldLabel(context, "Password", this.passwordField);
        drawFieldLabel(context, "UUID", this.uuidField);
        drawFieldLabel(context, "Server Name / SNI", this.sniField);
        drawPlaceholder(context, importField, "ss://your-link-here");
        drawPlaceholder(context, idField, "profile-id");
        drawPlaceholder(context, nameField, "Netherlands SS 1");
        drawPlaceholder(context, hostField, "203.0.113.10");
        drawPlaceholder(context, portField, "8388");
        drawPlaceholder(context, methodField, "chacha20-ietf-poly1305");
        drawPlaceholder(context, passwordField, "password-from-ss-link");
        drawPlaceholder(context, uuidField, "leave empty for Shadowsocks");
        drawPlaceholder(context, sniField, "leave empty for Shadowsocks");
        context.drawTextWithShadow(this.textRenderer, "Only Shadowsocks is enabled in the current build.", panelLeft, this.height - 46, 0xFFA0A0A0);
        if (!validationError.isBlank()) {
            context.drawTextWithShadow(this.textRenderer, validationError, panelLeft, this.height - 60, 0xFFFF8080);
        }
    }

    private void drawFieldLabel(DrawContext context, String label, TextFieldWidget field) {
        context.drawTextWithShadow(this.textRenderer, label, field.getX(), field.getY() - 12, 0xFFFFFFFF);
    }

    private void drawPlaceholder(DrawContext context, TextFieldWidget field, String placeholder) {
        if (!field.getText().isEmpty() || this.getFocused() == field) {
            return;
        }
        context.drawTextWithShadow(this.textRenderer, placeholder, field.getX() + 4, field.getY() + 6, 0xFF808080);
    }

    private static ProxyProfile copyOf(ProxyProfile source) {
        ProxyProfile target = new ProxyProfile();
        if (source == null) {
            return target;
        }
        target.setId(source.getId());
        target.setName(source.getName());
        target.setMode(source.getMode());
        target.setHost(source.getHost());
        target.setPort(source.getPort());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setMethod(source.getMethod());
        target.setVlessFlow(source.getVlessFlow());
        target.setUuid(source.getUuid());
        target.setServerName(source.getServerName());
        target.setEnabled(source.isEnabled());
        return target;
    }
}
