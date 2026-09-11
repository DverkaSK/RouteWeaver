package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;

import java.util.List;

/**
 * The Shadowsocks profiles that rules can route through.
 */
final class ProxiesTab extends ConfigTab {
    private String selectedId = "";
    private @Nullable ProxyEditor editor;

    ProxiesTab(ScreenHost host) {
        super(host);
    }

    @Override
    Text navLabel() {
        return Text.translatable("routeweaver.tab.proxies");
    }

    @Override
    Text title() {
        return Text.translatable("routeweaver.tab.proxies.title");
    }

    @Override
    Text subtitle() {
        return Text.translatable("routeweaver.tab.proxies.subtitle");
    }

    @Override
    void onHidden() {
        editor = null;
    }

    @Override
    FlowLayout buildList() {
        List<ProxyProfile> visible = host.config().getProfiles().stream()
                .filter(profile -> matchesQuery(profile.getName(), profile.getId(), profile.getHost()))
                .toList();
        if (visible.stream().noneMatch(profile -> profile.getId().equals(selectedId))) {
            selectedId = visible.isEmpty() ? "" : visible.getFirst().getId();
        }

        FlowLayout pane = Widgets.pane(LIST_WIDTH);
        pane.child(Widgets.heading(
                Text.translatable("routeweaver.proxies.list.title"),
                Text.translatable("routeweaver.proxies.list.subtitle")));

        FlowLayout list = Widgets.listContent();
        visible.forEach(profile -> list.child(profileCard(profile)));
        if (visible.isEmpty()) {
            list.child(Widgets.emptyState(
                    Text.translatable("routeweaver.proxies.empty.title"),
                    Text.translatable("routeweaver.proxies.empty.body")));
        }
        pane.child(host.scrollMemory().verticalList("proxies", list));

        ButtonComponent add = Widgets.button(Text.translatable("routeweaver.proxies.add"), () -> {
            editor = ProxyEditor.create(host, this::onEditorClosed);
            host.refresh();
        });
        add.horizontalSizing(Sizing.fixed(96));
        pane.child(add);
        return pane;
    }

    private FlowLayout profileCard(ProxyProfile profile) {
        boolean selected = profile.getId().equals(selectedId);
        FlowLayout card = Widgets.selectableCard(selected, () -> {
            selectedId = profile.getId();
            editor = null;
            host.refresh();
        });
        card.padding(Insets.of(8));
        card.gap(3);
        card.child(Widgets.label(profile.displayName(), selected ? Theme.ACCENT : Theme.TEXT));
        card.child(Widgets.label(profile.getHost() + ":" + profile.getPort(), Theme.MUTED));
        card.child(profile.isEnabled()
                ? Widgets.label(profile.getMethod(), Theme.ACCENT)
                : Widgets.label(Text.translatable("routeweaver.proxies.disabled"), Theme.ERROR));
        return card;
    }

    @Override
    FlowLayout buildDetail() {
        if (editor != null) {
            return editor.build();
        }

        FlowLayout pane = Widgets.pane(Sizing.expand());
        pane.child(Widgets.heading(
                Text.translatable("routeweaver.proxies.detail.title"),
                Text.translatable("routeweaver.proxies.detail.subtitle")));

        RouteWeaverConfig config = host.config();
        ProxyProfile profile = config.findProfile(selectedId);
        if (profile == null) {
            return pane.child(Widgets.emptyState(
                    Text.translatable("routeweaver.proxies.none.title"),
                    Text.translatable("routeweaver.proxies.none.body")));
        }

        pane.child(Widgets.card(Text.translatable("routeweaver.proxies.endpoint"))
                .child(Widgets.title(Text.literal(profile.displayName())))
                .child(Widgets.body(Text.literal(profile.getHost() + ":" + profile.getPort())))
                .child(Widgets.body(Text.translatable("routeweaver.proxies.method", profile.getMethod())))
                .child(profile.isEnabled()
                        ? Widgets.label(Text.translatable("routeweaver.state.enabled"), Theme.SUCCESS)
                        : Widgets.label(Text.translatable("routeweaver.state.disabled"), Theme.ERROR)));

        int rulesUsing = config.countRulesUsing(profile.getId());
        pane.child(Widgets.card(Text.translatable("routeweaver.proxies.usage"))
                .child(Widgets.body(Text.translatable("routeweaver.proxies.usage.rules", rulesUsing)))
                .child(Widgets.body(Text.translatable("routeweaver.proxies.usage.id", profile.getId()))));

        FlowLayout actions = Widgets.card(Text.translatable("routeweaver.proxies.actions"));
        FlowLayout buttons = Widgets.row();
        buttons.child(Widgets.flexButton(Text.translatable("routeweaver.action.edit"), () -> {
            editor = ProxyEditor.edit(host, profile, this::onEditorClosed);
            host.refresh();
        }));
        ButtonComponent delete = Widgets.flexButton(Text.translatable("routeweaver.action.delete"),
                () -> delete(profile));
        delete.active(rulesUsing == 0);
        actions.child(buttons.child(delete));
        if (rulesUsing > 0) {
            actions.child(Widgets.body(Text.translatable("routeweaver.proxies.delete_locked")));
        }
        return pane.child(actions);
    }

    private void onEditorClosed(@Nullable String savedId) {
        editor = null;
        if (savedId != null) {
            selectedId = savedId;
        }
    }

    private void delete(ProxyProfile profile) {
        host.config().getProfiles().remove(profile);
        selectedId = "";
        host.flash(Text.translatable("routeweaver.flash.proxy_deleted", profile.displayName()), false);
        host.saveAndRefresh();
    }
}
