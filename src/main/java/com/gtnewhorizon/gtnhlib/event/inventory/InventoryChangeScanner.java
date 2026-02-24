package com.gtnewhorizon.gtnhlib.event.inventory;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Centralized scanner for player inventory ownership changes.
 */
public final class InventoryChangeScanner {

    private static final Object2ObjectMap<UUID, PlayerScanState> SERVER_STATES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<UUID, PlayerScanState> CLIENT_STATES = new Object2ObjectOpenHashMap<>();
    private static boolean scannerRequired;

    private InventoryChangeScanner() {}

    /**
     * Opt in to centralized inventory scanning.
     * <p>
     * Mods that consume {@link InventoryChangedEvent} should call this during startup.
     */
    public static void requireScanner() {
        scannerRequired = true;
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!scannerRequired) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player == null) return;

        EntityPlayer player = event.player;
        PlayerScanState state = getState(player);
        if (state == null) {
            state = getOrCreateState(player);
            ensureListenerAttached(player, state);
            initializeBaseline(player, state);
            return;
        }

        ensureListenerAttached(player, state);
        markClientDirtyFallback(player, state);

        if (!state.initialized) {
            initializeBaseline(player, state);
            return;
        }

        if (!state.pendingScan) return;
        state.pendingScan = false;

        GTNHLib.info("Scanning player inventory");
        state.current.clear();
        collectSnapshot(player, state.current);

        state.entered.clear();
        state.left.clear();
        computeDiff(state.baseline, state.current, state.entered, state.left);

        if (consumeInitialClientDeltaSuppression(player, state)) {
            state.swapCurrentAndBaseline();
            return;
        }

        if (!state.entered.isEmpty()) {
            MinecraftForge.EVENT_BUS.post(new InventoryChangedEvent.Entered(player, state.entered));
        }
        if (!state.left.isEmpty()) {
            MinecraftForge.EVENT_BUS.post(new InventoryChangedEvent.Left(player, state.left));
        }

        state.swapCurrentAndBaseline();
    }

    private static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!scannerRequired) return;
        if (!(event.entity instanceof EntityPlayer player)) return;

        PlayerScanState state = getOrCreateState(player);
        armInitialClientDeltaSuppression(player, state);
        ensureListenerAttached(player, state);
        initializeBaseline(player, state);
    }

    private static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        removePlayer(event.player.getUniqueID());
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.original != null) {
            removePlayer(event.original.getUniqueID());
        }
        if (event.entityPlayer != null) {
            removePlayer(event.entityPlayer.getUniqueID());
            if (scannerRequired) {
                PlayerScanState state = getOrCreateState(event.entityPlayer);
                armInitialClientDeltaSuppression(event.entityPlayer, state);
                ensureListenerAttached(event.entityPlayer, state);
                initializeBaseline(event.entityPlayer, state);
            }
        }
    }

    private static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clearStates(CLIENT_STATES);
    }

    private static PlayerScanState getState(EntityPlayer player) {
        boolean clientSide = player.worldObj != null && player.worldObj.isRemote;
        Object2ObjectMap<UUID, PlayerScanState> stateMap = clientSide ? CLIENT_STATES : SERVER_STATES;
        return stateMap.get(player.getUniqueID());
    }

    private static PlayerScanState getOrCreateState(EntityPlayer player) {
        boolean clientSide = player.worldObj != null && player.worldObj.isRemote;
        Object2ObjectMap<UUID, PlayerScanState> stateMap = clientSide ? CLIENT_STATES : SERVER_STATES;
        UUID playerId = player.getUniqueID();
        PlayerScanState state = stateMap.get(playerId);

        if (state == null) {
            state = new PlayerScanState();
            if (clientSide) {
                state.suppressNextClientDelta = true;
            }
            stateMap.put(playerId, state);
        }
        return state;
    }

    private static void ensureListenerAttached(EntityPlayer player, PlayerScanState state) {
        Container container = player.openContainer != null ? player.openContainer : player.inventoryContainer;
        if (container == null) {
            state.detachListener();
            return;
        }

        if (state.listener == null) {
            state.listener = new InventoryChangeListener(state);
        }

        if (state.attachedContainer == container) return;

        state.detachListener();
        container.addCraftingToCrafters(state.listener);
        state.attachedContainer = container;
        // Container switches can coincide with inventory mutations; force one reconciliation scan.
        state.pendingScan = state.initialized;
    }

    private static void initializeBaseline(EntityPlayer player, PlayerScanState state) {
        state.current.clear();
        collectSnapshot(player, state.current);
        state.swapCurrentAndBaseline();
        state.initialized = true;
        state.pendingScan = false;
    }

    private static void markClientDirtyFallback(EntityPlayer player, PlayerScanState state) {
        if (player.worldObj == null || !player.worldObj.isRemote) return;
        if (player.inventory == null) return;
        if (player.inventory.inventoryChanged) {
            state.pendingScan = true;
            // InventoryPlayer.inventoryChanged is sticky in 1.7.10 until manually cleared.
            player.inventory.inventoryChanged = false;
        }
    }

    private static void armInitialClientDeltaSuppression(EntityPlayer player, PlayerScanState state) {
        if (player.worldObj != null && player.worldObj.isRemote) {
            state.suppressNextClientDelta = true;
        }
    }

    private static boolean consumeInitialClientDeltaSuppression(EntityPlayer player, PlayerScanState state) {
        if (player.worldObj == null || !player.worldObj.isRemote) return false;
        if (!state.suppressNextClientDelta) return false;
        state.suppressNextClientDelta = false;
        return true;
    }

    private static void collectSnapshot(EntityPlayer player, Object2IntOpenHashMap<InventoryKey> counts) {
        if (player.inventory != null) {
            addStacks(counts, player.inventory.mainInventory);
            addStacks(counts, player.inventory.armorInventory);
            addStack(counts, player.inventory.getItemStack());
        }

        // Slot 0 is crafting result, 1..4 are player 2x2 crafting input.
        if (player.inventoryContainer instanceof ContainerPlayer container) {
            for (int i = 1; i <= 4; i++) {
                Slot slot = container.getSlot(i);
                if (slot != null) {
                    addStack(counts, slot.getStack());
                }
            }
        }
    }

    private static void addStacks(Object2IntOpenHashMap<InventoryKey> counts, ItemStack[] stacks) {
        if (stacks == null) return;
        for (ItemStack stack : stacks) {
            addStack(counts, stack);
        }
    }

    private static void addStack(Object2IntOpenHashMap<InventoryKey> counts, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        InventoryKey key = InventoryKey.of(stack, GTNHLibConfig.inventoryScannerStrictNBT);
        if (key == null) return;
        counts.addTo(key, stack.stackSize);
    }

    private static void computeDiff(Object2IntOpenHashMap<InventoryKey> previous,
            Object2IntOpenHashMap<InventoryKey> current, Object2IntOpenHashMap<InventoryKey> entered,
            Object2IntOpenHashMap<InventoryKey> left) {
        for (Object2IntMap.Entry<InventoryKey> entry : current.object2IntEntrySet()) {
            InventoryKey key = entry.getKey();
            int currentCount = entry.getIntValue();
            int previousCount = previous.getInt(key);
            int delta = currentCount - previousCount;

            if (delta > 0) {
                entered.put(key, delta);
            } else if (delta < 0) {
                left.put(key, -delta);
            }
        }

        for (Object2IntMap.Entry<InventoryKey> entry : previous.object2IntEntrySet()) {
            InventoryKey key = entry.getKey();
            if (!current.containsKey(key)) {
                left.put(key, entry.getIntValue());
            }
        }
    }

    private static void removePlayer(UUID playerId) {
        detachAndRemove(SERVER_STATES, playerId);
        detachAndRemove(CLIENT_STATES, playerId);
    }

    private static void clearStates(Object2ObjectMap<UUID, PlayerScanState> states) {
        for (PlayerScanState state : states.values()) {
            state.detachListener();
        }
        states.clear();
    }

    private static void detachAndRemove(Object2ObjectMap<UUID, PlayerScanState> states, UUID playerId) {
        PlayerScanState removed = states.remove(playerId);
        if (removed != null) {
            removed.detachListener();
        }
    }

    private static final class PlayerScanState {

        private Object2IntOpenHashMap<InventoryKey> baseline;
        private Object2IntOpenHashMap<InventoryKey> current;
        private final Object2IntOpenHashMap<InventoryKey> entered;
        private final Object2IntOpenHashMap<InventoryKey> left;
        private InventoryChangeListener listener;
        private Container attachedContainer;
        private boolean initialized;
        private boolean pendingScan;
        private boolean suppressNextClientDelta;

        private PlayerScanState() {
            this.baseline = new Object2IntOpenHashMap<>();
            this.current = new Object2IntOpenHashMap<>();
            this.entered = new Object2IntOpenHashMap<>();
            this.left = new Object2IntOpenHashMap<>();
            this.baseline.defaultReturnValue(0);
            this.current.defaultReturnValue(0);
            this.entered.defaultReturnValue(0);
            this.left.defaultReturnValue(0);
        }

        private void detachListener() {
            if (attachedContainer == null || listener == null) return;
            attachedContainer.removeCraftingFromCrafters(listener);
            attachedContainer = null;
        }

        private void swapCurrentAndBaseline() {
            Object2IntOpenHashMap<InventoryKey> tmp = baseline;
            baseline = current;
            current = tmp;
        }
    }

    private static final class InventoryChangeListener implements ICrafting {

        private final PlayerScanState state;

        private InventoryChangeListener(PlayerScanState state) {
            this.state = state;
        }

        @Override
        public void sendContainerAndContentsToPlayer(Container container, List<ItemStack> items) {
            state.pendingScan = true;
        }

        @Override
        public void sendSlotContents(Container container, int slot, ItemStack stack) {
            state.pendingScan = true;
        }

        @Override
        public void sendProgressBarUpdate(Container container, int id, int value) {}
    }

    @EventBusSubscriber
    public static final class EventHandlers {

        private EventHandlers() {}

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            InventoryChangeScanner.onPlayerTick(event);
        }

        @SubscribeEvent
        public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
            InventoryChangeScanner.onEntityJoinWorld(event);
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerLoggedOutEvent event) {
            InventoryChangeScanner.onPlayerLogout(event);
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            InventoryChangeScanner.onPlayerClone(event);
        }

        @SubscribeEvent
        public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
            InventoryChangeScanner.onClientDisconnect(event);
        }
    }
}
