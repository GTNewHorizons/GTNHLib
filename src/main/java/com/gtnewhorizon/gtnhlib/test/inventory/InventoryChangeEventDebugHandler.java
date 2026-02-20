package com.gtnewhorizon.gtnhlib.test.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.event.inventory.InventoryChangeScanner;
import com.gtnewhorizon.gtnhlib.event.inventory.InventoryChangedEvent;
import com.gtnewhorizon.gtnhlib.event.inventory.InventoryKey;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

@EventBusSubscriber
public final class InventoryChangeEventDebugHandler {

    private static final String PREFIX = "[InventoryScannerTest]";
    private static final String CHAT_PREFIX = EnumChatFormatting.AQUA + PREFIX + EnumChatFormatting.RESET + " ";

    private InventoryChangeEventDebugHandler() {}

    @EventBusSubscriber.Condition
    public static boolean shouldRegister() {
        if (Launch.blackboard == null) {
            return false;
        }
        Object value = Launch.blackboard.get("fml.deobfuscatedEnvironment");
        boolean shouldRegister = Boolean.TRUE.equals(value);
        if (shouldRegister) {
            InventoryChangeScanner.requireScanner();
        }
        return shouldRegister;
    }

    @SubscribeEvent
    public static void onEntered(InventoryChangedEvent.Entered event) {
        logChange("ENTERED", event.getPlayer(), event.getChanges());
    }

    @SubscribeEvent
    public static void onLeft(InventoryChangedEvent.Left event) {
        logChange("LEFT", event.getPlayer(), event.getChanges());
    }

    private static void logChange(String direction, EntityPlayer player, Object2IntMap<InventoryKey> changes) {
        int totalDelta = 0;
        for (Object2IntMap.Entry<InventoryKey> entry : changes.object2IntEntrySet()) {
            totalDelta += entry.getIntValue();
        }

        String side = player.worldObj != null && player.worldObj.isRemote ? "CLIENT" : "SERVER";
        GTNHLib.LOG.info(
                "{} side={} player={} direction={} keys={} totalDelta={}",
                PREFIX,
                side,
                player.getCommandSenderName(),
                direction,
                changes.size(),
                totalDelta);

        player.addChatMessage(
                new ChatComponentText(
                        CHAT_PREFIX + "side="
                                + side
                                + " direction="
                                + direction
                                + " keys="
                                + changes.size()
                                + " totalDelta="
                                + totalDelta));
    }
}
