package com.gtnewhorizon.gtnhlib;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;
import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;
import cpw.mods.fml.common.event.*;

@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MinecraftForge.EVENT_BUS.register(new AnimatedTooltipHandler());
    }

    @Override
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        super.serverAboutToStart(event);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
        super.serverStarted(event);
    }

    @Override
    public void serverStopping(FMLServerStoppingEvent event) {
        super.serverStopping(event);
    }

    @Override
    public void serverStopped(FMLServerStoppedEvent event) {
        super.serverStopped(event);
    }

    @Override
    public void addDebugToChat(String message) {
        addDebugToChat(new ChatComponentText(message));
    }

    @Override
    public void addDebugToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.YELLOW + "[Debug]: ").appendSibling(componentText));
    }

    @Override
    public void addInfoToChat(String message) {
        addInfoToChat(new ChatComponentText(message));
    }

    @Override
    public void addInfoToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.GREEN + "[Info]: ").appendSibling(componentText));
    }

    @Override
    public void addWarnToChat(String message) {
        addWarnToChat(new ChatComponentText(message));
    }

    @Override
    public void addWarnToChat(IChatComponent componentText) {
        addMessageToChat(
                new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[Warn]: ").appendSibling(componentText));
    }

    @Override
    public void addErrorToChat(String message) {
        addErrorToChat(new ChatComponentText(message));
    }

    @Override
    public void addErrorToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.RED + "[Error]: ").appendSibling(componentText));
    }

    @Override
    public void addMessageToChat(IChatComponent componentText) {
        if (mc.theWorld != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(componentText);
        }
    }

    /**
     * Prints a message above the hotbar
     *
     * @param message         Color it with EnumChatFormatting
     * @param displayDuration in ticks
     * @param drawShadow      Should the message be drawn with a drawShadow
     * @param shouldFade      Should the message fade away with time
     */
    @Override
    public void printMessageAboveHotbar(String message, int displayDuration, boolean drawShadow, boolean shouldFade) {
        AboveHotbarHUD.renderTextAboveHotbar(message, displayDuration, drawShadow, shouldFade);
    }
}
