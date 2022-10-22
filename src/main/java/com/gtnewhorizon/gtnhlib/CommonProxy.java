package com.gtnewhorizon.gtnhlib;

import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.PacketMessageAboveHotbar;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.FakePlayer;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GTNHLib.info("GTNHLib version " + com.gtnewhorizon.gtnhlib.Tags.VERSION + " loaded.");
    }

    public void init(FMLInitializationEvent event) {
        NetworkHandler.init();
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

    public void serverStarted(FMLServerStartedEvent event) {}

    public void serverStopping(FMLServerStoppingEvent event) {}

    public void serverStopped(FMLServerStoppedEvent event) {}

    public void addDebugToChat(String message) {}

    public void addDebugToChat(IChatComponent componentText) {}

    public void addInfoToChat(String message) {}

    public void addInfoToChat(IChatComponent componentText) {}

    public void addWarnToChat(String message) {}

    public void addWarnToChat(IChatComponent componentText) {}

    public void addErrorToChat(String message) {}

    public void addErrorToChat(IChatComponent componentText) {}

    public void addMessageToChat(IChatComponent componentText) {}

    public void printMessageAboveHotbar(String message, int displayDuration, boolean drawShadow, boolean shouldFade) {}

    /**
     * Sends packet from server to client that will display message above hotbar.
     * @see ClientProxy#printMessageAboveHotbar
     */
    public void sendMessageAboveHotbar(
            EntityPlayerMP player,
            IChatComponent chatComponent,
            int displayDuration,
            boolean drawShadow,
            boolean shouldFade) {
        if (player instanceof FakePlayer) return;
        NetworkHandler.instance.sendTo(
                new PacketMessageAboveHotbar(chatComponent, displayDuration, drawShadow, shouldFade), player);
    }
}
