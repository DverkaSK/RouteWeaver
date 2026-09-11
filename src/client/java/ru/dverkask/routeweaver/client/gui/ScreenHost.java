package ru.dverkask.routeweaver.client.gui;

import net.minecraft.text.Text;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;

import java.util.List;

/**
 * What {@link RouteWeaverScreen} offers its tabs and editors.
 */
interface ScreenHost {
    RouteWeaverConfig config();

    List<SavedServer> savedServers();

    ScrollMemory scrollMemory();

    /** Persists the config and redraws the screen. */
    void saveAndRefresh();

    /** Redraws everything below the header. */
    void refresh();

    /** Shows a one-line status message above the tab content until the next tab switch. */
    void flash(Text message, boolean error);
}
