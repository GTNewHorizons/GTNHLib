package com.gtnewhorizon.gtnhlib.event.inventory;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import lombok.Getter;

/**
 * Immutable item identity used by the centralized inventory scanner.
 */
public final class InventoryKey {

    @Getter
    private final long packedItemMeta;
    @Getter
    private final int nbtSignature;
    private final @Nullable NBTTagCompound nbt;

    private InventoryKey(long packedItemMeta, int nbtSignature, @Nullable NBTTagCompound nbt) {
        this.packedItemMeta = packedItemMeta;
        this.nbtSignature = nbtSignature;
        this.nbt = nbt;
    }

    public static @Nullable InventoryKey of(@Nullable ItemStack stack, boolean strictNBT) {
        if (stack == null || stack.stackSize <= 0 || stack.getItem() == null) {
            return null;
        }

        int itemId = Item.getIdFromItem(stack.getItem());
        if (itemId < 0) {
            return null;
        }

        int meta = ItemUtil.getStackMeta(stack);
        long packedItemMeta = packItemMeta(itemId, meta);

        if (!strictNBT) {
            return new InventoryKey(packedItemMeta, 0, null);
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return new InventoryKey(packedItemMeta, 0, null);
        }

        NBTTagCompound tagCopy = (NBTTagCompound) tag.copy();
        return new InventoryKey(packedItemMeta, tagCopy.hashCode(), tagCopy);
    }

    public int getItemId() {
        return unpackItemId(packedItemMeta);
    }

    public int getMeta() {
        return unpackMeta(packedItemMeta);
    }

    public @Nullable NBTTagCompound getTagCopy() {
        return nbt == null ? null : (NBTTagCompound) nbt.copy();
    }

    public ItemStack toStack(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        Item item = Item.getItemById(getItemId());
        if (item == null) {
            throw new IllegalStateException("No item found for id " + getItemId());
        }

        ItemStack stack = new ItemStack(item, amount, getMeta());
        if (nbt != null) {
            stack.setTagCompound((NBTTagCompound) nbt.copy());
        }
        return stack;
    }

    private static long packItemMeta(int itemId, int meta) {
        return ((long) itemId << 32) | (meta & 0xFFFF_FFFFL);
    }

    private static int unpackItemId(long packed) {
        return (int) (packed >>> 32);
    }

    private static int unpackMeta(long packed) {
        return (int) packed;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof InventoryKey other)) return false;
        if (packedItemMeta != other.packedItemMeta) return false;
        if (nbtSignature != other.nbtSignature) return false;
        return Objects.equals(nbt, other.nbt);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(packedItemMeta);
        result = 31 * result + nbtSignature;
        return result;
    }

    @Override
    public String toString() {
        return "InventoryKey{itemId=" + getItemId() + ", meta=" + getMeta() + ", nbtSignature=" + nbtSignature + "}";
    }
}
