package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

public class TeamGui implements IGuiHolder<GuiData> {

    public static final SimpleGuiFactory teamGui = new SimpleGuiFactory("gtnhlib:team_gui", TeamGui::new);

    @Override
    public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        ModularPanel panel = ModularPanel.defaultPanel("team_gui").child(new TextWidget<>(IKey.str("team gui")))
                .child(ButtonWidget.panelCloseButton());
        return panel;
    }
}
