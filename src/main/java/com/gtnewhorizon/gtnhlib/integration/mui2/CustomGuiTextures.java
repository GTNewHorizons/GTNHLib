package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.drawable.UITexture;
import com.gtnewhorizon.gtnhlib.GTNHLib;

public class CustomGuiTextures {

    public static void init() {}

    public static final UITexture TEXT_FIELD_BACKGROUND = UITexture.builder()
            .location(GTNHLib.MODID, "gui/background/text_field_background").imageSize(61, 12).adaptable(1)
            .canApplyTheme().name("text_field_background").build();

    public static final UITexture LIST_BACKGROUND = UITexture.builder()
            .location(GTNHLib.MODID, "gui/background/list_background").canApplyTheme().imageSize(153, 15).adaptable(4)
            .name("list_background").build();
}
