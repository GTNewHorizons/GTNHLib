package com.gtnewhorizon.gtnhlib.integration.mui2;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

public class FreezablePanel extends ModularPanel {

    // stop updates when shiftHeld
    public boolean shiftHeld = false;

    private final TeamGui gui;
    private final PanelSyncManager syncManager;
    private int ticksSinceRefresh = 0;

    public FreezablePanel(@NotNull String name, TeamGui gui, PanelSyncManager syncManager) {
        super(name);
        this.size(176, 166);

        this.gui = gui;
        this.syncManager = syncManager;
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT) {
            shiftHeld = true;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            gui.restoreView(this.syncManager);
        }

        return super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public boolean onKeyRelease(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT) {
            shiftHeld = false;
        }

        TeamGui.forceRefresh = true;
        return super.onKeyRelease(typedChar, keyCode);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (TeamGui.forceRefresh || ticksSinceRefresh > 20) {
            TeamGui.updateDisplayList(TeamGui.forceRefresh || !shiftHeld);
            TeamGui.forceRefresh = false;
            ticksSinceRefresh = 0;
            return;
        }
        ticksSinceRefresh++;
    }
}
