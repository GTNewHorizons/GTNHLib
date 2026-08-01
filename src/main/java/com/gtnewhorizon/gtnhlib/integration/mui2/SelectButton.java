package com.gtnewhorizon.gtnhlib.integration.mui2;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.widgets.ToggleButton;

public class SelectButton extends ToggleButton {

    private static final int UNSELECTED_STATE = 0;

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (getState() == UNSELECTED_STATE) {
            return super.onMousePressed(mouseButton);
        }
        return Result.IGNORE;
    }
}
