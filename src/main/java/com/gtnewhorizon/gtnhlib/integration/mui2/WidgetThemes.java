package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;

public final class WidgetThemes {

    private static final IThemeApi themeApi = IThemeApi.get();

    public static final WidgetThemeKey<WidgetTheme> BACKGROUND_INTERFACE = themeApi
            .widgetThemeKeyBuilder("background_interface", WidgetTheme.class)
            .defaultTheme(new WidgetTheme(0, 0, GuiTextures.PANEL_BACKGROUND, Color.WHITE.main, 0xFF404040, false, 0))
            .defaultHoverTheme(null).register();
}
