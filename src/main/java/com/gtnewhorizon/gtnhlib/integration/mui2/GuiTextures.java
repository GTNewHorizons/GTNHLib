package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.drawable.UITexture;
import com.gtnewhorizon.gtnhlib.GTNHLib;

public final class GuiTextures {

    public static final UITexture PANEL_BACKGROUND = UITexture.builder()
            .location(GTNHLib.MODID, "gui/background/panel_background").imageSize(50, 50).adaptable(4).canApplyTheme()
            .name("panel_background").build();
}
