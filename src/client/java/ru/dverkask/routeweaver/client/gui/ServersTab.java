package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ConfigIds;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;
import ru.dverkask.routeweaver.config.RoutingRule;
import ru.dverkask.routeweaver.routing.RouteDecision;
import ru.dverkask.routeweaver.routing.Router;

import java.util.List;

/**
 * The player's saved multiplayer servers, each routable through a proxy in one click.
 */
final class ServersTab extends ConfigTab {
    private static final int NAME_LENGTH = 32;
    private static final int MOTD_LENGTH = 44;
    private static final int ADDRESS_LENGTH = 26;
    private static final int FAVICON_SIZE = 32;

    private final ProfileDropdown profileDropdown;
    private String selectedAddress = "";
    private String profileChoiceId = "";
    private @Nullable RuleEditor ruleEditor;

    ServersTab(ScreenHost host) {
        super(host);
        this.profileDropdown = new ProfileDropdown(host::refresh);
    }

    @Override
    Component navLabel() {
        return Component.translatable("routeweaver.tab.servers");
    }

    @Override
    Component title() {
        return Component.translatable("routeweaver.tab.servers.title");
    }

    @Override
    Component subtitle() {
        return Component.translatable("routeweaver.tab.servers.subtitle");
    }

    @Override
    void onHidden() {
        ruleEditor = null;
        profileDropdown.collapse();
    }

    // ─── List ───────────────────────────────────────────────────────────────

    @Override
    FlowLayout buildList() {
        List<SavedServer> visible = host.savedServers().stream()
                .filter(server -> matchesQuery(server.name(), server.rawAddress()))
                .toList();
        keepSelectionVisible(visible);

        FlowLayout pane = Widgets.pane(LIST_WIDTH);
        pane.child(Widgets.heading(
                Component.translatable("routeweaver.servers.list.title"),
                Component.translatable("routeweaver.servers.list.subtitle")));

        FlowLayout list = Widgets.listContent();
        visible.forEach(server -> list.child(serverCard(server)));
        if (visible.isEmpty()) {
            list.child(Widgets.emptyState(
                    Component.translatable("routeweaver.servers.empty.title"),
                    Component.translatable("routeweaver.servers.empty.body")));
        }
        pane.child(host.scrollMemory().verticalList("servers", list));
        return pane;
    }

    private void keepSelectionVisible(List<SavedServer> visible) {
        boolean selectionVisible = visible.stream().anyMatch(server -> server.rawAddress().equals(selectedAddress));
        if (!selectionVisible) {
            selectedAddress = visible.isEmpty() ? "" : visible.getFirst().rawAddress();
            profileChoiceId = visible.isEmpty() ? "" : preferredProfileId(visible.getFirst());
        } else if (host.config().findProfile(profileChoiceId) == null) {
            SavedServer selected = selectedServer();
            profileChoiceId = selected == null ? "" : preferredProfileId(selected);
        }
    }

    private FlowLayout serverCard(SavedServer server) {
        boolean selected = server.rawAddress().equals(selectedAddress);
        FlowLayout card = Widgets.selectableCard(selected, () -> select(server));
        card.padding(Insets.of(6));

        FlowLayout row = Widgets.row();
        row.verticalAlignment(VerticalAlignment.TOP);
        Identifier favicon = ServerFavicons.textureOf(server.info());
        if (favicon != null) {
            row.child(UIComponents.texture(favicon, 0, 0, 64, 64, 64, 64)
                    .blend(true)
                    .horizontalSizing(Sizing.fixed(FAVICON_SIZE))
                    .verticalSizing(Sizing.fixed(FAVICON_SIZE)));
        }

        FlowLayout info = UIContainers.verticalFlow(Sizing.expand(), Sizing.content());
        info.gap(2);
        info.child(Widgets.label(Widgets.ellipsize(server.name(), NAME_LENGTH), selected ? Theme.ACCENT : Theme.TEXT));
        String motd = firstMotdLine(server.info());
        if (!motd.isEmpty()) {
            info.child(Widgets.label(Widgets.ellipsize(motd, MOTD_LENGTH), Theme.MUTED));
        }
        info.child(metaRow(server));
        info.child(Widgets.label(routeSummary(Router.resolve(host.config(), server.address())),
                selected ? Theme.ACCENT : Theme.MUTED));

        card.child(row.child(info));
        return card;
    }

    private static FlowLayout metaRow(SavedServer server) {
        ServerData info = server.info();
        FlowLayout meta = Widgets.row();
        meta.child(Widgets.label(Widgets.ellipsize(server.rawAddress(), ADDRESS_LENGTH), Theme.MUTED));
        if (info.ping > 0) {
            Color pingColor = info.ping < 100 ? Theme.SUCCESS : info.ping < 300 ? Theme.ACCENT : Theme.ERROR;
            meta.child(Widgets.label(Component.translatable("routeweaver.ping", info.ping), pingColor));
        }
        if (info.status != null && !info.status.getString().isBlank()) {
            meta.child(UIComponents.label(info.status).shadow(false));
        }
        return meta;
    }

    private static String firstMotdLine(ServerData info) {
        if (info.motd == null) {
            return "";
        }
        String motd = info.motd.getString().trim();
        int newline = motd.indexOf('\n');
        return (newline >= 0 ? motd.substring(0, newline) : motd).trim();
    }

    private void select(SavedServer server) {
        selectedAddress = server.rawAddress();
        profileChoiceId = preferredProfileId(server);
        profileDropdown.collapse();
        ruleEditor = null;
        host.refresh();
    }

    // ─── Detail ─────────────────────────────────────────────────────────────

    @Override
    FlowLayout buildDetail() {
        if (ruleEditor != null) {
            return ruleEditor.build();
        }

        FlowLayout pane = Widgets.pane(Sizing.expand());
        pane.child(Widgets.heading(
                Component.translatable("routeweaver.servers.detail.title"),
                Component.translatable("routeweaver.servers.detail.subtitle")));

        SavedServer server = selectedServer();
        if (server == null) {
            return pane.child(Widgets.emptyState(
                    Component.translatable("routeweaver.servers.none.title"),
                    Component.translatable("routeweaver.servers.none.body")));
        }

        RouteWeaverConfig config = host.config();
        RoutingRule exactRule = Router.findExactRule(config, server.address());
        RouteDecision decision = Router.resolve(config, server.address());

        pane.child(Widgets.card(Component.translatable("routeweaver.servers.target"))
                .child(Widgets.title(Component.literal(server.name())))
                .child(Widgets.body(Component.literal(server.rawAddress())))
                .child(routeStatus(decision)));
        pane.child(quickRouteCard(server, exactRule));

        FlowLayout help = Widgets.card(Component.translatable("routeweaver.servers.help.title"));
        help.child(Widgets.body(Component.translatable("routeweaver.servers.help.save")));
        help.child(Widgets.body(Component.translatable("routeweaver.servers.help.exact", server.address().toString())));
        help.child(Widgets.body(Component.translatable(exactRule != null
                ? "routeweaver.servers.help.has_route"
                : "routeweaver.servers.help.no_route")));
        pane.child(help);
        return pane;
    }

    private FlowLayout quickRouteCard(SavedServer server, @Nullable RoutingRule exactRule) {
        FlowLayout card = Widgets.card(Component.translatable("routeweaver.servers.quick_route"));
        List<ProxyProfile> profiles = host.config().getProfiles();
        if (profiles.isEmpty()) {
            card.child(Widgets.body(Component.translatable("routeweaver.servers.no_profiles")));
        } else {
            card.child(profileDropdown.build(profiles, profileChoiceId, id -> profileChoiceId = id));
        }

        ButtonComponent save = Widgets.wideButton(
                Component.translatable("routeweaver.servers.save_route"), () -> saveRoute(server));
        save.active(host.config().findProfile(profileChoiceId) != null);
        card.child(save);

        if (exactRule != null) {
            card.child(Widgets.toggle(exactRule.isEnabled(),
                    Component.translatable("routeweaver.servers.route_enabled"),
                    Component.translatable("routeweaver.servers.route_disabled"),
                    () -> {
                        exactRule.setEnabled(!exactRule.isEnabled());
                        host.saveAndRefresh();
                    }));
        }

        card.child(Widgets.wideButton(Component.translatable("routeweaver.servers.edit_rule"),
                () -> openRuleEditor(server, exactRule)));
        return card;
    }

    private static LabelComponent routeStatus(RouteDecision decision) {
        return switch (decision.kind()) {
            case PROXY -> Widgets.label(
                    Component.translatable("routeweaver.route.proxy", decision.profile().displayName()), Theme.SUCCESS);
            case PROXY_DISABLED -> Widgets.label(
                    Component.translatable("routeweaver.route.proxy_disabled", decision.profile().displayName()), Theme.ACCENT);
            case DIRECT -> Widgets.label(Component.translatable("routeweaver.route.direct_long"), Theme.MUTED);
        };
    }

    private static Component routeSummary(RouteDecision decision) {
        return switch (decision.kind()) {
            case PROXY -> Component.translatable("routeweaver.route.proxy", decision.profile().displayName());
            case PROXY_DISABLED -> Component.translatable("routeweaver.route.proxy_disabled_short");
            case DIRECT -> Component.translatable("routeweaver.route.direct");
        };
    }

    // ─── Actions ────────────────────────────────────────────────────────────

    /**
     * Creates or updates the one rule that matches exactly this server.
     */
    private void saveRoute(SavedServer server) {
        RouteWeaverConfig config = host.config();
        ProxyProfile profile = config.findProfile(profileChoiceId);
        if (profile == null) {
            host.flash(Component.translatable("routeweaver.error.profile_required"), true);
            host.refresh();
            return;
        }

        RoutingRule rule = Router.findExactRule(config, server.address());
        if (rule == null) {
            rule = new RoutingRule();
            rule.setId(ConfigIds.uniqueId(server.name(), "route", config.ruleIds()));
            config.getRules().add(rule);
        }
        rule.setLabel(server.name());
        rule.setServerPattern(server.address().host());
        rule.setPort(server.address().port());
        rule.setProxyProfileId(profile.getId());
        rule.setEnabled(true);

        host.flash(Component.translatable("routeweaver.flash.route_saved", server.name(), profile.displayName()), false);
        host.saveAndRefresh();
    }

    private void openRuleEditor(SavedServer server, @Nullable RoutingRule exactRule) {
        profileDropdown.collapse();
        ruleEditor = exactRule != null
                ? RuleEditor.edit(host, exactRule, savedId -> ruleEditor = null)
                : RuleEditor.create(host, server.name(), server.address().host(), server.address().port(),
                        profileChoiceId, savedId -> ruleEditor = null);
        host.refresh();
    }

    private @Nullable SavedServer selectedServer() {
        for (SavedServer server : host.savedServers()) {
            if (server.rawAddress().equals(selectedAddress)) {
                return server;
            }
        }
        return null;
    }

    /**
     * The profile of the server's existing route, else the current choice, else the first profile.
     */
    private String preferredProfileId(SavedServer server) {
        RouteWeaverConfig config = host.config();
        RoutingRule rule = Router.findExactRule(config, server.address());
        if (rule != null && config.findProfile(rule.getProxyProfileId()) != null) {
            return rule.getProxyProfileId();
        }
        if (config.findProfile(profileChoiceId) != null) {
            return profileChoiceId;
        }
        return config.getProfiles().isEmpty() ? "" : config.getProfiles().getFirst().getId();
    }
}
