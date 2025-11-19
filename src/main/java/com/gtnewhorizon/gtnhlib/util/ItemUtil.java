package com.gtnewhorizon.gtnhlib.util;

import java.util.Objects;

import net.minecraft.block.BlockChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.gtnewhorizon.gtnhlib.capability.item.ItemIO;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSink;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSource;
import com.gtnewhorizon.gtnhlib.item.WrappedItemIO;
import com.gtnewhorizon.gtnhlib.item.impl.ItemDuctSink;
import com.gtnewhorizon.gtnhlib.item.impl.mfr.DSUItemIO;
import com.gtnewhorizon.gtnhlib.item.impl.mfr.DSUItemSink;
import com.gtnewhorizon.gtnhlib.item.impl.mfr.DSUItemSource;

import cofh.api.transport.IItemDuct;
import cpw.mods.fml.common.Optional;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class ItemUtil {

    private static int counter = 0;
    public static final int WRAP_INVENTORIES = 0b1 << counter++;
    public static final int FOR_INSERTS = 0b1 << counter++;
    public static final int FOR_EXTRACTS = 0b1 << counter++;
    /// Create [ItemSink]s / [ItemSource]s for [IDeepStorageUnit]s.
    public static final int WRAP_DSUS = 0b1 << counter++;
    /// Create [ItemSink]s for [IItemDuct]s.
    public static final int WRAP_ITEM_DUCTS = 0b1 << counter++;
    public static final int DEFAULT = WRAP_INVENTORIES | FOR_INSERTS | FOR_EXTRACTS | WRAP_DSUS | WRAP_ITEM_DUCTS;

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

    public static ItemSource getItemSource(Object obj, ForgeDirection side) {
        return getItemSource(obj, side, DEFAULT);
    }

    public static ItemSource getItemSource(Object obj, ForgeDirection side,
            @MagicConstant(flagsFromClass = ItemUtil.class) int usage) {
        if ((usage & FOR_EXTRACTS) == 0) return null;

        if (obj instanceof ItemSource source) {
            return source;
        }

        if (obj instanceof CapabilityProvider capabilityProvider) {
            ItemSource source = capabilityProvider.getCapability(ItemSource.class, side);

            if (source != null) return source;
        }

        if ((usage & WRAP_INVENTORIES) != 0) {
            IInventory inv = getInventory(obj, side);

            if (inv != null) return new InventoryItemSource(inv, side);
        }

        if ((usage & WRAP_DSUS) != 0 && GTNHLib.isMFRLoaded) {
            ItemSource source = getDSUSource(obj);

            if (source != null) return source;
        }

        return null;
    }

    public static ItemSink getItemSink(Object obj, ForgeDirection side) {
        return getItemSink(obj, side, DEFAULT);
    }

    public static ItemSink getItemSink(Object obj, ForgeDirection side,
            @MagicConstant(flagsFromClass = ItemUtil.class) int usage) {
        if ((usage & FOR_INSERTS) == 0) return null;

        if (obj instanceof ItemSink sink) {
            return sink;
        }

        if (obj instanceof CapabilityProvider capabilityProvider) {
            ItemSink sink = capabilityProvider.getCapability(ItemSink.class, side);

            if (sink != null) return sink;
        }

        if ((usage & WRAP_INVENTORIES) != 0) {
            IInventory inv = getInventory(obj, side);

            if (inv != null) return new InventoryItemSink(inv, side);
        }

        if ((usage & WRAP_DSUS) != 0 && GTNHLib.isMFRLoaded) {
            ItemSink sink = getDSUSink(obj);

            if (sink != null) return sink;
        }

        if ((usage & WRAP_ITEM_DUCTS) != 0 && GTNHLib.isCoFHCoreLoaded) {
            ItemSink sink = getDuctSink(obj, side);

            if (sink != null) return sink;
        }

        return null;
    }

    public static ItemIO getItemIO(Object obj, ForgeDirection side) {
        return getItemIO(obj, side, DEFAULT);
    }

    /// Gets an [ItemIO] from a generic object that can be used to push or pull items to or from it. The object can be
    /// anything, but it's typically a TileEntity.
    ///
    /// The usage flags are used to indicate the desired usage of the [ItemIO], and control what type of [ItemIO] is
    /// returned, if any. If the usage is only [#FOR_EXTRACTS], the [ItemIO] will only have its [ItemSource] present
    /// and cannot be extracted from. This is useful for accurately polling what kind of capabilities the object has -
    /// if the caller can only extract items, the source is irrelevant and the caller should not receive an [ItemIO]
    /// with only the [ItemSink] present because such an object is useless to the caller and may prevent other fallbacks
    /// from being used.
    public static ItemIO getItemIO(Object obj, ForgeDirection side,
            @MagicConstant(flagsFromClass = ItemUtil.class) int usage) {
        if (obj instanceof ItemIO itemIO) return itemIO;

        if (obj instanceof CapabilityProvider capabilityProvider) {
            ItemIO itemIO = capabilityProvider.getCapability(ItemIO.class, side);

            if (itemIO != null) return itemIO;
        }

        if ((usage & WRAP_DSUS) != 0 && GTNHLib.isMFRLoaded) {
            ItemIO io = getDSUIO(obj);

            if (io != null) return io;
        }

        ItemSource source = getItemSource(obj, side, usage);
        ItemSink sink = getItemSink(obj, side, usage);

        if (source == null && sink == null) return null;

        return new WrappedItemIO(source, sink);
    }

    @Optional.Method(modid = "MineFactoryReloaded")
    private static ItemIO getDSUIO(Object obj) {
        if (obj instanceof IDeepStorageUnit dsu) {
            return new DSUItemIO(dsu);
        } else {
            return null;
        }
    }

    @Optional.Method(modid = "MineFactoryReloaded")
    private static ItemSource getDSUSource(Object obj) {
        if (obj instanceof IDeepStorageUnit dsu) {
            return new DSUItemSource(dsu);
        } else {
            return null;
        }
    }

    @Optional.Method(modid = "MineFactoryReloaded")
    private static ItemSink getDSUSink(Object obj) {
        if (obj instanceof IDeepStorageUnit dsu) {
            return new DSUItemSink(dsu);
        } else {
            return null;
        }
    }

    @Optional.Method(modid = "CoFHCore")
    private static ItemSink getDuctSink(Object obj, ForgeDirection side) {
        if (obj instanceof IItemDuct duct) {
            return new ItemDuctSink(duct, side);
        } else {
            return null;
        }
    }

}
