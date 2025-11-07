package com.gtnewhorizon.gtnhlib;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizon.gtnhlib.blockstate.command.BlockStateCommand;
import com.gtnewhorizon.gtnhlib.blockstate.init.BlockPropertyInit;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.block.BlockTest;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.eventbus.AutoEventBus;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.eventbus.Phase;
import com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.PacketMessageAboveHotbar;
import com.gtnewhorizon.gtnhlib.network.PacketViewDistance;

import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@EventBusSubscriber
public class CommonProxy {

    public void construct(FMLConstructionEvent event) {
        AutoEventBus.executePhase(Phase.CONSTRUCT);
    }

    public void preInit(FMLPreInitializationEvent event) {
        AutoEventBus.executePhase(Phase.PRE);
        GTNHLib.info("GTNHLib version " + Tags.VERSION + " loaded.");

        if (GTNHLibConfig.enableTestBlocks) {
            GameRegistry.registerBlock(new BlockTest(), "model_test");
        }

        BlockPropertyInit.init();
    }

    public void init(FMLInitializationEvent event) {
        AutoEventBus.executePhase(Phase.INIT);
        NetworkHandler.init();
        ConfigurationManager.onInit();

        if ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            SyncedKeybind.createConfigurable("gtnhlib.test_keybind", "debug", 0).registerGlobalListener(
                    (p, l, keyDown) -> { GTNHLib.LOG.info("GTNHLib test keybind down: {}", keyDown); });
        }
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new BlockStateCommand());
    }

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
     *
     * @see ClientProxy#printMessageAboveHotbar
     */
    public void sendMessageAboveHotbar(EntityPlayerMP player, IChatComponent chatComponent, int displayDuration,
            boolean drawShadow, boolean shouldFade) {
        if (player instanceof FakePlayer) return;
        NetworkHandler.instance
                .sendTo(new PacketMessageAboveHotbar(chatComponent, displayDuration, drawShadow, shouldFade), player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP playerMP)) return;
        int distance = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
        NetworkHandler.instance.sendTo(new PacketViewDistance(distance), playerMP);
    }
}
