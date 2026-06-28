package com.gtnewhorizon.gtnhlib.eventhandlers;

import net.minecraftforge.client.event.GuiOpenEvent;

import com.gtnewhorizon.gtnhlib.api.gui.GuiConfirmationWCW;
import com.gtnewhorizon.gtnhlib.api.gui.WorldConversionWarningManager;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.mixins.early.AccessorGuiNotification;
import com.gtnewhorizon.gtnhlib.mixins.early.AccessorStartupQuery;

import cpw.mods.fml.client.GuiConfirmation;
import cpw.mods.fml.common.StartupQuery;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@EventBusSubscriber
public class ClientEventHandler {

    // Put here because WorldConversionWarningManager is common and cannot try to import gui classes
    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiConfirmation confirmationGui)) return;

        StartupQuery query = ((AccessorGuiNotification) confirmationGui).gtnhlib$getQuery();

        WorldConversionWarningManager.WARNINGS.forEach((id, wcw) -> {
            if (query.getText().equals(id)) {
                ((AccessorStartupQuery) query).gtnhlib$setText(wcw.getClientMessage());
                GuiConfirmationWCW gui = wcw.getGui(query);

                if (gui != null) {
                    event.gui = gui;
                } else {
                    event.gui = new GuiConfirmationWCW(query);
                }
            }
        });
    }
}
