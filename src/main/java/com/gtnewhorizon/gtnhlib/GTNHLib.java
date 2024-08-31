package com.gtnewhorizon.gtnhlib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;

@Mod(
        modid = GTNHLib.MODID,
        version = Tags.VERSION,
        name = GTNHLib.MODNAME,
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "[0.0.13,)",
        guiFactory = "com.gtnewhorizon.gtnhlib.GuiFactory")
public class GTNHLib {

    public static final String MODID = "gtnhlib";
    public static final String MODNAME = "GTNH Lib";
    public static final String GROUPNAME = "com.gtnewhorizon.gtnhlib";
    public static final Logger LOG = LogManager.getLogger(GTNHLib.MODID);

    @SidedProxy(clientSide = GTNHLib.GROUPNAME + ".ClientProxy", serverSide = GTNHLib.GROUPNAME + ".CommonProxy")
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
}
