package com.gtnewhorizon.gtnhlib;

import static com.gtnewhorizon.gtnhlib.client.model.ModelLoader.shouldLoadModels;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;

import com.gtnewhorizon.gtnhlib.client.model.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.tooltip.LoreHandler;
import com.gtnewhorizon.gtnhlib.commands.ItemInHandCommand;
import com.gtnewhorizon.gtnhlib.compat.FalseTweaks;
import com.gtnewhorizon.gtnhlib.compat.Mods;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;

import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

@SuppressWarnings("unused")
@EventBusSubscriber(side = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    private static boolean modelsBaked = false;
    public static boolean doThreadSafetyChecks = true;
    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientCommandHandler.instance.registerCommand(new ItemInHandCommand());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        if (Mods.FALSETWEAKS) {
            doThreadSafetyChecks = FalseTweaks.doTessSafetyChecks();
            if (!doThreadSafetyChecks) {
                GTNHLib.info("FalseTweaks threaded rendering is enabled - disabling GTNHLib's thread safety checks");
            }
        }

        if (shouldLoadModels()) {
            Minecraft.getMinecraft().refreshResources();
            ModelLoader.loadModels();
        }

        LoreHandler.postInit();
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

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!modelsBaked) {
            ModelLoader.bakeModels();
            modelsBaked = true;
        }
    }
}
