package ru.dverkask.routeweaver.client.gui;

import io.wispforest.owo.ui.core.Color;

/**
 * The RouteWeaver palette, shared by the config screen and the toast.
 */
public final class Theme {
    public static final Color ACCENT = Color.ofArgb(0xFFC9F24A);
    public static final Color ACCENT_SOFT = Color.ofArgb(0xFF9BBE3D);
    public static final Color TEXT = Color.ofArgb(0xFFF5F5F2);
    public static final Color MUTED = Color.ofArgb(0xFF999B99);
    public static final Color SUCCESS = Color.ofArgb(0xFF7BDF91);
    public static final Color ERROR = Color.ofArgb(0xFFFF8E8E);

    public static final int BACKDROP = 0xA008090B;
    public static final int PANEL = 0xE6111214;
    public static final int PANEL_ALT = 0xCC17191D;
    public static final int CARD = 0xB816181C;
    public static final int CARD_HOVER = 0xD7202328;
    public static final int CARD_ACTIVE = 0xFF272B31;
    public static final int CARD_DISABLED = 0x88202024;
    public static final int OUTLINE = 0xFF2A2F31;
    public static final int INPUT = 0xFF0E1012;
    public static final int FLASH = 0x8F161A18;
    public static final int DROPDOWN_LIST = 0x22000000;
    public static final int TRANSPARENT = 0x00000000;

    public static final int BUTTON = 0xFF21252B;
    public static final int BUTTON_HOVER = 0xFF30363E;
    public static final int TOGGLE_ON = 0xFF1A2E1A;
    public static final int TOGGLE_ON_HOVER = 0xFF203520;
    public static final int TOGGLE_OFF = 0xFF2E1A1A;
    public static final int TOGGLE_OFF_HOVER = 0xFF352020;
    public static final int DANGER = 0xFF2E1A1A;
    public static final int DANGER_HOVER = 0xFF3D2020;

    private Theme() {
    }
}
