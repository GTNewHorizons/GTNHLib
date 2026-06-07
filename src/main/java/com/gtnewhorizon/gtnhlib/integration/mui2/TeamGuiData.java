package com.gtnewhorizon.gtnhlib.integration.mui2;

import net.minecraft.entity.player.EntityPlayer;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.factory.GuiData;

public class TeamGuiData extends GuiData {

    public GuiView currentView;
    public boolean forceRefreshWithNextUpdate;

    public TeamGuiData(@NotNull EntityPlayer player, GuiView currentView) {
        super(player);
        this.currentView = currentView;
        this.forceRefreshWithNextUpdate = false;
    }
}
