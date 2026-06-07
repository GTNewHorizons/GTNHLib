package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;

public class CustomWidgetTheme {

    public static void init() {}

    private static final IThemeApi themeApi = IThemeApi.get();

    public static final WidgetThemeKey<TextFieldTheme> BACKGROUND_TEXT_FIELD = themeApi
            .widgetThemeKeyBuilder("text_field_theme", TextFieldTheme.class)
            .defaultTheme(
                    new TextFieldTheme(
                            0,
                            0,
                            CustomGuiTextures.TEXT_FIELD_BACKGROUND,
                            Color.WHITE.main,
                            0xFF404040,
                            false,
                            0,
                            0,
                            0xFF404040))
            .defaultHoverTheme(null).register();
}
