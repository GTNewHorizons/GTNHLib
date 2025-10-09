package com.gtnewhorizon.gtnhlib.util;

import java.util.Objects;

import net.minecraft.block.BlockChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Contract;

import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.gtnewhorizon.gtnhlib.capability.item.IItemIO;
import com.gtnewhorizon.gtnhlib.capability.item.IItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.IItemSource;
import com.gtnewhorizon.gtnhlib.capability.item.InventoryItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.InventoryItemSource;
import com.gtnewhorizon.gtnhlib.capability.item.WrappedItemIO;

public class ItemUtil {

    public static int getStackMeta(ItemStack stack) {
        return Items.feather.getDamage(stack);
    }

    public static boolean isStackEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize <= 0;
    }

    public static boolean isStackInvalid(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize < 0;
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2) {
        return areStacksEqual(stack1, stack2, false);
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2, boolean ignoreNBT) {
        return stack1 != null && stack2 != null
                && stack1.getItem() == stack2.getItem()
                && doStackMetasMatch(getStackMeta(stack1), getStackMeta(stack2))
                && (ignoreNBT || Objects.equals(stack1.getTagCompound(), stack2.getTagCompound()));
    }

    public static boolean doStackMetasMatch(int meta1, int meta2) {
        if (meta1 == OreDictionary.WILDCARD_VALUE) return true;
        if (meta2 == OreDictionary.WILDCARD_VALUE) return true;

        return meta1 == meta2;
    }

    @Contract("_, null -> null")
    public static ItemStack copyAmount(int amount, ItemStack stack) {
        if (isStackInvalid(stack)) return null;

        stack = stack.copy();
        stack.stackSize = amount;

        return stack;
    }

    public static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    public static IInventory getChestInventory(TileEntityChest chest) {
        if (!(chest.getBlockType() instanceof BlockChest blockChest)) return null;

        IInventory inv = blockChest.func_149951_m(chest.getWorldObj(), chest.xCoord, chest.yCoord, chest.zCoord);

        return inv != null ? inv : chest;
    }

    public static IInventory getInventory(Object obj, ForgeDirection side) {
        if (obj instanceof TileEntityChest chest) {
            return getChestInventory(chest);
        } else if (obj instanceof IInventory inv) {
            return inv;
        } else if (obj instanceof CapabilityProvider capabilityProvider) {
            return capabilityProvider.getCapability(IInventory.class, side);
        } else {
            return null;
        }
    }

    public static IItemSource getItemSource(Object obj, ForgeDirection side, boolean wrapInventories) {
        if (obj instanceof IItemSource source) {
            return source;
        }

        if (obj instanceof CapabilityProvider capabilityProvider) {
            IItemSource source = capabilityProvider.getCapability(IItemSource.class, side);

            if (source != null) return source;
        }

        if (wrapInventories) {
            IInventory inv = getInventory(obj, side);

            if (inv != null) return new InventoryItemSource(inv, side);
        }

        return null;
    }

    public static IItemSink getItemSink(Object obj, ForgeDirection side, boolean wrapInventories) {
        if (obj instanceof IItemSink sink) {
            return sink;
        }

        if (obj instanceof CapabilityProvider capabilityProvider) {
            IItemSink sink = capabilityProvider.getCapability(IItemSink.class, side);

            if (sink != null) return sink;
        }

        if (wrapInventories) {
            IInventory inv = getInventory(obj, side);

            if (inv != null) return new InventoryItemSink(inv, side);
        }

        return null;
    }

    public static IItemIO getItemIO(Object obj, ForgeDirection side, boolean wrapInventories) {
        if (obj instanceof IItemIO itemIO) return itemIO;

        if (obj instanceof CapabilityProvider capabilityProvider) {
            IItemIO itemIO = capabilityProvider.getCapability(IItemIO.class, side);

            if (itemIO != null) return itemIO;
        }

        IItemSource source = getItemSource(obj, side, wrapInventories);
        IItemSink sink = getItemSink(obj, side, wrapInventories);

        if (source == null && sink == null) return null;

        return new WrappedItemIO(source, sink);
    }
}
