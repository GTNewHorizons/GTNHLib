package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.TextWidget;

public class TeamAdminGui implements IGuiHolder<GuiData> {

    public static final SimpleGuiFactory teamAdminGui = new SimpleGuiFactory(
            "gtnhlib:team_admin_gui",
            TeamAdminGui::new);

    @Override
    public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        ModularPanel panel = ModularPanel.defaultPanel("team_admin_gui")
                .child(new TextWidget<>(IKey.str("team admin gui")));

        IPanelHandler teamGuiChildPanel = IPanelHandler.simple(panel, (mainPanel, player) -> {
            ModularPanel teamPanel = new Dialog<>("team_subpanel").setDisablePanelsBelow(true).setDraggable(false);
            return teamPanel;
        }, true);
        panel.child(new ButtonWidget<>().top(7).size(12, 12).leftRel(0.5f).onMouseTapped(mouseButton -> {
            teamGuiChildPanel.openPanel();
            return true;
        }));
        return panel;
    }
}
