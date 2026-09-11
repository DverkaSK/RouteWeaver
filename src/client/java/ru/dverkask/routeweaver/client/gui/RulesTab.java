package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RoutingRule;

import java.util.List;

/**
 * Every routing rule, including the pattern rules that do not map to a saved server.
 */
final class RulesTab extends ConfigTab {
    private static final int PATTERN_LENGTH = 40;
    private static final int PROFILE_LENGTH = 36;

    private String selectedId = "";
    private @Nullable RuleEditor editor;

    RulesTab(ScreenHost host) {
        super(host);
    }

    @Override
    Component navLabel() {
        return Component.translatable("routeweaver.tab.rules");
    }

    @Override
    Component title() {
        return Component.translatable("routeweaver.tab.rules.title");
    }

    @Override
    Component subtitle() {
        return Component.translatable("routeweaver.tab.rules.subtitle");
    }

    @Override
    void onHidden() {
        editor = null;
    }

    @Override
    FlowLayout buildList() {
        List<RoutingRule> visible = host.config().getRules().stream()
                .filter(rule -> matchesQuery(rule.getLabel(), rule.getServerPattern(), profileName(rule)))
                .toList();
        if (visible.stream().noneMatch(rule -> rule.getId().equals(selectedId))) {
            selectedId = visible.isEmpty() ? "" : visible.getFirst().getId();
        }

        FlowLayout pane = Widgets.pane(LIST_WIDTH);
        pane.child(Widgets.heading(
                Component.translatable("routeweaver.rules.list.title"),
                Component.translatable("routeweaver.rules.list.subtitle")));

        FlowLayout list = Widgets.listContent();
        visible.forEach(rule -> list.child(ruleCard(rule)));
        if (visible.isEmpty()) {
            list.child(Widgets.emptyState(
                    Component.translatable("routeweaver.rules.empty.title"),
                    Component.translatable("routeweaver.rules.empty.body")));
        }
        return pane.child(host.scrollMemory().verticalList("rules", list));
    }

    private FlowLayout ruleCard(RoutingRule rule) {
        boolean selected = rule.getId().equals(selectedId);
        FlowLayout card = Widgets.selectableCard(selected, () -> {
            selectedId = rule.getId();
            editor = null;
            host.refresh();
        });
        card.padding(Insets.of(8));
        card.gap(4);
        card.child(Widgets.label(Widgets.ellipsize(target(rule), PATTERN_LENGTH),
                selected ? Theme.ACCENT : rule.isEnabled() ? Theme.TEXT : Theme.MUTED));
        String profileName = profileName(rule);
        card.child(profileName.isEmpty()
                ? Widgets.label(Component.translatable("routeweaver.rules.no_proxy"), Theme.ERROR)
                : Widgets.label(Widgets.ellipsize(profileName, PROFILE_LENGTH), Theme.MUTED));
        card.child(Widgets.enabledToggle(rule.isEnabled(), () -> {
            rule.setEnabled(!rule.isEnabled());
            host.saveAndRefresh();
        }));
        return card;
    }

    @Override
    FlowLayout buildDetail() {
        if (editor != null) {
            return editor.build();
        }

        FlowLayout pane = Widgets.pane(Sizing.expand());
        RoutingRule rule = host.config().findRule(selectedId);
        if (rule == null) {
            return pane
                    .child(Widgets.heading(
                            Component.translatable("routeweaver.rules.detail.title"),
                            Component.translatable("routeweaver.rules.none.subtitle")))
                    .child(Widgets.emptyState(
                            Component.translatable("routeweaver.rules.none.title"),
                            Component.translatable("routeweaver.rules.none.body")));
        }

        String profileName = profileName(rule);
        pane.child(Widgets.heading(Component.translatable("routeweaver.rules.detail.title"), Component.literal(target(rule))));
        pane.child(Widgets.card(Component.translatable("routeweaver.rules.settings"))
                .child(Widgets.body(Component.translatable("routeweaver.rules.pattern", rule.getServerPattern())))
                .child(Widgets.body(rule.hasPortFilter()
                        ? Component.translatable("routeweaver.rules.port", rule.getPort())
                        : Component.translatable("routeweaver.rules.port", Component.translatable("routeweaver.rules.port_any"))))
                .child(Widgets.body(Component.translatable("routeweaver.rules.proxy",
                        profileName.isEmpty() ? "—" : profileName)))
                .child(rule.isEnabled()
                        ? Widgets.label(Component.translatable("routeweaver.state.enabled"), Theme.SUCCESS)
                        : Widgets.label(Component.translatable("routeweaver.state.disabled"), Theme.MUTED)));

        pane.child(Widgets.wideButton(Component.translatable("routeweaver.rules.edit"), () -> {
            editor = RuleEditor.edit(host, rule, this::onEditorClosed);
            host.refresh();
        }));
        pane.child(Widgets.dangerButton(Component.translatable("routeweaver.rules.delete"), () -> {
            host.config().getRules().remove(rule);
            selectedId = "";
            host.flash(Component.translatable("routeweaver.flash.rule_deleted"), false);
            host.saveAndRefresh();
        }));
        return pane;
    }

    private void onEditorClosed(@Nullable String savedId) {
        editor = null;
        if (savedId != null) {
            selectedId = savedId;
        }
    }

    private static String target(RoutingRule rule) {
        return rule.hasPortFilter() ? rule.getServerPattern() + ":" + rule.getPort() : rule.getServerPattern();
    }

    private String profileName(RoutingRule rule) {
        ProxyProfile profile = host.config().findProfile(rule.getProxyProfileId());
        return profile == null ? "" : profile.displayName();
    }
}
