package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * One page of the config screen: a list on the left and a detail or editor pane on the right.
 */
abstract class ConfigTab {
    protected static final Sizing LIST_WIDTH = Sizing.fill(40);

    protected final ScreenHost host;
    private String query = "";

    ConfigTab(ScreenHost host) {
        this.host = host;
    }

    abstract Component navLabel();

    abstract Component title();

    abstract Component subtitle();

    abstract FlowLayout buildList();

    abstract FlowLayout buildDetail();

    /** Called when the player switches to another tab; unsaved edits are dropped. */
    void onHidden() {
    }

    final FlowLayout build() {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        row.gap(12);
        row.child(buildList());
        row.child(buildDetail());
        return row;
    }

    final String query() {
        return query;
    }

    final void query(String query) {
        this.query = query;
    }

    /** Whether any of {@code values} contains the search query, ignoring case. */
    protected final boolean matchesQuery(String... values) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
