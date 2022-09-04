package com.gtnewhorizon.gtnhlib;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MODID, version = Tags.VERSION, name = Tags.MODNAME, acceptedMinecraftVersions = "[1.7.10]")
public class GTNHLib {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Logger LOG = LogManager.getLogger(Tags.MODID);

    @SidedProxy(clientSide = Tags.GROUPNAME + ".ClientProxy", serverSide = Tags.GROUPNAME + ".CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.serverAboutToStart(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }

    public static void debug(String message) {
        LOG.debug(message);
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void error(String message) {
        LOG.error(message);
    }

    public static void addDebugToChat(String message) {
        addDebugToChat(new ChatComponentText(message));
    }

    public static void addDebugToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.YELLOW + "[Debug]: ").appendSibling(componentText));
    }

    public static void addInfoToChat(String message) {
        addInfoToChat(new ChatComponentText(message));
    }

    public static void addInfoToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.GREEN + "[Info]: ").appendSibling(componentText));
    }

    public static void addWarnToChat(String message) {
        addWarnToChat(new ChatComponentText(message));
    }

    public static void addWarnToChat(IChatComponent componentText) {
        addMessageToChat(
                new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[Warn]: ").appendSibling(componentText));
    }

    public static void addErrorToChat(String message) {
        addErrorToChat(new ChatComponentText(message));
    }

    public static void addErrorToChat(IChatComponent componentText) {
        addMessageToChat(new ChatComponentText(EnumChatFormatting.RED + "[Error]: ").appendSibling(componentText));
    }

    public static void addMessageToChat(IChatComponent componentText) {
        if (mc.theWorld != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(componentText);
        }
    }
}
