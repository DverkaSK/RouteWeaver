package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Factory methods for the building blocks every tab is made of.
 */
final class Widgets {
    private static final int BODY_MAX_WIDTH = 420;
    private static final int FIELD_MAX_LENGTH = 512;

    private Widgets() {
    }

    // ─── Labels ─────────────────────────────────────────────────────────────

    static LabelComponent title(Component text) {
        return label(text, Theme.TEXT);
    }

    /** Accent-coloured caption above a card or field. */
    static LabelComponent caption(Component text) {
        return label(text, Theme.ACCENT);
    }

    static LabelComponent body(Component text) {
        return label(text, Theme.MUTED).maxWidth(BODY_MAX_WIDTH);
    }

    static LabelComponent label(Component text, Color color) {
        return UIComponents.label(text).color(color).shadow(false);
    }

    static LabelComponent label(String literal, Color color) {
        return label(Component.literal(literal), color);
    }

    // ─── Containers ─────────────────────────────────────────────────────────

    /** One column of a tab: a full-height panel. */
    static FlowLayout pane(Sizing width) {
        FlowLayout pane = UIContainers.verticalFlow(width, Sizing.fill(100));
        pane.surface(Surface.flat(Theme.PANEL_ALT).and(Surface.outline(Theme.OUTLINE)));
        pane.padding(Insets.of(12));
        pane.gap(10);
        return pane;
    }

    static FlowLayout card(Component caption) {
        FlowLayout card = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.flat(Theme.CARD).and(Surface.outline(Theme.OUTLINE)));
        card.padding(Insets.of(10));
        card.gap(5);
        card.child(caption(caption));
        return card;
    }

    static FlowLayout heading(Component title, Component subtitle) {
        FlowLayout heading = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        heading.gap(2);
        heading.child(title(title));
        heading.child(body(subtitle));
        return heading;
    }

    static FlowLayout emptyState(Component title, Component body) {
        return card(title).child(body(body));
    }

    /** The column of cards inside a scrollable list; the right inset keeps clear of the scrollbar. */
    static FlowLayout listContent() {
        FlowLayout list = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        list.gap(6);
        list.padding(Insets.right(8));
        return list;
    }

    static FlowLayout row() {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(8);
        return row;
    }

    /** A list entry that highlights when selected and reacts to a left click. */
    static FlowLayout selectableCard(boolean selected, Runnable onClick) {
        FlowLayout card = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        card.surface(Surface.flat(selected ? Theme.CARD_ACTIVE : Theme.CARD)
                .and(Surface.outline(selected ? Theme.ACCENT.argb() : Theme.OUTLINE)));
        card.mouseDown().subscribe((click, doubled) -> {
            if (click.button() != 0) {
                return false;
            }
            onClick.run();
            return true;
        });
        return card;
    }

    // ─── Buttons ────────────────────────────────────────────────────────────

    static ButtonComponent button(Component text, Runnable action) {
        ButtonComponent button = UIComponents.button(text, pressed -> action.run());
        button.textShadow(false);
        button.renderer(ButtonComponent.Renderer.flat(Theme.BUTTON, Theme.BUTTON_HOVER, Theme.CARD_DISABLED));
        return button;
    }

    /** A button that shares a row's free width with its siblings. */
    static ButtonComponent flexButton(Component text, Runnable action) {
        ButtonComponent button = button(text, action);
        button.horizontalSizing(Sizing.expand());
        return button;
    }

    static ButtonComponent wideButton(Component text, Runnable action) {
        ButtonComponent button = button(text, action);
        button.horizontalSizing(Sizing.fill(100));
        return button;
    }

    static ButtonComponent dangerButton(Component text, Runnable action) {
        ButtonComponent button = wideButton(text, action);
        button.renderer(ButtonComponent.Renderer.flat(Theme.DANGER, Theme.DANGER_HOVER, Theme.CARD_DISABLED));
        return button;
    }

    /** A full-width on/off switch tinted green or red. */
    static ButtonComponent toggle(boolean on, Component onText, Component offText, Runnable action) {
        ButtonComponent button = wideButton(on ? onText : offText, action);
        button.renderer(ButtonComponent.Renderer.flat(
                on ? Theme.TOGGLE_ON : Theme.TOGGLE_OFF,
                on ? Theme.TOGGLE_ON_HOVER : Theme.TOGGLE_OFF_HOVER,
                Theme.CARD_DISABLED));
        return button;
    }

    static ButtonComponent enabledToggle(boolean enabled, Runnable action) {
        return toggle(enabled,
                Component.translatable("routeweaver.state.enabled"),
                Component.translatable("routeweaver.state.disabled"),
                action);
    }

    // ─── Inputs ─────────────────────────────────────────────────────────────

    /** A text box on the dark input surface instead of vanilla's white frame. */
    static FlowLayout input(TextBoxComponent box) {
        box.setBordered(false);
        box.horizontalSizing(Sizing.expand());
        FlowLayout wrapper = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        wrapper.surface(Surface.flat(Theme.INPUT).and(Surface.outline(Theme.OUTLINE)));
        wrapper.padding(Insets.of(4, 6, 4, 6));
        wrapper.verticalAlignment(VerticalAlignment.CENTER);
        wrapper.child(box);
        return wrapper;
    }

    static TextBoxComponent textBox(String value, int maxLength, Consumer<String> onChange) {
        TextBoxComponent box = UIComponents.textBox(Sizing.fill(100), value);
        box.setMaxLength(maxLength);
        box.onChanged().subscribe(onChange::accept);
        return box;
    }

    /** A captioned text field. */
    static FlowLayout field(Component caption, String value, Consumer<String> onChange) {
        FlowLayout field = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        field.gap(3);
        field.child(caption(caption));
        field.child(input(textBox(value, FIELD_MAX_LENGTH, onChange)));
        return field;
    }

    /** A captioned button, laid out like {@link #field} so the two line up in a row. */
    static FlowLayout buttonField(Component caption, ButtonComponent button) {
        FlowLayout field = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        field.gap(3);
        field.child(caption(caption));
        button.horizontalSizing(Sizing.fill(100));
        field.child(button);
        return field;
    }

    // ─── Text helpers ───────────────────────────────────────────────────────

    static String ellipsize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
