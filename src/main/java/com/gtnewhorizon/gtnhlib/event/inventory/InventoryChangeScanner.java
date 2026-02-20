package com.gtnewhorizon.gtnhlib.event.inventory;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Centralized scanner for player inventory ownership changes.
 */
@EventBusSubscriber
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!scannerRequired) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player == null) return;

        EntityPlayer player = event.player;
        boolean clientSide = player.worldObj != null && player.worldObj.isRemote;
        Object2ObjectMap<UUID, PlayerScanState> stateMap = clientSide ? CLIENT_STATES : SERVER_STATES;
        UUID playerId = player.getUniqueID();

        PlayerScanState state = stateMap.get(playerId);
        if (state == null) {
            state = new PlayerScanState();
            stateMap.put(playerId, state);
        }

        state.current.clear();
        collectSnapshot(player, state.current);

        if (!state.initialized) {
            state.initialized = true;
            state.swapCurrentAndBaseline();
            return;
        }

        state.entered.clear();
        state.left.clear();
        computeDiff(state.baseline, state.current, state.entered, state.left);

        if (!state.entered.isEmpty()) {
            MinecraftForge.EVENT_BUS.post(new InventoryChangedEvent.Entered(player, state.entered));
        }
        if (!state.left.isEmpty()) {
            MinecraftForge.EVENT_BUS.post(new InventoryChangedEvent.Left(player, state.left));
        }

        state.swapCurrentAndBaseline();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        removePlayer(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.original != null) {
            removePlayer(event.original.getUniqueID());
        }
        if (event.entityPlayer != null) {
            removePlayer(event.entityPlayer.getUniqueID());
        }
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
        SERVER_STATES.remove(playerId);
        CLIENT_STATES.remove(playerId);
    }

    private static final class PlayerScanState {

        private Object2IntOpenHashMap<InventoryKey> baseline;
        private Object2IntOpenHashMap<InventoryKey> current;
        private final Object2IntOpenHashMap<InventoryKey> entered;
        private final Object2IntOpenHashMap<InventoryKey> left;
        private boolean initialized;

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

        private void swapCurrentAndBaseline() {
            Object2IntOpenHashMap<InventoryKey> tmp = baseline;
            baseline = current;
            current = tmp;
        }
    }
}
