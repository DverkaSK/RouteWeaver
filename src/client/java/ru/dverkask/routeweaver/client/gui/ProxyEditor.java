package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ConfigIds;
import ru.dverkask.routeweaver.config.InvalidShadowsocksUriException;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;
import ru.dverkask.routeweaver.config.ShadowsocksUri;
import ru.dverkask.routeweaver.proxy.CipherMethod;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The form for creating or editing a Shadowsocks profile, with {@code ss://} link import.
 */
final class ProxyEditor {
    private static final int LINK_MAX_LENGTH = 4096;

    private final ScreenHost host;
    private final String profileId;
    private final Consumer<String> onClose;

    private String link = "";
    private String name;
    private String address;
    private String port;
    private CipherMethod method;
    private String password;
    private boolean enabled;
    private @Nullable Text error;

    private ProxyEditor(ScreenHost host, String profileId, Consumer<String> onClose) {
        this.host = host;
        this.profileId = profileId;
        this.onClose = onClose;
    }

    /**
     * @param onClose receives the id of the saved profile, or null if the edit was cancelled
     */
    static ProxyEditor create(ScreenHost host, Consumer<String> onClose) {
        ProxyEditor editor = new ProxyEditor(host, "", onClose);
        editor.name = "";
        editor.address = "";
        editor.port = "";
        editor.method = CipherMethod.DEFAULT;
        editor.password = "";
        editor.enabled = true;
        return editor;
    }

    static ProxyEditor edit(ScreenHost host, ProxyProfile profile, Consumer<String> onClose) {
        ProxyEditor editor = new ProxyEditor(host, profile.getId(), onClose);
        editor.name = profile.getName();
        editor.address = profile.getHost();
        editor.port = profile.getPort() > 0 ? String.valueOf(profile.getPort()) : "";
        CipherMethod method = CipherMethod.fromId(profile.getMethod());
        editor.method = method != null ? method : CipherMethod.DEFAULT;
        editor.password = profile.getPassword();
        editor.enabled = profile.isEnabled();
        return editor;
    }

    private boolean isNew() {
        return profileId.isEmpty();
    }

    FlowLayout build() {
        FlowLayout pane = Widgets.pane(Sizing.expand());
        pane.child(Widgets.heading(
                Text.translatable(isNew() ? "routeweaver.proxy_editor.new" : "routeweaver.proxy_editor.edit"),
                Text.translatable("routeweaver.proxy_editor.subtitle")));

        FlowLayout importCard = Widgets.card(Text.translatable("routeweaver.proxy_editor.import"));
        FlowLayout importRow = Widgets.row();
        FlowLayout linkInput = Widgets.input(Widgets.textBox(link, LINK_MAX_LENGTH, value -> link = value));
        linkInput.horizontalSizing(Sizing.expand());
        ButtonComponent importButton = Widgets.button(
                Text.translatable("routeweaver.proxy_editor.import_button"), this::importLink);
        importButton.horizontalSizing(Sizing.fixed(72));
        importCard.child(importRow.child(linkInput).child(importButton));
        pane.child(importCard);

        FlowLayout settings = Widgets.card(Text.translatable("routeweaver.proxy_editor.settings"));
        settings.child(Widgets.field(Text.translatable("routeweaver.field.name"), name, value -> name = value));

        FlowLayout endpointRow = Widgets.row();
        endpointRow.gap(10);
        FlowLayout hostField = Widgets.field(Text.translatable("routeweaver.field.host"), address,
                value -> address = value);
        hostField.horizontalSizing(Sizing.expand());
        FlowLayout portField = Widgets.field(Text.translatable("routeweaver.field.port"), port, value -> port = value);
        portField.horizontalSizing(Sizing.fixed(90));
        settings.child(endpointRow.child(hostField).child(portField));

        FlowLayout credentialsRow = Widgets.row();
        credentialsRow.gap(10);
        FlowLayout cipherField = Widgets.buttonField(Text.translatable("routeweaver.field.cipher"),
                Widgets.button(Text.literal(method.getId()), this::cycleMethod));
        cipherField.horizontalSizing(Sizing.expand());
        FlowLayout passwordField = Widgets.field(Text.translatable("routeweaver.field.password"), password,
                value -> password = value);
        passwordField.horizontalSizing(Sizing.expand());
        settings.child(credentialsRow.child(cipherField).child(passwordField));
        pane.child(settings);

        pane.child(Widgets.card(Text.translatable("routeweaver.field.status"))
                .child(Widgets.enabledToggle(enabled, () -> {
                    enabled = !enabled;
                    host.refresh();
                })));

        if (error != null) {
            pane.child(Widgets.label(error, Theme.ERROR));
        }

        FlowLayout actions = Widgets.row();
        actions.child(Widgets.flexButton(Text.translatable("routeweaver.action.save"), this::save));
        actions.child(Widgets.flexButton(Text.translatable("routeweaver.action.cancel"), () -> {
            onClose.accept(null);
            host.refresh();
        }));
        pane.child(actions);
        return pane;
    }

    private void cycleMethod() {
        CipherMethod[] methods = CipherMethod.values();
        method = methods[(method.ordinal() + 1) % methods.length];
        host.refresh();
    }

    private void importLink() {
        try {
            ShadowsocksUri uri = ShadowsocksUri.parse(link);
            CipherMethod importedMethod = CipherMethod.fromId(uri.method());
            if (importedMethod == null) {
                error = Text.translatable("routeweaver.error.ss_uri.unsupported_cipher", uri.method());
            } else {
                if (name.isBlank()) {
                    name = uri.name().isBlank()
                            ? Text.translatable("routeweaver.proxy_editor.imported_name").getString()
                            : uri.name();
                }
                address = uri.host();
                port = String.valueOf(uri.port());
                method = importedMethod;
                password = uri.password();
                error = null;
            }
        } catch (InvalidShadowsocksUriException exception) {
            error = Text.translatable("routeweaver.error.ss_uri." + exception.reason().name().toLowerCase(Locale.ROOT));
        }
        host.refresh();
    }

    private void save() {
        ProxyProfile form = new ProxyProfile();
        form.setName(name.trim());
        form.setHost(address.trim());
        form.setPort(Widgets.parsePort(port));
        form.setMethod(method.getId());
        form.setPassword(password);
        form.setEnabled(enabled);

        if (form.getHost().isEmpty()) {
            fail(Text.translatable("routeweaver.error.host_required"));
            return;
        }
        if (form.getPort() < 0) {
            fail(Text.translatable("routeweaver.error.port_invalid"));
            return;
        }
        if (form.getPassword().isEmpty()) {
            fail(Text.translatable("routeweaver.error.password_required"));
            return;
        }

        RouteWeaverConfig config = host.config();
        ProxyProfile target = isNew() ? null : config.findProfile(profileId);
        if (target == null) {
            for (ProxyProfile existing : config.getProfiles()) {
                if (existing.hasSameEndpoint(form)) {
                    RouteWeaverToast.show(
                            Text.translatable("routeweaver.toast.duplicate.title"),
                            Text.translatable("routeweaver.toast.duplicate.body", existing.displayName()));
                    return;
                }
            }
            String idSource = form.getName().isEmpty() ? form.getHost() : form.getName();
            form.setId(ConfigIds.uniqueId(idSource, "profile", config.profileIds()));
            config.getProfiles().add(form);
            target = form;
            host.flash(Text.translatable("routeweaver.flash.proxy_added", target.displayName()), false);
        } else {
            // The id stays fixed so the rules pointing at this profile keep working after a rename.
            target.setName(form.getName());
            target.setHost(form.getHost());
            target.setPort(form.getPort());
            target.setMethod(form.getMethod());
            target.setPassword(form.getPassword());
            target.setEnabled(form.isEnabled());
            host.flash(Text.translatable("routeweaver.flash.proxy_updated", target.displayName()), false);
        }

        onClose.accept(target.getId());
        host.saveAndRefresh();
    }

    private void fail(Text message) {
        error = message;
        host.refresh();
    }
}
