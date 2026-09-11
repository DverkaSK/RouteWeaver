package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.WorldIcon;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import ru.dverkask.routeweaver.RouteWeaver;
import ru.dverkask.routeweaver.client.proxy.EngineSnapshot;
import ru.dverkask.routeweaver.client.config.ConfigIds;
import ru.dverkask.routeweaver.client.config.ProxyMode;
import ru.dverkask.routeweaver.client.config.ProxyProfile;
import ru.dverkask.routeweaver.client.config.RoutingRule;
import ru.dverkask.routeweaver.client.config.ShadowsocksUriParser;
import ru.dverkask.routeweaver.client.routing.ProxyRouter;
import ru.dverkask.routeweaver.client.routing.RoutingDecision;
import ru.dverkask.routeweaver.client.routing.ServerPatternMatcher;
import ru.dverkask.routeweaver.client.runtime.ProxyRuntime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProxyConfigScreen extends BaseOwoScreen<FlowLayout> {
    private static final Color ACCENT      = Color.ofArgb(0xFFC9F24A);
    private static final Color ACCENT_SOFT = Color.ofArgb(0xFF9BBE3D);
    private static final Color TEXT        = Color.ofArgb(0xFFF5F5F2);
    private static final Color MUTED       = Color.ofArgb(0xFF999B99);
    private static final Color SUCCESS     = Color.ofArgb(0xFF7BDF91);
    private static final Color ERROR       = Color.ofArgb(0xFFFF8E8E);
    private static final int   PANEL       = 0xE6111214;
    private static final int   PANEL_ALT   = 0xCC17191D;
    private static final int   CARD        = 0xB816181C;
    private static final int   CARD_HOVER  = 0xD7202328;
    private static final int   CARD_ACTIVE = 0xFF272B31;
    private static final int   CARD_DISABLED = 0x88202024;
    private static final int   OUTLINE     = 0xFF2A2F31;

    // ─── runtime / data ───────────────────────────────────────────────────────
    private final Screen parent;
    private final ProxyRuntime runtime;
    private final ProxyRouter router = new ProxyRouter();
    private final List<ServerInfo> savedServers = new ArrayList<>();

    // ─── navigation & selection ───────────────────────────────────────────────
    private Tab currentTab = Tab.SERVERS;
    private EngineSnapshot engineSnap;
    private String selectedServerAddress      = "";
    private String selectedProfileId          = "";
    private String selectedRoutedServerAddress = "";
    private String selectedRuleId             = "";
    private String serverProfileChoiceId      = "";
    private String flashMessage               = "";
    private Color  flashColor                 = SUCCESS;
    private TextBoxComponent searchBox;

    // ─── inline proxy edit state ──────────────────────────────────────────────
    private boolean isEditingProxy   = false;
    private String  editingProfileId = "";   // empty = new profile
    private String  editImportInput  = "";
    private String  editNameInput    = "";
    private String  editHostInput    = "";
    private String  editPortInput    = "";
    private String  editMethodInput  = "";
    private String  editPasswordInput = "";
    private String  editUuidInput    = "";
    private String  editSniInput     = "";
    private boolean editEnabledInput = true;
    private String  editValidationError = "";

    // ─── dropdown state ───────────────────────────────────────────────────────
    private boolean proxyPickerExpanded     = false;
    private boolean ruleProxyPickerExpanded = false;

    // ─── inline rule edit state ───────────────────────────────────────────────
    private boolean isEditingRule        = false;
    private String  editingRuleId        = "";   // empty = new rule
    private String  editRuleIdInput      = "";
    private String  editRuleLabelInput   = "";
    private String  editRulePatternInput = "";
    private String  editRulePortInput    = "";
    private String  editRuleProfileId    = "";
    private String  editRuleValidationError = "";

    // ─── scroll position preservation across rebuilds ─────────────────────────
    private ScrollContainer<FlowLayout> serversListScroll;
    private ScrollContainer<FlowLayout> proxiesListScroll;
    private ScrollContainer<FlowLayout> rulesListScroll;

    // ─── favicon cache (persists within the game session) ─────────────────────
    private static final Map<String, WorldIcon>  FAVICON_ICONS = new HashMap<>();
    private static final Map<String, Identifier> FAVICON_CACHE = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    public ProxyConfigScreen(Screen parent, ProxyRuntime runtime) {
        super(Text.literal("Proxy routing"));
        this.parent  = parent;
        this.runtime = runtime;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        this.engineSnap = runtime.getEngineSnapshot();
        loadSavedServers();
        ensureSelections();

        root.clearChildren();
        root.surface(Surface.blur(2.5f, 4f).and(Surface.flat(0xA008090B)));
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.CENTER);

        // Dynamic padding to center the UI and limit its maximum size
        int padW = Math.max(16, (this.width - 1060) / 2);
        int padH = Math.max(16, (this.height - 640) / 2);
        root.padding(Insets.of(padH, padH, padW, padW));

        root.child(buildShell());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Shell
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout buildShell() {
        FlowLayout shell = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        shell.surface(Surface.flat(PANEL).and(Surface.outline(OUTLINE)));
        shell.padding(Insets.of(14));
        shell.gap(14);
        shell.child(buildSidebar());
        shell.child(buildWorkspace());
        return shell;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Sidebar
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout buildSidebar() {
        FlowLayout sidebar = UIContainers.verticalFlow(Sizing.fixed(176), Sizing.fill(100));
        sidebar.surface(Surface.flat(PANEL_ALT).and(Surface.outline(OUTLINE)));
        sidebar.padding(Insets.of(12));
        sidebar.gap(10);

        // Simple non-interactive header — no card, no button styling
        FlowLayout brand = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        brand.padding(Insets.of(2, 0, 6, 0));
        brand.gap(3);
        brand.child(titleLabel("Proxy"));
        brand.child(bodyLabel("Route servers through Shadowsocks"));
        sidebar.child(brand);

        sidebar.child(navEntry("Servers",  Tab.SERVERS));
        sidebar.child(navEntry("Proxies",  Tab.PROXIES));
        sidebar.child(navEntry("Rules",    Tab.ACTIVITY));

        sidebar.child(UIComponents.box(Sizing.fill(100), Sizing.fixed(1))
                .color(ACCENT_SOFT).fill(true).margins(Insets.vertical(6)));

        EngineSnapshot snap = engineSnap != null ? engineSnap : runtime.getEngineSnapshot();
        FlowLayout agentCard = infoCard("Proxy Engine");
        agentCard.child(metricLabel(
                snap != null && snap.getRunning() ? "RUNNING" : "STOPPED",
                snap != null && snap.getRunning() ? SUCCESS : ERROR));
        if (snap != null && snap.getRunning()) {
            agentCard.child(bodyLabel("Tunnel " + snap.getTunnelAddress()));
        } else if (snap != null && !snap.getError().isEmpty()) {
            agentCard.child(bodyLabel(trim(snap.getError(), 36)));
        }
        sidebar.child(agentCard);

        FlowLayout statsCard = infoCard("Snapshot");
        statsCard.child(bodyLabel("Saved servers: "  + savedServers.size()));
        statsCard.child(bodyLabel("Proxies: "        + runtime.getConfig().getProfiles().size()));
        statsCard.child(bodyLabel("Enabled routes: " + enabledRulesCount()));
        sidebar.child(statsCard);

        // Spacer pushes Close to bottom
        sidebar.child(UIComponents.box(Sizing.fill(100), Sizing.expand())
                .color(Color.ofArgb(0x00000000)).fill(true));
        sidebar.child(actionButton("Close", b -> close()).horizontalSizing(Sizing.fill(100)));
        return sidebar;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Workspace
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout buildWorkspace() {
        FlowLayout ws = UIContainers.verticalFlow(Sizing.expand(), Sizing.fill(100));
        ws.gap(12);
        ws.child(buildHeader());
        if (!flashMessage.isBlank()) ws.child(buildFlash());
        ws.child(buildCurrentTab());
        return ws;
    }

    private FlowLayout buildHeader() {
        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.surface(Surface.flat(PANEL_ALT).and(Surface.outline(OUTLINE)));
        header.padding(Insets.of(10));
        header.gap(10);
        header.verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout heading = UIContainers.verticalFlow(Sizing.expand(), Sizing.content());
        heading.gap(2);
        heading.child(titleLabel(currentTabTitle()));
        heading.child(bodyLabel(currentTabSubtitle()));
        header.child(heading);

        FlowLayout searchCol = UIContainers.verticalFlow(Sizing.fixed(220), Sizing.content());
        searchCol.gap(4);
        this.searchBox = UIComponents.textBox(Sizing.fill(100));
        this.searchBox.text(searchText());
        this.searchBox.setSuggestion("Search…");
        this.searchBox.onChanged().subscribe(v -> {
            saveSearchText(v);
            ensureSelections();
            rebuildUi();
        });
        searchCol.child(styledBox(this.searchBox));
        header.child(searchCol);

        ButtonComponent refresh = actionButton("Refresh", b -> {
            engineSnap = runtime.getEngineSnapshot();
            loadSavedServers();
            ensureSelections();
            flash("Updated agent state and saved server list.", SUCCESS);
            rebuildUi();
        });
        refresh.horizontalSizing(Sizing.fixed(92));
        header.child(refresh);
        return header;
    }

    private FlowLayout buildFlash() {
        FlowLayout f = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        f.surface(Surface.flat(0x8F161A18).and(Surface.outline(flashColor.argb())));
        f.padding(Insets.of(8));
        f.child(UIComponents.label(Text.literal(flashMessage)).color(flashColor).shadow(false));
        return f;
    }

    private FlowLayout buildCurrentTab() {
        return switch (currentTab) {
            case SERVERS  -> buildServersTab();
            case PROXIES  -> buildProxiesTab();
            case ACTIVITY -> buildActivityTab();
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Tab: Servers
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout buildServersTab() {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        row.gap(12);
        row.child(buildServersListPane());
        row.child(isEditingRule ? buildInlineRuleEditPane() : buildServerDetailPane());
        return row;
    }

    private FlowLayout buildServersListPane() {
        FlowLayout pane = pane(Sizing.fill(40));
        pane.child(sectionHeader("Saved Servers", "Choose one of your normal multiplayer entries"));

        FlowLayout list = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        list.gap(6);
        list.padding(Insets.right(8));
        for (ServerInfo server : filteredServers()) {
            list.child(serverEntry(server));
        }
        if (list.children().isEmpty()) {
            list.child(emptyState("No saved servers found",
                    "Add servers in Minecraft Multiplayer first, then assign routes here."));
        }

        serversListScroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), list);
        serversListScroll.surface(Surface.flat(0x00000000));
        pane.child(serversListScroll);

        FlowLayout footer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        footer.gap(8);
        ButtonComponent reload = actionButton("Reload List", b -> {
            loadSavedServers();
            ensureSelections();
            flash("Reloaded saved Minecraft servers.", SUCCESS);
            rebuildUi();
        });
        reload.horizontalSizing(Sizing.fixed(106));
        footer.child(reload);
        pane.child(footer);
        return pane;
    }

    /** Server list entry: favicon + MOTD + ping + player count + route status. */
    private FlowLayout serverEntry(ServerInfo server) {
        boolean selected = server.address.equals(selectedServerAddress);

        FlowLayout card = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.flat(selected ? CARD_ACTIVE : CARD)
                .and(Surface.outline(selected ? ACCENT.argb() : OUTLINE)));
        card.padding(Insets.of(6));

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        row.verticalAlignment(VerticalAlignment.TOP);

        // ── Favicon (32×32) — only if available (servers must be pinged first) ─
        Identifier faviconId = getOrRegisterFavicon(server);
        if (faviconId != null) {
            row.child(UIComponents.texture(faviconId, 0, 0, 64, 64, 64, 64)
                    .blend(true)
                    .horizontalSizing(Sizing.fixed(32))
                    .verticalSizing(Sizing.fixed(32)));
        }

        // ── Info column ───────────────────────────────────────────────────────
        FlowLayout info = UIContainers.verticalFlow(Sizing.expand(), Sizing.content());
        info.gap(2);

        // Server name — whole card is clickable via mouseDown
        LabelComponent nameLabel = UIComponents.label(Text.literal(trim(server.name, 32)));
        nameLabel.shadow(false).color(selected ? ACCENT : TEXT);
        info.child(nameLabel);

        // MOTD (first line, plain text)
        if (server.label != null) {
            String motdRaw = server.label.getString().trim();
            if (!motdRaw.isBlank()) {
                String firstLine = motdRaw.contains("\n")
                        ? motdRaw.substring(0, motdRaw.indexOf('\n')).trim()
                        : motdRaw;
                if (!firstLine.isBlank()) {
                    info.child(UIComponents.label(Text.literal(trim(firstLine, 44)))
                            .shadow(false).color(MUTED)
                            .margins(Insets.of(0, 0, 0, 1)));
                }
            }
        }

        // Address / ping / player count
        FlowLayout meta = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        meta.gap(8);
        meta.child(UIComponents.label(Text.literal(trim(server.address, 26)))
                .shadow(false).color(MUTED));
        if (server.ping > 0) {
            Color pingColor = server.ping < 100 ? SUCCESS : server.ping < 300 ? ACCENT : ERROR;
            meta.child(UIComponents.label(Text.literal(server.ping + "ms"))
                    .shadow(false).color(pingColor));
        }
        if (server.playerCountLabel != null && !server.playerCountLabel.getString().isBlank()) {
            meta.child(UIComponents.label(server.playerCountLabel).shadow(false));
        }
        info.child(meta);

        // Proxy / Direct status
        info.child(UIComponents.label(Text.literal(serverRouteSummary(server)))
                .shadow(false).color(selected ? ACCENT : MUTED));

        row.child(info);
        card.child(row);
        card.mouseDown().subscribe((click, consumed) -> {
            if (click.button() == 0) {
                selectedServerAddress = server.address;
                serverProfileChoiceId = preferredProfileIdForServer(server);
                proxyPickerExpanded   = false;
                rebuildUi();
                return true;
            }
            return false;
        });
        return card;
    }

    private FlowLayout buildServerDetailPane() {
        FlowLayout pane = pane(Sizing.expand());
        pane.child(sectionHeader("Server Detail", "Assign a proxy to the selected server"));

        ServerInfo server = selectedServer();
        if (server == null) {
            pane.child(emptyState("No server selected",
                    "Pick a server from the left list to assign a proxy."));
            return pane;
        }

        ResolvedServer resolved  = resolveServer(server);
        RoutingRule    anyRule   = findExactRuleAny(server);
        RoutingDecision decision = resolved == null ? null
                : router.decide(runtime.getConfig(),
                        new ru.dverkask.routeweaver.client.routing.ServerAddress(resolved.host, resolved.port));

        // Target card
        String[] status = serverRouteStatusPair(server, decision);
        Color statusColor = switch (status[1]) {
            case "success" -> SUCCESS;
            case "warn"    -> ACCENT;
            default        -> MUTED;
        };
        FlowLayout targetCard = infoCard("Target");
        targetCard.child(titleLabel(server.name));
        targetCard.child(bodyLabel(server.address));
        targetCard.child(metricLabel(status[0], statusColor));
        pane.child(targetCard);

        // Quick Route card — inline proxy picker replacing the cycling button
        FlowLayout quickRoute = infoCard("Quick Route");

        List<ProxyProfile> profiles = runtime.getConfig().getProfiles();
        if (profiles.isEmpty()) {
            quickRoute.child(bodyLabel("No proxy profiles yet. Add one in the Proxies tab first."));
        } else {
            ProxyProfile chosenProfile = profileById(serverProfileChoiceId);
            String chosenName = chosenProfile != null
                    ? trim(displayProfileName(chosenProfile), 32)
                    : "Select proxy…";
            ButtonComponent dropdownBtn = actionButton(
                    chosenName + (proxyPickerExpanded ? "  ▲" : "  ▼"),
                    b -> { proxyPickerExpanded = !proxyPickerExpanded; rebuildUi(); });
            dropdownBtn.horizontalSizing(Sizing.fill(100));
            dropdownBtn.renderer(ButtonComponent.Renderer.flat(
                    proxyPickerExpanded ? CARD_ACTIVE : 0xFF21252B, 0xFF30363E, CARD_DISABLED));

            FlowLayout dropdownWrapper = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
            if (proxyPickerExpanded) dropdownWrapper.surface(Surface.outline(ACCENT.argb()));
            dropdownWrapper.child(dropdownBtn);
            quickRoute.child(dropdownWrapper);

            if (proxyPickerExpanded) {
                FlowLayout pickerList = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
                pickerList.surface(Surface.flat(0x22000000).and(Surface.outline(OUTLINE)));
                pickerList.padding(Insets.of(2, 4, 2, 4));
                for (ProxyProfile profile : profiles) {
                    pickerList.child(proxyPickerRow(profile,
                            profile.getId().equals(serverProfileChoiceId)));
                }
                int visibleRows = Math.min(profiles.size(), 4);
                ScrollContainer<FlowLayout> pickerScroll = UIContainers.verticalScroll(
                        Sizing.fill(100), Sizing.fixed(visibleRows * 26), pickerList);
                pickerScroll.surface(Surface.flat(0x00000000));
                quickRoute.child(pickerScroll);
            }
        }

        ButtonComponent save = actionButton("Save Route", b -> routeSelectedServer());
        save.active(serverProfileChoiceId != null && !serverProfileChoiceId.isBlank());
        save.horizontalSizing(Sizing.fill(100));
        quickRoute.child(save);

        if (anyRule != null) {
            boolean routeEnabled = anyRule.isEnabled();
            ButtonComponent toggleRoute = actionButton(
                    routeEnabled ? "● Route enabled" : "○ Route disabled",
                    b -> {
                        anyRule.setEnabled(!anyRule.isEnabled());
                        runtime.saveConfig();
                        engineSnap = runtime.getEngineSnapshot();
                        rebuildUi();
                    });
            toggleRoute.horizontalSizing(Sizing.fill(100));
            toggleRoute.renderer(ButtonComponent.Renderer.flat(
                    routeEnabled ? 0xFF1A2E1A : 0xFF2E1A1A,
                    routeEnabled ? 0xFF203520 : 0xFF352020,
                    CARD_DISABLED));
            quickRoute.child(toggleRoute);
        }

        ButtonComponent editRule = actionButton("Edit", b -> startEditRule());
        editRule.horizontalSizing(Sizing.fill(100));
        quickRoute.child(editRule);

        pane.child(quickRoute);

        // What this does
        FlowLayout explain = infoCard("What this does");
        explain.child(bodyLabel("Save Route creates or updates one exact rule for this server."));
        if (resolved != null) {
            explain.child(bodyLabel("Exact match: " + resolved.host + ":" + resolved.port));
        }
        explain.child(bodyLabel(anyRule != null
                ? "This server already has an exact route bound to a proxy."
                : "No exact route yet — the server will connect directly until you save one."));
        pane.child(explain);
        return pane;
    }

    /** Single row in the proxy picker list. */
    private FlowLayout proxyPickerRow(ProxyProfile profile, boolean selected) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.padding(Insets.of(2, 4, 2, 4));
        row.verticalAlignment(VerticalAlignment.CENTER);
        if (selected) row.surface(Surface.outline(ACCENT.argb()));

        String label = trim(displayProfileName(profile), 30)
                + (profile.isEnabled() ? "" : "  [off]");
        ButtonComponent btn = listButton(label, selected, c -> {
            serverProfileChoiceId = profile.getId();
            proxyPickerExpanded   = false;
            rebuildUi();
        });
        btn.horizontalSizing(Sizing.expand());
        row.child(btn);

        return row;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Tab: Proxies
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout buildProxiesTab() {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        row.gap(12);
        row.child(buildProxiesListPane());
        row.child(isEditingProxy ? buildInlineProxyEditPane() : buildProxyDetailPane());
        return row;
    }

    private FlowLayout buildProxiesListPane() {
        FlowLayout pane = pane(Sizing.fill(40));
        pane.child(sectionHeader("Proxy Profiles",
                "Store Shadowsocks endpoints and switch between them quickly"));

        FlowLayout list = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        list.gap(6);
        list.padding(Insets.right(8));
        for (ProxyProfile profile : filteredProfiles()) {
            list.child(profileEntry(profile));
        }
        if (list.children().isEmpty()) {
            list.child(emptyState("No proxies yet",
                    "Add a Shadowsocks profile and import the full ss:// link in one step."));
        }

        proxiesListScroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), list);
        proxiesListScroll.surface(Surface.flat(0x00000000));
        pane.child(proxiesListScroll);

        FlowLayout footer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        footer.gap(8);
        footer.child(actionButton("Add Proxy", b -> startAddProxy())
                .horizontalSizing(Sizing.fixed(96)));
        pane.child(footer);
        return pane;
    }

    private FlowLayout profileEntry(ProxyProfile profile) {
        boolean selected = profile.getId().equals(selectedProfileId);
        FlowLayout wrapper = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(selected ? CARD_ACTIVE : CARD)
                .and(Surface.outline(selected ? ACCENT.argb() : OUTLINE)));
        wrapper.padding(Insets.of(2));

        LabelComponent nameLabel = UIComponents.label(Text.literal(displayProfileName(profile)));
        nameLabel.shadow(false).color(selected ? ACCENT : TEXT).margins(Insets.of(6, 6, 3, 8));
        wrapper.child(nameLabel);
        wrapper.child(UIComponents.label(Text.literal(profile.getHost() + ":" + profile.getPort()))
                .shadow(false).color(MUTED).margins(Insets.of(0, 6, 4, 8)));
        wrapper.child(UIComponents.label(Text.literal(
                        profile.isEnabled() ? profile.getMode().name() : "DISABLED"))
                .shadow(false)
                .color(profile.isEnabled() ? ACCENT : ERROR)
                .margins(Insets.of(0, 8, 8, 8)));
        wrapper.mouseDown().subscribe((click, consumed) -> {
            if (click.button() == 0) {
                selectedProfileId = profile.getId();
                isEditingProxy    = false;
                rebuildUi();
                return true;
            }
            return false;
        });
        return wrapper;
    }

    private FlowLayout buildProxyDetailPane() {
        FlowLayout pane = pane(Sizing.expand());
        pane.child(sectionHeader("Proxy Detail", "Inspect and maintain the selected endpoint"));

        ProxyProfile profile = selectedProfile();
        if (profile == null) {
            pane.child(emptyState("No proxy selected",
                    "Choose a profile from the left or add a new one."));
            return pane;
        }

        pane.child(infoCard("Endpoint")
                .child(titleLabel(displayProfileName(profile)))
                .child(bodyLabel(profile.getHost() + ":" + profile.getPort()))
                .child(bodyLabel("Method: " + trim(profile.getMethod(), 40)))
                .child(metricLabel(profile.isEnabled() ? "Enabled" : "Disabled",
                        profile.isEnabled() ? SUCCESS : ERROR)));

        FlowLayout usage = infoCard("Usage");
        usage.child(bodyLabel("Rules using this proxy: " + countRulesForProfile(profile.getId())));
        usage.child(bodyLabel("Mode: " + profile.getMode().name()));
        usage.child(bodyLabel("ID: "   + profile.getId()));
        pane.child(usage);

        FlowLayout actions = infoCard("Actions");
        FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        btnRow.gap(8);
        btnRow.child(actionButton("Edit", b -> startEditProxy())
                .horizontalSizing(Sizing.expand()));
        ButtonComponent delete = actionButton("Delete", b -> deleteSelectedProfile());
        delete.active(!isProfileReferenced(profile.getId()));
        btnRow.child(delete.horizontalSizing(Sizing.expand()));
        actions.child(btnRow);
        if (isProfileReferenced(profile.getId())) {
            actions.child(bodyLabel(
                    "Delete is locked because at least one route still points at this proxy."));
        }
        pane.child(actions);
        return pane;
    }

    // ─── Inline proxy edit form ────────────────────────────────────────────────

    private FlowLayout buildInlineProxyEditPane() {
        FlowLayout pane = pane(Sizing.expand());
        pane.child(sectionHeader(
                editingProfileId.isBlank() ? "New Proxy" : "Edit Proxy",
                "Configure the Shadowsocks endpoint"));

        // SS import
        FlowLayout importCard = infoCard("Import from ss:// link");
        FlowLayout importRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        importRow.gap(8);
        TextBoxComponent importBox = UIComponents.textBox(Sizing.expand(), editImportInput);
        importBox.setMaxLength(4096);
        importBox.onChanged().subscribe(v -> this.editImportInput = v);
        FlowLayout importBoxWrapper = styledBox(importBox);
        importBoxWrapper.horizontalSizing(Sizing.expand());
        importRow.child(importBoxWrapper);
        importRow.child(actionButton("Import", b -> importSsLinkInline())
                .horizontalSizing(Sizing.fixed(72)));
        importCard.child(importRow);
        pane.child(importCard);

        // Form fields
        FlowLayout formCard = infoCard("Profile settings");

        // Name — full width
        formCard.child(formField("Name", editNameInput, v -> editNameInput = v));

        // Host + Port
        FlowLayout row1 = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row1.gap(10);
        row1.child(formField("Host", editHostInput, v -> editHostInput = v)
                .horizontalSizing(Sizing.expand()));
        row1.child(formField("Port", editPortInput, v -> editPortInput = v)
                .horizontalSizing(Sizing.fixed(90)));
        formCard.child(row1);

        // Method + Password
        FlowLayout row2 = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row2.gap(10);
        row2.child(formField("Method", editMethodInput, v -> editMethodInput = v)
                .horizontalSizing(Sizing.expand()));
        row2.child(formField("Password", editPasswordInput, v -> editPasswordInput = v)
                .horizontalSizing(Sizing.expand()));
        formCard.child(row2);

        // UUID + SNI (optional, Shadowsocks-only fields)
        FlowLayout row3 = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row3.gap(10);
        row3.child(formField("UUID (optional)", editUuidInput, v -> editUuidInput = v)
                .horizontalSizing(Sizing.expand()));
        row3.child(formField("SNI (optional)", editSniInput, v -> editSniInput = v)
                .horizontalSizing(Sizing.expand()));
        formCard.child(row3);

        pane.child(formCard);

        // Status toggle
        FlowLayout toggleCard = infoCard("Status");
        ButtonComponent toggleBtn = actionButton(
                editEnabledInput ? "● Enabled" : "○ Disabled",
                b -> { editEnabledInput = !editEnabledInput; rebuildUi(); });
        toggleBtn.horizontalSizing(Sizing.fill(100));
        toggleBtn.renderer(ButtonComponent.Renderer.flat(
                editEnabledInput ? 0xFF1A2E1A : 0xFF2E1A1A,
                editEnabledInput ? 0xFF203520 : 0xFF352020,
                CARD_DISABLED));
        toggleCard.child(toggleBtn);
        toggleCard.child(bodyLabel("Only Shadowsocks is supported in the current build."));
        pane.child(toggleCard);

        // Validation error
        if (!editValidationError.isBlank()) {
            pane.child(UIComponents.label(Text.literal(editValidationError))
                    .shadow(false).color(ERROR));
        }

        // Save / Cancel
        FlowLayout actionRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        actionRow.gap(8);
        actionRow.child(actionButton("Save", b -> saveInlineProxy())
                .horizontalSizing(Sizing.expand()));
        actionRow.child(actionButton("Cancel", b -> cancelInlineEdit())
                .horizontalSizing(Sizing.expand()));
        pane.child(actionRow);

        return pane;
    }

    /** A labeled text-box field, returns the vertical container. */
    private FlowLayout formField(String label, String value,
                                 java.util.function.Consumer<String> onChange) {
        FlowLayout col = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        col.gap(3);
        col.child(sectionLabel(label));
        TextBoxComponent box = UIComponents.textBox(Sizing.fill(100), value);
        box.setMaxLength(512);
        box.onChanged().subscribe(v -> onChange.accept(v));
        col.child(styledBox(box));
        return col;
    }

    /**
     * Wraps a TextBoxComponent in a dark-styled input row — removes the vanilla
     * white border and replaces it with the mod's dark surface + accent outline.
     */
    private FlowLayout styledBox(TextBoxComponent box) {
        box.setDrawsBackground(false);
        box.horizontalSizing(Sizing.expand());
        FlowLayout wrapper = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(0xFF0E1012).and(Surface.outline(OUTLINE)));
        wrapper.padding(Insets.of(4, 6, 4, 6));
        wrapper.verticalAlignment(VerticalAlignment.CENTER);
        wrapper.child(box);
        return wrapper;
    }

    /** Same as {@link #styledBox} but with a leading icon label (e.g. a loupe). */
    private FlowLayout styledBoxWithIcon(String icon, TextBoxComponent box) {
        box.setDrawsBackground(false);
        box.horizontalSizing(Sizing.expand());
        FlowLayout wrapper = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(0xFF0E1012).and(Surface.outline(OUTLINE)));
        wrapper.padding(Insets.of(4, 6, 4, 6));
        wrapper.gap(5);
        wrapper.verticalAlignment(VerticalAlignment.CENTER);
        wrapper.child(UIComponents.label(Text.literal(icon)).shadow(false).color(MUTED));
        wrapper.child(box);
        return wrapper;
    }

    // ─── Inline rule edit form ─────────────────────────────────────────────────

    private FlowLayout buildInlineRuleEditPane() {
        FlowLayout pane = pane(Sizing.expand());
        pane.child(sectionHeader(
                editingRuleId.isBlank() ? "New Rule" : "Edit Rule",
                "Pattern matching and port filtering for advanced routing"));

        // Fields
        FlowLayout formCard = infoCard("Rule settings");

        // Label — full width
        formCard.child(formField("Rule Name", editRuleLabelInput, v -> editRuleLabelInput = v));

        // Pattern + Port
        FlowLayout row1 = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row1.gap(10);
        row1.child(formField("Server Pattern", editRulePatternInput, v -> editRulePatternInput = v)
                .horizontalSizing(Sizing.expand()));
        row1.child(formField("Port", editRulePortInput, v -> editRulePortInput = v)
                .horizontalSizing(Sizing.fixed(90)));
        formCard.child(row1);

        formCard.child(bodyLabel("Examples: hypixel.net  ·  1.2.3.4  ·  *.example.net"));
        pane.child(formCard);

        // Proxy profile picker (same style as Quick Route)
        FlowLayout profileCard = infoCard("Proxy profile");
        List<ProxyProfile> profiles = runtime.getConfig().getProfiles();
        if (profiles.isEmpty()) {
            profileCard.child(bodyLabel("No profiles yet — add one in the Proxies tab."));
        } else {
            ProxyProfile chosenRuleProfile = profileById(editRuleProfileId);
            String chosenRuleName = chosenRuleProfile != null
                    ? trim(displayProfileName(chosenRuleProfile), 32)
                    : "Select proxy…";
            ButtonComponent ruleDropdownBtn = actionButton(
                    chosenRuleName + (ruleProxyPickerExpanded ? "  ▲" : "  ▼"),
                    b -> { ruleProxyPickerExpanded = !ruleProxyPickerExpanded; rebuildUi(); });
            ruleDropdownBtn.horizontalSizing(Sizing.fill(100));
            ruleDropdownBtn.renderer(ButtonComponent.Renderer.flat(
                    ruleProxyPickerExpanded ? CARD_ACTIVE : 0xFF21252B, 0xFF30363E, CARD_DISABLED));

            FlowLayout ruleDropdownWrapper = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
            if (ruleProxyPickerExpanded) ruleDropdownWrapper.surface(Surface.outline(ACCENT.argb()));
            ruleDropdownWrapper.child(ruleDropdownBtn);
            profileCard.child(ruleDropdownWrapper);

            if (ruleProxyPickerExpanded) {
                FlowLayout pickerList = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
                pickerList.surface(Surface.flat(0x22000000).and(Surface.outline(OUTLINE)));
                pickerList.padding(Insets.of(2, 4, 2, 4));
                for (ProxyProfile profile : profiles) {
                    pickerList.child(ruleProfilePickerRow(profile,
                            profile.getId().equals(editRuleProfileId)));
                }
                int visibleRows = Math.min(profiles.size(), 4);
                ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(
                        Sizing.fill(100), Sizing.fixed(visibleRows * 26), pickerList);
                scroll.surface(Surface.flat(0x00000000));
                profileCard.child(scroll);
            }
        }
        pane.child(profileCard);

        // Validation error
        if (!editRuleValidationError.isBlank()) {
            pane.child(UIComponents.label(Text.literal(editRuleValidationError))
                    .shadow(false).color(ERROR));
        }

        // Save / Cancel / Delete
        FlowLayout actionRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        actionRow.gap(8);
        actionRow.child(actionButton("Save", b -> saveInlineRule()).horizontalSizing(Sizing.expand()));
        actionRow.child(actionButton("Cancel", b -> cancelInlineRule()).horizontalSizing(Sizing.expand()));
        pane.child(actionRow);

        if (!editingRuleId.isBlank()) {
            ButtonComponent deleteBtn = actionButton("Delete Rule", b -> {
                runtime.getConfig().getRules().removeIf(r -> editingRuleId.equals(r.getId()));
                runtime.saveConfig();
                engineSnap = runtime.getEngineSnapshot();
                isEditingRule = false;
                ruleProxyPickerExpanded = false;
                flash("Rule deleted.", SUCCESS);
                rebuildUi();
            });
            deleteBtn.horizontalSizing(Sizing.fill(100));
            deleteBtn.renderer(ButtonComponent.Renderer.flat(0xFF2E1A1A, 0xFF3D2020, CARD_DISABLED));
            pane.child(deleteBtn);
        }
        return pane;
    }

    private FlowLayout ruleProfilePickerRow(ProxyProfile profile, boolean selected) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.padding(Insets.of(2, 4, 2, 4));
        row.verticalAlignment(VerticalAlignment.CENTER);
        if (selected) row.surface(Surface.outline(ACCENT.argb()));
        String label = trim(displayProfileName(profile), 30)
                + (profile.isEnabled() ? "" : "  [off]");
        ButtonComponent btn = listButton(label, selected, c -> {
            editRuleProfileId       = profile.getId();
            ruleProxyPickerExpanded = false;
            rebuildUi();
        });
        btn.horizontalSizing(Sizing.expand());
        row.child(btn);
        return row;
    }

    private void startEditRuleById(String ruleId) {
        RoutingRule existing = ruleById(ruleId);
        if (existing == null) return;
        editingRuleId           = existing.getId();
        editRuleIdInput         = existing.getId();
        editRuleLabelInput      = existing.getLabel() != null ? existing.getLabel() : "";
        editRulePatternInput    = existing.getServerPattern();
        editRulePortInput       = existing.getPort() > 0 ? String.valueOf(existing.getPort()) : "";
        editRuleProfileId       = existing.getProxyProfileId();
        editRuleValidationError = "";
        ruleProxyPickerExpanded = false;
        isEditingRule           = true;
        rebuildUi();
    }

    private void startEditRule() {
        ServerInfo sv = selectedServer();
        RoutingRule existing = findExactRuleAny(sv);
        editingRuleId           = existing != null ? existing.getId() : "";
        editRuleIdInput         = existing != null ? existing.getId() : "";
        editRuleLabelInput      = sv != null ? sv.name : (existing != null ? existing.getLabel() : "");
        editRulePatternInput    = existing != null ? existing.getServerPattern() : "";
        editRulePortInput       = (existing != null && existing.getPort() > 0)
                ? String.valueOf(existing.getPort()) : "";
        editRuleProfileId       = existing != null ? existing.getProxyProfileId() : serverProfileChoiceId;
        editRuleValidationError = "";
        // Pre-fill pattern from resolved server if creating new
        if (existing == null && sv != null) {
            ResolvedServer resolved = resolveServer(sv);
            if (resolved != null) {
                editRulePatternInput = resolved.host();
                editRulePortInput    = String.valueOf(resolved.port());
            }
        }
        isEditingRule = true;
        rebuildUi();
    }

    private void cancelInlineRule() {
        isEditingRule           = false;
        editRuleValidationError = "";
        ruleProxyPickerExpanded = false;
        rebuildUi();
    }

    private void saveInlineRule() {
        String pattern = editRulePatternInput.trim();
        if (pattern.isBlank()) {
            editRuleValidationError = "Server pattern is required.";
            rebuildUi(); return;
        }
        if (editRuleProfileId.isBlank()) {
            editRuleValidationError = "Select a proxy profile for the rule.";
            rebuildUi(); return;
        }

        boolean isNew = editingRuleId.isBlank();
        Set<String> taken = ruleIdsExcluding(editingRuleId);
        String nameForId = editRuleLabelInput.trim().isBlank() ? pattern : editRuleLabelInput.trim();

        RoutingRule rule = isNew ? new RoutingRule() : ruleById(editingRuleId);
        if (rule == null) rule = new RoutingRule();

        rule.setId(ConfigIds.uniqueId(nameForId, "route", taken));
        rule.setLabel(editRuleLabelInput.trim());
        rule.setServerPattern(pattern);
        rule.setPort(parsePort(editRulePortInput));
        rule.setProxyProfileId(editRuleProfileId);

        if (isNew) {
            runtime.getConfig().getRules().add(rule);
        } else {
            int idx = indexOfRuleById(editingRuleId);
            if (idx >= 0) runtime.getConfig().getRules().set(idx, rule);
            else          runtime.getConfig().getRules().add(rule);
        }

        runtime.saveConfig();
        engineSnap = runtime.getEngineSnapshot();
        flash("Saved rule for " + pattern + ".", SUCCESS);
        isEditingRule           = false;
        editRuleValidationError = "";
        ruleProxyPickerExpanded = false;
        rebuildUi();
    }

    private RoutingRule ruleById(String id) {
        for (RoutingRule r : runtime.getConfig().getRules()) {
            if (id.equals(r.getId())) return r;
        }
        return null;
    }

    private int indexOfRuleById(String id) {
        List<RoutingRule> list = runtime.getConfig().getRules();
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) return i;
        }
        return -1;
    }

    // ─── Inline edit actions ───────────────────────────────────────────────────

    private void startAddProxy() {
        editingProfileId    = "";
        editImportInput     = "";
        editNameInput       = "";
        editHostInput       = "";
        editPortInput       = "";
        editMethodInput     = "";
        editPasswordInput   = "";
        editUuidInput       = "";
        editSniInput        = "";
        editEnabledInput    = true;
        editValidationError = "";
        isEditingProxy      = true;
        currentTab          = Tab.PROXIES;
        rebuildUi();
    }

    private void startEditProxy() {
        ProxyProfile p = selectedProfile();
        if (p == null) return;
        editingProfileId    = p.getId();
        editImportInput     = "";
        editNameInput       = p.getName() == null ? "" : p.getName();
        editHostInput       = p.getHost() == null ? "" : p.getHost();
        editPortInput       = p.getPort() > 0 ? String.valueOf(p.getPort()) : "";
        editMethodInput     = p.getMethod() == null ? "" : p.getMethod();
        editPasswordInput   = p.getPassword() == null ? "" : p.getPassword();
        editUuidInput       = p.getUuid() == null ? "" : p.getUuid();
        editSniInput        = p.getServerName() == null ? "" : p.getServerName();
        editEnabledInput    = p.isEnabled();
        editValidationError = "";
        isEditingProxy      = true;
        rebuildUi();
    }

    private void cancelInlineEdit() {
        isEditingProxy      = false;
        editValidationError = "";
        rebuildUi();
    }

    private void saveInlineProxy() {
        String host   = editHostInput.trim();
        String method = editMethodInput.trim();
        String pass   = editPasswordInput.trim();
        int    port   = parsePort(editPortInput);

        if (host.isBlank()) {
            editValidationError = "Host is required.";
            rebuildUi(); return;
        }
        if (port <= 0) {
            editValidationError = "Port must be a positive number.";
            rebuildUi(); return;
        }
        if (method.isBlank()) {
            editValidationError = "Encryption method is required.";
            rebuildUi(); return;
        }
        if (pass.isBlank()) {
            editValidationError = "Password is required.";
            rebuildUi(); return;
        }

        if (editingProfileId.isBlank()) {
            for (ProxyProfile existing : runtime.getConfig().getProfiles()) {
                if (existing.getHost().equalsIgnoreCase(host)
                        && existing.getPort() == port
                        && existing.getMethod().equalsIgnoreCase(method)
                        && existing.getPassword().equals(pass)) {
                    RouteWeaverToast.show(
                            Text.literal("Proxy already exists"),
                            Text.literal("\"" + displayProfileName(existing) + "\" has identical settings."));
                    return;
                }
            }
        }

        boolean isNew = editingProfileId.isBlank();
        ProxyProfile profile = isNew ? new ProxyProfile() : findProfileById(editingProfileId);
        if (profile == null) profile = new ProxyProfile();

        Set<String> takenIds = profileIdsExcluding(isNew ? "" : editingProfileId);
        String nameForId = editNameInput.trim().isBlank() ? host : editNameInput.trim();
        profile.setId(ConfigIds.uniqueId(nameForId, "profile", takenIds));
        profile.setName(editNameInput.trim());
        profile.setHost(host);
        profile.setPort(port);
        profile.setMethod(method);
        profile.setPassword(pass);
        profile.setUuid(editUuidInput.trim());
        profile.setServerName(editSniInput.trim());
        profile.setMode(ProxyMode.SHADOWSOCKS);
        profile.setEnabled(editEnabledInput);

        if (isNew) {
            runtime.getConfig().getProfiles().add(profile);
        } else {
            int idx = indexOfProfileById(editingProfileId);
            if (idx >= 0) runtime.getConfig().getProfiles().set(idx, profile);
            else          runtime.getConfig().getProfiles().add(profile);
        }

        runtime.saveConfig();
        selectedProfileId = profile.getId();
        if (serverProfileChoiceId.isBlank()) serverProfileChoiceId = profile.getId();

        flash((isNew ? "Added" : "Updated") + " proxy profile " + displayProfileName(profile) + ".", SUCCESS);
        isEditingProxy      = false;
        editValidationError = "";
        rebuildUi();
    }

    private void importSsLinkInline() {
        try {
            ShadowsocksUriParser.ParsedShadowsocksUri parsed =
                    ShadowsocksUriParser.parse(editImportInput.trim());
            if (!parsed.name().isBlank() && editNameInput.isBlank()) {
                editNameInput = parsed.name();
            } else if (editNameInput.isBlank()) {
                editNameInput = "Imported SS";
            }
            editHostInput       = parsed.host();
            editPortInput       = String.valueOf(parsed.port());
            editMethodInput     = parsed.method();
            editPasswordInput   = parsed.password();
            editValidationError = "";
        } catch (IllegalArgumentException e) {
            editValidationError = e.getMessage();
        }
        rebuildUi();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Tab: Activity
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout buildActivityTab() {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        row.gap(12);
        row.child(buildActivityListPane());
        row.child(isEditingRule ? buildInlineRuleEditPane() : buildRuleDetailPane());
        return row;
    }

    private FlowLayout buildActivityListPane() {
        FlowLayout pane = pane(Sizing.fill(40));
        pane.child(sectionHeader("All Rules",
                "Manage routing rules — toggle, edit or delete"));

        FlowLayout list = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        list.gap(6);
        list.padding(Insets.right(8));
        List<RoutingRule> rules = runtime.getConfig().getRules();
        for (RoutingRule rule : rules) {
            list.child(ruleManageEntry(rule));
        }
        if (list.children().isEmpty()) {
            list.child(emptyState("No rules yet",
                    "Create a route from the Servers tab."));
        }

        rulesListScroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), list);
        pane.child(rulesListScroll);
        return pane;
    }

    private FlowLayout ruleManageEntry(RoutingRule rule) {
        boolean enabled  = rule.isEnabled();
        boolean selected = rule.getId().equals(selectedRuleId);
        ProxyProfile profile = profileById(rule.getProxyProfileId());

        FlowLayout card = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.flat(selected ? CARD_ACTIVE : CARD)
                .and(Surface.outline(selected ? ACCENT.argb() : (enabled ? 0xFF2A3A2A : OUTLINE))));
        card.padding(Insets.of(8));
        card.gap(4);

        String patternText = rule.getServerPattern()
                + (rule.getPort() > 0 ? ":" + rule.getPort() : "");
        card.child(UIComponents.label(Text.literal(trim(patternText, 40)))
                .shadow(false).color(selected ? ACCENT : (enabled ? TEXT : MUTED)));
        card.child(UIComponents.label(Text.literal(profile != null
                        ? trim(displayProfileName(profile), 36) : "No proxy"))
                .shadow(false).color(MUTED));

        ButtonComponent toggle = actionButton(
                enabled ? "● Enabled" : "○ Disabled",
                b -> {
                    rule.setEnabled(!rule.isEnabled());
                    runtime.saveConfig();
                    engineSnap = runtime.getEngineSnapshot();
                    rebuildUi();
                });
        toggle.horizontalSizing(Sizing.fill(100));
        toggle.renderer(ButtonComponent.Renderer.flat(
                enabled ? 0xFF1A2E1A : 0xFF2E1A1A,
                enabled ? 0xFF203520 : 0xFF352020,
                CARD_DISABLED));
        card.child(toggle);

        card.mouseDown().subscribe((click, consumed) -> {
            if (click.button() == 0) {
                selectedRuleId = rule.getId();
                rebuildUi();
                return true;
            }
            return false;
        });
        return card;
    }

    private FlowLayout activityEntry(ServerInfo server) {
        boolean selected = server.address.equals(selectedRoutedServerAddress);
        FlowLayout wrapper = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(selected ? CARD_ACTIVE : CARD)
                .and(Surface.outline(selected ? ACCENT.argb() : OUTLINE)));
        wrapper.padding(Insets.of(2));

        LabelComponent nameLabel = UIComponents.label(Text.literal(server.name));
        nameLabel.shadow(false).color(selected ? ACCENT : TEXT).margins(Insets.of(6, 6, 3, 8));
        wrapper.child(nameLabel);
        wrapper.child(UIComponents.label(Text.literal(trim(server.address, 40)))
                .shadow(false).color(MUTED).margins(Insets.of(0, 6, 4, 8)));
        wrapper.child(UIComponents.label(Text.literal(trim(serverRouteSummary(server), 44)))
                .shadow(false).color(ACCENT).margins(Insets.of(0, 8, 8, 8)));
        wrapper.mouseDown().subscribe((click, consumed) -> {
            if (click.button() == 0) {
                selectedRoutedServerAddress = server.address;
                rebuildUi();
                return true;
            }
            return false;
        });
        return wrapper;
    }

    private FlowLayout buildRuleDetailPane() {
        FlowLayout pane = pane(Sizing.expand());

        RoutingRule rule = ruleById(selectedRuleId);
        if (rule == null) {
            pane.child(sectionHeader("Rule Detail", "Select a rule on the left to manage it"));
            pane.child(emptyState("No rule selected",
                    "Click on a rule to view its details, edit or delete it."));
            return pane;
        }

        ProxyProfile profile = profileById(rule.getProxyProfileId());
        boolean enabled = rule.isEnabled();

        pane.child(sectionHeader("Rule Detail", rule.getServerPattern()));

        FlowLayout infoCard = infoCard("Settings");
        infoCard.child(bodyLabel("Pattern: " + rule.getServerPattern()));
        infoCard.child(bodyLabel("Port: " + (rule.getPort() > 0 ? rule.getPort() : "any")));
        infoCard.child(bodyLabel("Proxy: " + (profile != null ? displayProfileName(profile) : "—")));
        infoCard.child(metricLabel(enabled ? "● Enabled" : "○ Disabled", enabled ? SUCCESS : MUTED));
        pane.child(infoCard);

        ButtonComponent editBtn = actionButton("Edit Rule", b -> startEditRuleById(selectedRuleId));
        editBtn.horizontalSizing(Sizing.fill(100));
        pane.child(editBtn);

        ButtonComponent deleteBtn = actionButton("Delete Rule", b -> {
            runtime.getConfig().getRules().removeIf(r -> selectedRuleId.equals(r.getId()));
            runtime.saveConfig();
            engineSnap = runtime.getEngineSnapshot();
            selectedRuleId = "";
            flash("Rule deleted.", SUCCESS);
            rebuildUi();
        });
        deleteBtn.horizontalSizing(Sizing.fill(100));
        deleteBtn.renderer(ButtonComponent.Renderer.flat(0xFF2E1A1A, 0xFF3D2020, CARD_DISABLED));
        pane.child(deleteBtn);

        return pane;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Primitive UI helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private FlowLayout pane(Sizing horizontalSizing) {
        FlowLayout p = UIContainers.verticalFlow(horizontalSizing, Sizing.fill(100));
        p.surface(Surface.flat(PANEL_ALT).and(Surface.outline(OUTLINE)));
        p.padding(Insets.of(12));
        p.gap(10);
        return p;
    }

    private FlowLayout infoCard(String title) {
        FlowLayout card = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.flat(CARD).and(Surface.outline(OUTLINE)));
        card.padding(Insets.of(10));
        card.gap(5);
        card.child(sectionLabel(title));
        return card;
    }

    private FlowLayout sectionHeader(String title, String subtitle) {
        FlowLayout h = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        h.gap(2);
        h.child(titleLabel(title));
        h.child(bodyLabel(subtitle));
        return h;
    }

    private FlowLayout emptyState(String title, String body) {
        FlowLayout s = infoCard(title);
        s.child(bodyLabel(body));
        return s;
    }

    private FlowLayout navEntry(String title, Tab tab) {
        boolean sel = currentTab == tab;
        FlowLayout wrapper = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(sel ? CARD_ACTIVE : CARD)
                .and(Surface.outline(sel ? ACCENT.argb() : OUTLINE)));
        wrapper.padding(Insets.of(8, 8, 10, 8));

        wrapper.child(UIComponents.label(Text.literal(title))
                .shadow(false).color(sel ? ACCENT : TEXT));

        wrapper.mouseDown().subscribe((click, consumed) -> {
            if (click.button() == 0) {
                currentTab     = tab;
                flashMessage   = "";
                isEditingProxy = false;
                isEditingRule  = false;
                ensureSelections();
                rebuildUi();
                return true;
            }
            return false;
        });
        return wrapper;
    }

    private ButtonComponent listButton(String title, boolean selected,
                                       java.util.function.Consumer<ButtonComponent> action) {
        Text label = Text.literal(title).withColor(selected ? ACCENT.argb() : TEXT.argb());
        ButtonComponent btn = UIComponents.button(label, action::accept);
        btn.horizontalSizing(Sizing.fill(100));
        btn.textShadow(false);
        btn.renderer(ButtonComponent.Renderer.flat(
                selected ? CARD_ACTIVE : 0x00111111,
                CARD_HOVER,
                CARD_DISABLED));
        return btn;
    }

    private ButtonComponent actionButton(String title,
                                         java.util.function.Consumer<ButtonComponent> action) {
        ButtonComponent btn = UIComponents.button(Text.literal(title), action::accept);
        btn.textShadow(false);
        btn.renderer(ButtonComponent.Renderer.flat(0xFF21252B, 0xFF30363E, CARD_DISABLED));
        return btn;
    }

    private LabelComponent titleLabel(String text) {
        return UIComponents.label(Text.literal(text)).color(TEXT).shadow(false);
    }

    private LabelComponent sectionLabel(String text) {
        return UIComponents.label(Text.literal(text)).color(ACCENT).shadow(false);
    }

    private LabelComponent bodyLabel(String text) {
        return UIComponents.label(Text.literal(text)).color(MUTED).shadow(false).maxWidth(420);
    }

    private LabelComponent metricLabel(String text, Color color) {
        return UIComponents.label(Text.literal(text)).color(color).shadow(false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Favicon
    // ═══════════════════════════════════════════════════════════════════════════

    private Identifier getOrRegisterFavicon(ServerInfo server) {
        byte[] raw = server.getFavicon();
        if (raw == null || raw.length == 0) return null;

        String key = server.address;
        if (FAVICON_CACHE.containsKey(key)) return FAVICON_CACHE.get(key);

        try {
            NativeImage img = NativeImage.read(raw);
            // Use vanilla WorldIcon so the identifier matches what vanilla MC registers
            WorldIcon icon = WorldIcon.forServer(
                    MinecraftClient.getInstance().getTextureManager(), key);
            icon.load(img);
            Identifier id = icon.getTextureId();
            FAVICON_ICONS.put(key, icon);
            FAVICON_CACHE.put(key, id);
            RouteWeaver.LOGGER.debug("Registered server favicon for {}: {}", key, id);
            return id;
        } catch (Exception e) {
            RouteWeaver.LOGGER.warn("Failed to load favicon for {}: {}", key, e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Data / business logic
    // ═══════════════════════════════════════════════════════════════════════════

    private void rebuildUi() {
        if (this.uiAdapter == null) return;
        double serversProg  = scrollProgress(serversListScroll);
        double proxiesProg  = scrollProgress(proxiesListScroll);
        double rulesProg    = scrollProgress(rulesListScroll);
        build(this.uiAdapter.rootComponent);
        this.uiAdapter.inflateAndMount();
        if (serversListScroll != null) restoreScroll(serversListScroll, serversProg);
        if (proxiesListScroll != null) restoreScroll(proxiesListScroll, proxiesProg);
        if (rulesListScroll   != null) restoreScroll(rulesListScroll,   rulesProg);
    }

    /** Reads the current scroll position as a 0–1 fraction via reflection. */
    private static double scrollProgress(ScrollContainer<?> scroll) {
        if (scroll == null) return 0.0;
        try {
            java.lang.reflect.Field off = ScrollContainer.class.getDeclaredField("scrollOffset");
            java.lang.reflect.Field max = ScrollContainer.class.getDeclaredField("maxScroll");
            off.setAccessible(true);
            max.setAccessible(true);
            double offset = (double) off.get(scroll);
            int    maxVal = (int)    max.get(scroll);
            return maxVal > 0 ? Math.min(1.0, offset / maxVal) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Restores scroll to the saved progress and immediately snaps currentScrollPosition
     * to the same value, preventing the animated slide-from-top effect.
     */
    private static void restoreScroll(ScrollContainer<?> scroll, double progress) {
        scroll.scrollTo(progress);
        try {
            java.lang.reflect.Field off = ScrollContainer.class.getDeclaredField("scrollOffset");
            java.lang.reflect.Field cur = ScrollContainer.class.getDeclaredField("currentScrollPosition");
            off.setAccessible(true);
            cur.setAccessible(true);
            cur.set(scroll, off.get(scroll)); // snap visual position = target position
        } catch (Exception ignored) {}
    }

    private void loadSavedServers() {
        savedServers.clear();
        if (this.client == null) return;
        ServerList sl = new ServerList(this.client);
        sl.loadFile();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < sl.size(); i++) {
            ServerInfo s = sl.get(i);
            if (seen.add(s.address.toLowerCase(Locale.ROOT))) {
                savedServers.add(s);
            }
        }
    }

    private void ensureSelections() {
        if ((selectedProfileId.isBlank() || profileById(selectedProfileId) == null)
                && !runtime.getConfig().getProfiles().isEmpty()) {
            selectedProfileId = runtime.getConfig().getProfiles().get(0).getId();
        }

        List<ServerInfo> vis = filteredServers();
        if (!vis.isEmpty() && vis.stream().noneMatch(s -> s.address.equals(selectedServerAddress))) {
            selectedServerAddress = vis.get(0).address;
        }
        if (vis.isEmpty()) selectedServerAddress = "";

        List<ServerInfo> routed = routedServers();
        if (!routed.isEmpty() && routed.stream()
                .noneMatch(s -> s.address.equals(selectedRoutedServerAddress))) {
            selectedRoutedServerAddress = routed.get(0).address;
        }
        if (routed.isEmpty()) selectedRoutedServerAddress = "";

        if (serverProfileChoiceId.isBlank() && !selectedServerAddress.isBlank()) {
            ServerInfo sv = selectedServer();
            if (sv != null) serverProfileChoiceId = preferredProfileIdForServer(sv);
        }
        if (!serverProfileChoiceId.isBlank() && profileById(serverProfileChoiceId) == null) {
            serverProfileChoiceId = "";
        }
    }

    private List<ServerInfo> filteredServers() {
        String q = searchText().trim().toLowerCase(Locale.ROOT);
        List<ServerInfo> out = new ArrayList<>();
        for (ServerInfo s : savedServers) {
            if (matchesQuery(s.name, q) || matchesQuery(s.address, q)) out.add(s);
        }
        return out;
    }

    private List<ProxyProfile> filteredProfiles() {
        String q = searchText().trim().toLowerCase(Locale.ROOT);
        List<ProxyProfile> out = new ArrayList<>();
        for (ProxyProfile p : runtime.getConfig().getProfiles()) {
            if (matchesQuery(p.getName(), q) || matchesQuery(p.getId(), q)
                    || matchesQuery(p.getHost(), q)) out.add(p);
        }
        return out;
    }

    private List<ServerInfo> routedServers() {
        String q = searchText().trim().toLowerCase(Locale.ROOT);
        List<ServerInfo> out = new ArrayList<>();
        for (ServerInfo s : savedServers) {
            ResolvedServer r = resolveServer(s);
            if (r == null) continue;
            RoutingDecision d = router.decide(runtime.getConfig(),
                    new ru.dverkask.routeweaver.client.routing.ServerAddress(r.host, r.port));
            if (d.shouldUseProxy() && (matchesQuery(s.name, q) || matchesQuery(s.address, q)))
                out.add(s);
        }
        return out;
    }

    private boolean matchesQuery(String value, String query) {
        return query.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(query));
    }

    private ServerInfo selectedServer() {
        for (ServerInfo s : savedServers) {
            if (s.address.equals(selectedServerAddress)) return s;
        }
        return null;
    }

    private ServerInfo routedServerSelection() {
        for (ServerInfo s : routedServers()) {
            if (s.address.equals(selectedRoutedServerAddress)) return s;
        }
        return null;
    }

    private ProxyProfile selectedProfile() { return profileById(selectedProfileId); }

    private ProxyProfile profileById(String id) {
        for (ProxyProfile p : runtime.getConfig().getProfiles()) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    private ProxyProfile findProfileById(String id) { return profileById(id); }

    private int indexOfProfileById(String id) {
        List<ProxyProfile> list = runtime.getConfig().getProfiles();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private void routeSelectedServer() {
        ServerInfo sv = selectedServer();
        ProxyProfile prof = profileById(serverProfileChoiceId);
        if (sv == null)   { flash("Choose a saved Minecraft server first.", ERROR);        rebuildUi(); return; }
        if (prof == null) { flash("Choose a proxy profile before saving the route.", ERROR); rebuildUi(); return; }

        ResolvedServer resolved = resolveServer(sv);
        if (resolved == null) {
            flash("Could not parse the saved server address.", ERROR); rebuildUi(); return;
        }

        RoutingRule existing = findExactRuleAny(sv);
        Set<String> taken   = ruleIdsExcluding(existing != null ? existing.getId() : "");
        boolean     isNew   = existing == null;
        RoutingRule rule    = isNew ? new RoutingRule() : existing;
        if (isNew) rule.setId(ConfigIds.uniqueId(sv.name, "route", taken));
        rule.setLabel(sv.name);
        rule.setServerPattern(resolved.host);
        rule.setPort(resolved.port);
        rule.setProxyProfileId(prof.getId());
        rule.setEnabled(true);

        if (isNew) runtime.getConfig().getRules().add(rule);
        runtime.saveConfig();
        engineSnap = runtime.getEngineSnapshot();
        flash("Saved route for " + sv.name + " via " + displayProfileName(prof) + ".", SUCCESS);
        rebuildUi();
    }

    private void clearSelectedServerRoute() {
        ServerInfo sv = selectedServer();
        RoutingRule rule = findExactRuleAny(sv);
        if (rule == null) {
            flash("This server does not have an exact saved route.", ERROR); rebuildUi(); return;
        }
        runtime.getConfig().getRules().removeIf(r -> {
            ResolvedServer res = resolveServer(sv);
            if (res == null) return false;
            if (!r.getServerPattern().equalsIgnoreCase(res.host)) return false;
            if (r.getPort() > 0 && r.getPort() != res.port) return false;
            return true;
        });
        runtime.saveConfig();
        engineSnap = runtime.getEngineSnapshot();
        flash("Removed the exact route for " + sv.name + ".", SUCCESS);
        rebuildUi();
    }

    private RoutingRule findExactRule(ServerInfo server) {
        if (server == null) return null;
        ResolvedServer resolved = resolveServer(server);
        if (resolved == null) return null;
        for (RoutingRule rule : runtime.getConfig().getRules()) {
            if (!rule.isEnabled()) continue;
            // Exact host match (case-insensitive), port must match or rule has no port constraint
            if (!rule.getServerPattern().equalsIgnoreCase(resolved.host)) continue;
            if (rule.getPort() > 0 && rule.getPort() != resolved.port) continue;
            return rule;
        }
        return null;
    }

    /** Like {@link #findExactRule} but also returns disabled rules. */
    private RoutingRule findExactRuleAny(ServerInfo server) {
        if (server == null) return null;
        ResolvedServer resolved = resolveServer(server);
        if (resolved == null) return null;
        for (RoutingRule rule : runtime.getConfig().getRules()) {
            if (!rule.getServerPattern().equalsIgnoreCase(resolved.host)) continue;
            if (rule.getPort() > 0 && rule.getPort() != resolved.port) continue;
            return rule;
        }
        return null;
    }


    private void deleteSelectedProfile() {
        ProxyProfile p = selectedProfile();
        if (p == null) return;
        if (isProfileReferenced(p.getId())) {
            flash("This proxy is still used by one or more routes.", ERROR); rebuildUi(); return;
        }
        runtime.getConfig().getProfiles().remove(p);
        runtime.saveConfig();
        selectedProfileId = "";
        if (serverProfileChoiceId.equals(p.getId())) serverProfileChoiceId = "";
        flash("Deleted proxy profile " + displayProfileName(p) + ".", SUCCESS);
        rebuildUi();
    }

    private String preferredProfileIdForServer(ServerInfo server) {
        RoutingRule rule = findExactRuleAny(server);
        if (rule != null && profileById(rule.getProxyProfileId()) != null)
            return rule.getProxyProfileId();
        if (selectedProfile() != null) return selectedProfile().getId();
        return runtime.getConfig().getProfiles().isEmpty()
                ? "" : runtime.getConfig().getProfiles().get(0).getId();
    }

    private String serverRouteSummary(ServerInfo server) {
        ResolvedServer resolved = resolveServer(server);
        if (resolved == null) return "Address could not be parsed";
        RoutingDecision d = router.decide(runtime.getConfig(),
                new ru.dverkask.routeweaver.client.routing.ServerAddress(resolved.host, resolved.port));
        if (d.shouldUseProxy()) return "Proxy → " + displayProfileName(d.profile());
        // Check if there's a rule but the profile is disabled
        for (RoutingRule rule : runtime.getConfig().getRules()) {
            if (!rule.isEnabled()) continue;
            if (!ServerPatternMatcher.matches(rule.getServerPattern(), resolved.host)) continue;
            if (rule.getPort() > 0 && rule.getPort() != resolved.port) continue;
            ProxyProfile linked = profileById(rule.getProxyProfileId());
            if (linked != null && !linked.isEnabled()) return "Route set (proxy disabled)";
        }
        return "Direct";
    }

    /** Returns (statusText, color) for the Target card. */
    private String[] serverRouteStatusPair(ServerInfo server, RoutingDecision decision) {
        if (decision != null && decision.shouldUseProxy()) {
            return new String[]{"Proxy: " + displayProfileName(decision.profile()), "success"};
        }
        ResolvedServer resolved = resolveServer(server);
        if (resolved != null) {
            for (RoutingRule rule : runtime.getConfig().getRules()) {
                if (!rule.isEnabled()) continue;
                if (!ServerPatternMatcher.matches(rule.getServerPattern(), resolved.host)) continue;
                if (rule.getPort() > 0 && rule.getPort() != resolved.port) continue;
                ProxyProfile linked = profileById(rule.getProxyProfileId());
                if (linked != null && !linked.isEnabled())
                    return new String[]{"Route set — proxy \"" + displayProfileName(linked) + "\" is disabled", "warn"};
            }
        }
        return new String[]{"Direct connection", "muted"};
    }

    private ResolvedServer resolveServer(ServerInfo server) {
        if (server == null || server.address == null || server.address.isBlank()) return null;
        ServerAddress parsed = ServerAddress.parse(server.address);
        if (parsed == null) return null;
        return new ResolvedServer(parsed.getAddress(), parsed.getPort());
    }

    private String displayProfileName(ProxyProfile profile) {
        return profile.getName().isBlank() ? profile.getId() : profile.getName();
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 3) + "…";
    }

    private int parsePort(String value) {
        try { return (value == null || value.isBlank()) ? 0 : Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String searchText() {
        return switch (currentTab) {
            case SERVERS  -> SearchState.SERVERS;
            case PROXIES  -> SearchState.PROXIES;
            case ACTIVITY -> SearchState.ACTIVITY;
        };
    }

    private void saveSearchText(String value) {
        switch (currentTab) {
            case SERVERS  -> SearchState.SERVERS  = value;
            case PROXIES  -> SearchState.PROXIES  = value;
            case ACTIVITY -> SearchState.ACTIVITY = value;
        }
    }

    private void flash(String message, Color color) {
        this.flashMessage = message;
        this.flashColor   = color;
    }

    private String currentTabTitle() {
        return switch (currentTab) {
            case SERVERS  -> "Route servers without touching raw patterns";
            case PROXIES  -> "Manage the Shadowsocks endpoints you actually use";
            case ACTIVITY -> "Active routing rules and agent state";
        };
    }

    private String currentTabSubtitle() {
        return switch (currentTab) {
            case SERVERS  -> "Saved multiplayer entries come first. Advanced rules are still available.";
            case PROXIES  -> "Profiles store credentials only. Routing starts from the server list.";
            case ACTIVITY -> "All routing rules, routed servers, and live agent state.";
        };
    }

    // ─── Counts & lookups ─────────────────────────────────────────────────────

    public Set<String> profileIdsExcluding(String excludedId) {
        Set<String> ids = new HashSet<>();
        for (ProxyProfile p : runtime.getConfig().getProfiles()) {
            if (p.getId() != null && !p.getId().isBlank() && !p.getId().equals(excludedId))
                ids.add(p.getId());
        }
        return ids;
    }

    public Set<String> ruleIdsExcluding(String excludedId) {
        Set<String> ids = new HashSet<>();
        for (RoutingRule r : runtime.getConfig().getRules()) {
            if (r.getId() != null && !r.getId().isBlank() && !r.getId().equals(excludedId))
                ids.add(r.getId());
        }
        return ids;
    }

    private boolean isProfileReferenced(String profileId) {
        for (RoutingRule r : runtime.getConfig().getRules()) {
            if (profileId.equals(r.getProxyProfileId())) return true;
        }
        return false;
    }

    private int enabledRulesCount() {
        int n = 0;
        for (RoutingRule r : runtime.getConfig().getRules()) if (r.isEnabled()) n++;
        return n;
    }

    private int countRulesForProfile(String profileId) {
        int n = 0;
        for (RoutingRule r : runtime.getConfig().getRules())
            if (profileId.equals(r.getProxyProfileId())) n++;
        return n;
    }

    private static RoutingRule copyOf(RoutingRule src) {
        RoutingRule t = new RoutingRule();
        t.setId(src.getId());
        t.setLabel(src.getLabel());
        t.setServerPattern(src.getServerPattern());
        t.setPort(src.getPort());
        t.setProxyProfileId(src.getProxyProfileId());
        t.setEnabled(src.isEnabled());
        return t;
    }

    @Override
    public void close() { this.client.setScreen(parent); }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Inner types
    // ═══════════════════════════════════════════════════════════════════════════

    private enum Tab { SERVERS, PROXIES, ACTIVITY }

    private static final class SearchState {
        private static String SERVERS  = "";
        private static String PROXIES  = "";
        private static String ACTIVITY = "";
    }

    private record ResolvedServer(String host, int port) {}
}
