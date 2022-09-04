package com.gtnewhorizon.gtnhlib;

import cpw.mods.fml.common.event.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class ClientProxy extends CommonProxy {

    private final Minecraft mc = Minecraft.getMinecraft();

    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        super.serverAboutToStart(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    public void serverStarted(FMLServerStartedEvent event) {
        super.serverStarted(event);
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        super.serverStopping(event);
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        super.serverStopped(event);
    }

    public void addDebugToChat(String message) {
        addDebugToChat(new ChatComponentText(message));
    }

    public void addDebugToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.YELLOW + "[Debug]: ").appendSibling(componentText));
    }

    public void addInfoToChat(String message) {
        addInfoToChat(new ChatComponentText(message));
    }

    public void addInfoToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.GREEN + "[Info]: ").appendSibling(componentText));
    }

    public void addWarnToChat(String message) {
        addWarnToChat(new ChatComponentText(message));
    }

    public void addWarnToChat(IChatComponent componentText) {
        addMessageToChat(
                new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[Warn]: ").appendSibling(componentText));
    }

    public void addErrorToChat(String message) {
        addErrorToChat(new ChatComponentText(message));
    }

    public void addErrorToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.RED + "[Error]: ").appendSibling(componentText));
    }

    public void addMessageToChat(IChatComponent componentText) {
        if (mc.theWorld != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(componentText);
        }
    }
}
