package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ConfigIds;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;
import ru.dverkask.routeweaver.config.RoutingRule;

import java.util.List;
import java.util.function.Consumer;

/**
 * The form for creating or editing a routing rule, shared by the Servers and Rules tabs.
 */
final class RuleEditor {
    private final ScreenHost host;
    private final String ruleId;
    private final Consumer<String> onClose;
    private final ProfileDropdown profileDropdown;

    private String label;
    private String pattern;
    private String port;
    private String profileId;
    private @Nullable Text error;

    private RuleEditor(ScreenHost host, String ruleId, String label, String pattern, int port, String profileId,
                       Consumer<String> onClose) {
        this.host = host;
        this.ruleId = ruleId;
        this.label = label;
        this.pattern = pattern;
        this.port = port > 0 ? String.valueOf(port) : "";
        this.profileId = profileId;
        this.onClose = onClose;
        this.profileDropdown = new ProfileDropdown(host::refresh);
    }

    /**
     * @param onClose receives the id of the saved rule, or null if the edit was cancelled or the
     *                rule deleted
     */
    static RuleEditor edit(ScreenHost host, RoutingRule rule, Consumer<String> onClose) {
        return new RuleEditor(host, rule.getId(), rule.getLabel(), rule.getServerPattern(), rule.getPort(),
                rule.getProxyProfileId(), onClose);
    }

    static RuleEditor create(ScreenHost host, String label, String pattern, int port, String profileId,
                             Consumer<String> onClose) {
        return new RuleEditor(host, "", label, pattern, port, profileId, onClose);
    }

    private boolean isNew() {
        return ruleId.isEmpty();
    }

    FlowLayout build() {
        FlowLayout pane = Widgets.pane(Sizing.expand());
        pane.child(Widgets.heading(
                Text.translatable(isNew() ? "routeweaver.rule_editor.new" : "routeweaver.rule_editor.edit"),
                Text.translatable("routeweaver.rule_editor.subtitle")));

        FlowLayout settings = Widgets.card(Text.translatable("routeweaver.rule_editor.settings"));
        settings.child(Widgets.field(Text.translatable("routeweaver.field.rule_name"), label, value -> label = value));
        FlowLayout patternRow = Widgets.row();
        patternRow.gap(10);
        FlowLayout patternField = Widgets.field(Text.translatable("routeweaver.field.pattern"), pattern,
                value -> pattern = value);
        patternField.horizontalSizing(Sizing.expand());
        FlowLayout portField = Widgets.field(Text.translatable("routeweaver.field.port"), port, value -> port = value);
        portField.horizontalSizing(Sizing.fixed(90));
        settings.child(patternRow.child(patternField).child(portField));
        settings.child(Widgets.body(Text.translatable("routeweaver.rule_editor.examples")));
        pane.child(settings);

        FlowLayout profileCard = Widgets.card(Text.translatable("routeweaver.rule_editor.profile"));
        List<ProxyProfile> profiles = host.config().getProfiles();
        if (profiles.isEmpty()) {
            profileCard.child(Widgets.body(Text.translatable("routeweaver.rule_editor.no_profiles")));
        } else {
            profileCard.child(profileDropdown.build(profiles, profileId, id -> profileId = id));
        }
        pane.child(profileCard);

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

        if (!isNew()) {
            pane.child(Widgets.dangerButton(Text.translatable("routeweaver.rules.delete"), this::delete));
        }
        return pane;
    }

    private void save() {
        String trimmedPattern = pattern.trim();
        if (trimmedPattern.isEmpty()) {
            fail("routeweaver.error.pattern_required");
            return;
        }
        int parsedPort = RoutingRule.ANY_PORT;
        if (!port.isBlank()) {
            parsedPort = Widgets.parsePort(port);
            if (parsedPort < 0) {
                fail("routeweaver.error.rule_port_invalid");
                return;
            }
        }
        RouteWeaverConfig config = host.config();
        if (config.findProfile(profileId) == null) {
            fail("routeweaver.error.profile_required");
            return;
        }

        RoutingRule rule = isNew() ? null : config.findRule(ruleId);
        if (rule == null) {
            rule = new RoutingRule();
            String idSource = label.isBlank() ? trimmedPattern : label;
            rule.setId(ConfigIds.uniqueId(idSource, "route", config.ruleIds()));
            config.getRules().add(rule);
        }
        rule.setLabel(label.trim());
        rule.setServerPattern(trimmedPattern);
        rule.setPort(parsedPort);
        rule.setProxyProfileId(profileId);

        host.flash(Text.translatable("routeweaver.flash.rule_saved", trimmedPattern), false);
        onClose.accept(rule.getId());
        host.saveAndRefresh();
    }

    private void delete() {
        host.config().getRules().removeIf(rule -> rule.getId().equals(ruleId));
        host.flash(Text.translatable("routeweaver.flash.rule_deleted"), false);
        onClose.accept(null);
        host.saveAndRefresh();
    }

    private void fail(String translationKey) {
        error = Text.translatable(translationKey);
        host.refresh();
    }
}
