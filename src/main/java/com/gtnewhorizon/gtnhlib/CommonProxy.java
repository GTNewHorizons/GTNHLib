package com.gtnewhorizon.gtnhlib;

import static com.gtnewhorizon.gtnhlib.core.GTNHLibCore.isObf;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizon.gtnhlib.blockstate.command.BlockStateCommand;
import com.gtnewhorizon.gtnhlib.blockstate.init.BlockPropertyInit;
import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.brigadier.BrigadierCommandWrapper;
import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentEnergy;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentFluid;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentFluidName;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentItemName;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentNumber;
import com.gtnewhorizon.gtnhlib.commands.TitleCommand;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.eventbus.AutoEventBus;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.eventbus.Phase;
import com.gtnewhorizon.gtnhlib.eventhandlers.InventoryEventDebugHandler;
import com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.PacketMessageAboveHotbar;
import com.gtnewhorizon.gtnhlib.network.PacketViewDistance;
import com.gtnewhorizon.gtnhlib.teams.TeamAdminCommand;
import com.gtnewhorizon.gtnhlib.teams.TeamCommand;
import com.gtnewhorizon.gtnhlib.test.block.BlockRngTest;
import com.gtnewhorizon.gtnhlib.test.block.BlockTest;
import com.gtnewhorizon.gtnhlib.test.block.BlockTestLectern;
import com.gtnewhorizon.gtnhlib.test.block.BlockTestTint;
import com.gtnewhorizon.gtnhlib.test.block.BlockTestTintMul;
import com.gtnewhorizon.gtnhlib.test.block.BlockWeightedRngTest;
import com.gtnewhorizon.gtnhlib.test.item.TestItem;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.mojang.brigadier.tree.CommandNode;

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
            BlockTest.register();
            BlockTestLectern.register();
            BlockTestTint.register();
            BlockTestTintMul.register();
            BlockRngTest.register();
            BlockWeightedRngTest.register();
        }

        if (GTNHLibConfig.enableTestItems) {
            GameRegistry.registerItem(TestItem.INSTANCE, "testitem");
        }

        BlockPropertyInit.init();

        ChatComponentCustomRegistry.register(ChatComponentNumber::new);
        ChatComponentCustomRegistry.register(ChatComponentFluid::new);
        ChatComponentCustomRegistry.register(ChatComponentEnergy::new);
        ChatComponentCustomRegistry.register(ChatComponentFluidName::new);
        ChatComponentCustomRegistry.register(ChatComponentItemName::new);

        // Number formatting config registration. Primarily aimed at client side, but does exist on server side
        // as well, just in-case calls are made to number formatting.
        try {
            ConfigurationManager.registerConfig(NumberFormatConfig.class);
            // only register in dev
            if (!isObf()) ConfigurationManager.registerConfig(ExampleConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
        NumberFormatUtil.postConfiguration();
    }

    public void init(FMLInitializationEvent event) {
        AutoEventBus.executePhase(Phase.INIT);
        NetworkHandler.init();
        ConfigurationManager.onInit();

        if ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            SyncedKeybind.createConfigurable("gtnhlib.test_keybind", "debug", 0).registerGlobalListener(
                    (p, l, keyDown) -> { GTNHLib.LOG.info("GTNHLib test keybind down: {}", keyDown); });
        }

        if (GTNHLibConfig.debugInventoryEvents) {
            MinecraftForge.EVENT_BUS.register(new InventoryEventDebugHandler());
        }
    }

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        BrigadierApi.init();
        if (GTNHLibConfig.enableTeamCommands) {
            TeamCommand.register();
            TeamAdminCommand.register();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new BlockStateCommand());
        event.registerServerCommand(new TitleCommand());

        if (isThermosServer()) {
            for (CommandNode<ICommandSender> node : BrigadierApi.getCommandDispatcher().getRoot().getChildren()) {
                event.registerServerCommand(new BrigadierCommandWrapper(node.getName()));
            }
        }
    }

    /**
     * Checks if the current environment is a Thermos server or its fork like Crucible.
     *
     * @return true if the server core is detected, false otherwise.
     */
    public static boolean isThermosServer() {
        try {
            Class.forName("thermos.ThermosRemapper", false, CommonProxy.class.getClassLoader());
            GTNHLib.LOG.info("Thermos detected, applying command wrapper");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void serverStarted(FMLServerStartedEvent event) {}

    public void serverStopping(FMLServerStoppingEvent event) {
        BrigadierApi.clear();
    }

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
