package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps list scroll positions across rebuilds. The screen rebuilds its content on every change,
 * which would otherwise throw each list back to the top.
 */
final class ScrollMemory {
    private final Map<String, Double> offsets = new HashMap<>();
    private final Map<String, KeptScrollContainer> live = new HashMap<>();

    /**
     * A vertical scroll list that resumes where the previous list under {@code key} was.
     */
    ScrollContainer<FlowLayout> verticalList(String key, FlowLayout content) {
        KeptScrollContainer scroll = new KeptScrollContainer(content, offsets.getOrDefault(key, 0.0));
        scroll.surface(Surface.flat(Theme.TRANSPARENT));
        live.put(key, scroll);
        return scroll;
    }

    /**
     * Records the positions of the current lists; call right before discarding them.
     */
    void capture() {
        live.forEach((key, scroll) -> offsets.put(key, scroll.offset()));
        live.clear();
    }

    private static final class KeptScrollContainer extends ScrollContainer<FlowLayout> {
        KeptScrollContainer(FlowLayout content, double offset) {
            super(ScrollDirection.VERTICAL, Sizing.fill(100), Sizing.expand(), content);
            // Setting both skips the slide-in animation; layout() clamps to the new content size.
            this.scrollOffset = offset;
            this.currentScrollPosition = offset;
        }

        double offset() {
            return scrollOffset;
        }
    }
}
