package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.text.Text;
import ru.dverkask.routeweaver.config.ProxyProfile;

import java.util.List;
import java.util.function.Consumer;

/**
 * An inline "pick a proxy profile" dropdown. It owns only its expanded state; the owner keeps
 * the selection and rebuilds the screen through {@code refresh}.
 */
final class ProfileDropdown {
    private static final int VISIBLE_ROWS = 4;
    private static final int ROW_HEIGHT = 26;
    private static final int NAME_LENGTH = 30;

    private final Runnable refresh;
    private boolean expanded;

    ProfileDropdown(Runnable refresh) {
        this.refresh = refresh;
    }

    void collapse() {
        expanded = false;
    }

    FlowLayout build(List<ProxyProfile> profiles, String selectedId, Consumer<String> onSelect) {
        ProxyProfile selected = profiles.stream()
                .filter(profile -> profile.getId().equals(selectedId))
                .findFirst()
                .orElse(null);
        Text label = selected == null
                ? Text.translatable("routeweaver.picker.placeholder")
                : Text.literal(Widgets.ellipsize(selected.displayName(), NAME_LENGTH + 2));

        ButtonComponent toggle = Widgets.wideButton(
                label.copy().append(expanded ? "  ▲" : "  ▼"),
                () -> {
                    expanded = !expanded;
                    refresh.run();
                });
        toggle.renderer(ButtonComponent.Renderer.flat(
                expanded ? Theme.CARD_ACTIVE : Theme.BUTTON, Theme.BUTTON_HOVER, Theme.CARD_DISABLED));

        FlowLayout dropdown = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        dropdown.child(toggle);
        if (!expanded) {
            return dropdown;
        }

        dropdown.surface(Surface.outline(Theme.ACCENT.argb()));
        FlowLayout options = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        options.surface(Surface.flat(Theme.DROPDOWN_LIST).and(Surface.outline(Theme.OUTLINE)));
        options.padding(Insets.of(2, 4, 2, 4));
        for (ProxyProfile profile : profiles) {
            options.child(option(profile, profile.getId().equals(selectedId), onSelect));
        }
        ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(
                Sizing.fill(100), Sizing.fixed(Math.min(profiles.size(), VISIBLE_ROWS) * ROW_HEIGHT), options);
        scroll.surface(Surface.flat(Theme.TRANSPARENT));
        dropdown.child(scroll);
        return dropdown;
    }

    private ButtonComponent option(ProxyProfile profile, boolean selected, Consumer<String> onSelect) {
        String name = Widgets.ellipsize(profile.displayName(), NAME_LENGTH);
        Text label = profile.isEnabled()
                ? Text.literal(name)
                : Text.translatable("routeweaver.picker.off", name);

        ButtonComponent option = Widgets.wideButton(
                label.copy().withColor((selected ? Theme.ACCENT : Theme.TEXT).argb()),
                () -> {
                    expanded = false;
                    onSelect.accept(profile.getId());
                    refresh.run();
                });
        option.renderer(ButtonComponent.Renderer.flat(
                selected ? Theme.CARD_ACTIVE : Theme.TRANSPARENT, Theme.CARD_HOVER, Theme.CARD_DISABLED));
        option.margins(Insets.of(2, 4, 2, 4));
        return option;
    }
}
