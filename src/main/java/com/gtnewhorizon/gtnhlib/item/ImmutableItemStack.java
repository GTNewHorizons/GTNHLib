package com.gtnewhorizon.gtnhlib.item;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableItemMeta;

/**
 * An immutable version of {@link ItemStack} for situations where ItemStacks should never be modified.
 */
public interface ImmutableItemStack extends ImmutableItemMeta {

    int getStackSize();

    NBTTagCompound getTag();

    default boolean isEmpty() {
        return getStackSize() <= 0;
    }

    default ItemStack toStack() {
        return toStack(getStackSize());
    }

    default ImmutableItemStack copy() {
        return new FastImmutableItemStack(toStack());
    }

    @Override
    default ItemStack toStack(int amount) {
        int meta = getItemMeta();

        ItemStack stack = new ItemStack(getItem(), amount, meta == OreDictionary.WILDCARD_VALUE ? 0 : meta);

        stack.setTagCompound(getTag() == null ? null : (NBTTagCompound) getTag().copy());

        return stack;
    }

    @Override
    default boolean matches(ItemStack stack) {
        if (stack == null) return false;

        if (getItem() != stack.getItem()) return false;
        if (getItemMeta() == OreDictionary.WILDCARD_VALUE) return true;
        if (ItemUtil.getStackMeta(stack) == OreDictionary.WILDCARD_VALUE) return true;
        if (getItemMeta() != ItemUtil.getStackMeta(stack)) return false;

        return Objects.equals(getTag(), stack.getTagCompound());
    }
}
