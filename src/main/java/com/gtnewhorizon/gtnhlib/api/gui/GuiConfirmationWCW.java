package com.gtnewhorizon.gtnhlib.api.gui;

import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.resources.I18n;

import cpw.mods.fml.client.GuiConfirmation;
import cpw.mods.fml.common.StartupQuery;

/**
 * The default missing mapping gui but with the yes/no buttons moved up. We need to use this because FML made these guis
 * run at very low fps, which could make the user think the gui isn't registering their clicks and prompt them to click
 * again, and accidentally skip past the missing mapping gui without reading it.
 */
@SuppressWarnings("unused")
public class GuiConfirmationWCW extends GuiConfirmation {

    public GuiConfirmationWCW(StartupQuery query) {
        super(query);
    }

    public void initGui() {
        this.buttonList.add(new GuiOptionButton(0, this.width / 2 - 155, this.height - 58, I18n.format("gui.yes")));
        this.buttonList
                .add(new GuiOptionButton(1, this.width / 2 - 155 + 160, this.height - 58, I18n.format("gui.no")));
    }
}
