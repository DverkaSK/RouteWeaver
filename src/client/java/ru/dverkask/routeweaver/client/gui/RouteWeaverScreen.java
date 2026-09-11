package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.client.ProxyRuntime;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * The RouteWeaver config screen: a sidebar with tabs and status, a header with search, and the
 * current tab below. Changes rebuild the sidebar and the tab but leave the header alone, so the
 * search box keeps focus while typing.
 */
public final class RouteWeaverScreen extends BaseOwoScreen<FlowLayout> implements ScreenHost {
    private static final int MAX_WIDTH = 1060;
    private static final int MAX_HEIGHT = 640;
    private static final int MIN_MARGIN = 16;
    private static final int SIDEBAR_WIDTH = 176;
    private static final int SEARCH_WIDTH = 220;
    private static final int ERROR_LENGTH = 36;

    private final Screen parent;
    private final ProxyRuntime runtime;
    private final ScrollMemory scrollMemory = new ScrollMemory();
    private final List<ConfigTab> tabs;
    private ConfigTab currentTab;
    private List<SavedServer> savedServers = List.of();
    private @Nullable Component flashMessage;
    private boolean flashIsError;

    private @Nullable FlowLayout sidebar;
    private @Nullable FlowLayout header;
    private @Nullable FlowLayout body;

    public RouteWeaverScreen(Screen parent, ProxyRuntime runtime) {
        super(Component.translatable("routeweaver.screen.title"));
        this.parent = parent;
        this.runtime = runtime;
        this.tabs = List.of(new ServersTab(this), new ProxiesTab(this), new RulesTab(this));
        this.currentTab = tabs.getFirst();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        savedServers = SavedServer.load(minecraft);

        root.surface(Surface.blur(2.5f, 4f).and(Surface.flat(Theme.BACKDROP)));
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.CENTER);
        applyMargins(root);

        sidebar = UIContainers.verticalFlow(Sizing.fixed(SIDEBAR_WIDTH), Sizing.fill(100));
        sidebar.surface(Surface.flat(Theme.PANEL_ALT).and(Surface.outline(Theme.OUTLINE)));
        sidebar.padding(Insets.of(12));
        sidebar.gap(10);

        header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.surface(Surface.flat(Theme.PANEL_ALT).and(Surface.outline(Theme.OUTLINE)));
        header.padding(Insets.of(10));
        header.gap(10);
        header.verticalAlignment(VerticalAlignment.CENTER);

        body = UIContainers.verticalFlow(Sizing.fill(100), Sizing.expand());
        body.gap(12);

        FlowLayout workspace = UIContainers.verticalFlow(Sizing.expand(), Sizing.fill(100));
        workspace.gap(12);
        workspace.child(header).child(body);

        FlowLayout shell = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        shell.surface(Surface.flat(Theme.PANEL).and(Surface.outline(Theme.OUTLINE)));
        shell.padding(Insets.of(14));
        shell.gap(14);
        root.child(shell.child(sidebar).child(workspace));

        rebuildHeader();
        refresh();
    }

    @Override
    protected void init() {
        super.init();
        // owo keeps the component tree on resize, so only the centring margins need updating.
        if (uiAdapter != null) {
            applyMargins(uiAdapter.rootComponent);
        }
    }

    private void applyMargins(FlowLayout root) {
        int horizontal = Math.max(MIN_MARGIN, (width - MAX_WIDTH) / 2);
        int vertical = Math.max(MIN_MARGIN, (height - MAX_HEIGHT) / 2);
        root.padding(Insets.of(vertical, vertical, horizontal, horizontal));
    }

    // ─── ScreenHost ─────────────────────────────────────────────────────────

    @Override
    public RouteWeaverConfig config() {
        return runtime.config();
    }

    @Override
    public List<SavedServer> savedServers() {
        return savedServers;
    }

    @Override
    public ScrollMemory scrollMemory() {
        return scrollMemory;
    }

    @Override
    public void saveAndRefresh() {
        runtime.saveConfig();
        refresh();
    }

    @Override
    public void refresh() {
        if (sidebar == null || body == null) {
            return;
        }
        scrollMemory.capture();
        rebuildSidebar();

        body.clearChildren();
        if (flashMessage != null) {
            FlowLayout flash = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
            flash.surface(Surface.flat(Theme.FLASH).and(Surface.outline(flashColor().argb())));
            flash.padding(Insets.of(8));
            body.child(flash.child(Widgets.label(flashMessage, flashColor())));
        }
        body.child(currentTab.build());
    }

    @Override
    public void flash(Component message, boolean error) {
        flashMessage = message;
        flashIsError = error;
    }

    private Color flashColor() {
        return flashIsError ? Theme.ERROR : Theme.SUCCESS;
    }

    // ─── Header ─────────────────────────────────────────────────────────────

    private void rebuildHeader() {
        if (header == null) {
            return;
        }
        header.clearChildren();

        FlowLayout heading = Widgets.heading(currentTab.title(), currentTab.subtitle());
        heading.horizontalSizing(Sizing.expand());
        header.child(heading);

        TextBoxComponent search = Widgets.textBox(currentTab.query(), 256, query -> {
            currentTab.query(query);
            refresh();
        });
        search.setHint(Component.translatable("routeweaver.search.hint"));
        FlowLayout searchInput = Widgets.input(search);
        searchInput.horizontalSizing(Sizing.fixed(SEARCH_WIDTH));
        header.child(searchInput);

        ButtonComponent reload = Widgets.button(Component.translatable("routeweaver.action.refresh"), () -> {
            savedServers = SavedServer.load(minecraft);
            flash(Component.translatable("routeweaver.flash.refreshed"), false);
            refresh();
        });
        reload.horizontalSizing(Sizing.fixed(92));
        header.child(reload);
    }

    // ─── Sidebar ────────────────────────────────────────────────────────────

    private void rebuildSidebar() {
        FlowLayout sidebar = this.sidebar;
        if (sidebar == null) {
            return;
        }
        sidebar.clearChildren();

        FlowLayout brand = Widgets.heading(
                Component.translatable("routeweaver.screen.title"),
                Component.translatable("routeweaver.sidebar.tagline"));
        brand.padding(Insets.of(2, 0, 6, 0));
        sidebar.child(brand);

        for (ConfigTab tab : tabs) {
            boolean selected = tab == currentTab;
            FlowLayout entry = Widgets.selectableCard(selected, () -> switchTab(tab));
            entry.padding(Insets.of(8, 8, 10, 8));
            sidebar.child(entry.child(Widgets.label(tab.navLabel(), selected ? Theme.ACCENT : Theme.TEXT)));
        }

        sidebar.child(UIComponents.box(Sizing.fill(100), Sizing.fixed(1))
                .color(Theme.ACCENT_SOFT)
                .fill(true)
                .margins(Insets.vertical(6)));

        sidebar.child(engineCard());

        RouteWeaverConfig config = config();
        sidebar.child(Widgets.card(Component.translatable("routeweaver.stats.title"))
                .child(Widgets.body(Component.translatable("routeweaver.stats.servers", savedServers.size())))
                .child(Widgets.body(Component.translatable("routeweaver.stats.proxies", config.getProfiles().size())))
                .child(Widgets.body(Component.translatable("routeweaver.stats.rules", config.countEnabledRules()))));

        sidebar.child(UIContainers.verticalFlow(Sizing.fill(100), Sizing.expand()));
        sidebar.child(Widgets.wideButton(Component.translatable("routeweaver.action.close"), this::onClose));
    }

    private FlowLayout engineCard() {
        FlowLayout card = Widgets.card(Component.translatable("routeweaver.engine.title"));
        InetSocketAddress tunnel = runtime.tunnelAddress();
        if (tunnel != null) {
            card.child(Widgets.label(Component.translatable("routeweaver.engine.running"), Theme.SUCCESS));
            card.child(Widgets.body(Component.translatable("routeweaver.engine.tunnel",
                    tunnel.getHostString() + ":" + tunnel.getPort())));
        } else {
            card.child(Widgets.label(Component.translatable("routeweaver.engine.stopped"), Theme.ERROR));
            if (!runtime.tunnelError().isEmpty()) {
                card.child(Widgets.body(Component.literal(Widgets.ellipsize(runtime.tunnelError(), ERROR_LENGTH))));
            }
        }
        return card;
    }

    private void switchTab(ConfigTab tab) {
        if (tab == currentTab) {
            return;
        }
        currentTab.onHidden();
        currentTab = tab;
        flashMessage = null;
        rebuildHeader();
        refresh();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
