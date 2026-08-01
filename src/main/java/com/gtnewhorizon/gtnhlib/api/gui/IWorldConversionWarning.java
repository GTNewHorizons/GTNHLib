package com.gtnewhorizon.gtnhlib.api.gui;

import cpw.mods.fml.common.StartupQuery;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface IWorldConversionWarning {

    /**
     * Will be called right after {@link cpw.mods.fml.common.event.FMLMissingMappingsEvent}s are fired to determine if
     * the warning should be shown.
     *
     * @return True if the warning should be shown
     */
    boolean shouldShow();

    /**
     * Server-side only a simple text message will be printed.
     *
     * @return The text message
     */
    String getServerMessage();

    /**
     * Client-side you can choose to either supply a custom {@link com.gtnewhorizon.gtnhlib.api.gui.GuiConfirmationWCW}
     * or the default one with a text message. To show a default
     * {@link com.gtnewhorizon.gtnhlib.api.gui.GuiConfirmationWCW} return null in {@link #getGui(StartupQuery)}.
     *
     * @return The text message for default gui class
     */
    @SideOnly(Side.CLIENT)
    String getClientMessage();

    /**
     * Children MUST annotate with @SideOnly(Side.CLIENT)
     * <p>
     * Client-side you can choose to either supply a custom {@link com.gtnewhorizon.gtnhlib.api.gui.GuiConfirmationWCW}
     * or the default one with a text message. To show a default
     * {@link com.gtnewhorizon.gtnhlib.api.gui.GuiConfirmationWCW} return null.
     *
     * @return Gui to be used in the warning. null to use default.
     */
    @SideOnly(Side.CLIENT)
    default GuiConfirmationWCW getGui(StartupQuery startupQuery) {
        return null;
    }
}
