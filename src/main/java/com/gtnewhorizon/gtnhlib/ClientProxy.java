package com.gtnewhorizon.gtnhlib;

import static com.gtnewhorizon.gtnhlib.GTNHLib.MODID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.tooltip.LoreHandler;
import com.gtnewhorizon.gtnhlib.commands.ItemInHandCommand;
import com.gtnewhorizon.gtnhlib.compat.FalseTweaks;
import com.gtnewhorizon.gtnhlib.compat.Mods;
import com.gtnewhorizon.gtnhlib.compat.NotEnoughItemsVersionChecker;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import lombok.Getter;

@SuppressWarnings("unused")
@EventBusSubscriber(side = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    private static boolean modelsBaked = false;
    public static boolean doThreadSafetyChecks = true;
    @Getter
    public static int currentServerViewDistance = 12;
    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        RenderingRegistry.registerBlockHandler(new ModelISBRH());

        // External model loader handlers. For the low low price of calling this method (and having your jar scanned),
        // you too can automatically load textures for your models!
        ModelRegistry.registerModid(MODID);
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
        if (Mods.NEI) {
            FMLCommonHandler.instance().bus().register(new NotEnoughItemsVersionChecker());
        }

        LoreHandler.postInit();

        // Internal model loader handlers
        final var resourceManager = ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager());
        resourceManager.registerReloadListener(new ModelRegistry.ReloadListener());
        MinecraftForge.EVENT_BUS.register(new ModelRegistry.EventHandler());
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
