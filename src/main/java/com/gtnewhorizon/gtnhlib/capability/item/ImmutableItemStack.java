package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.util.data.ImmutableItemMeta;

/**
 * An immutable version of {@link ItemStack} for situations where ItemStacks should never be modified.
 */
public interface ImmutableItemStack extends ImmutableItemMeta {

    int getStackSize();

    NBTTagCompound getTag();

    default ItemStack toStack() {
        return toStack(getStackSize());
    }

    default ImmutableItemStack copy() {
        return new FastImmutableItemStack(toStack());
    }

    @Override
    default ItemStack toStack(int amount) {
        int meta = getItemMeta();

        ItemStack stack = new ItemStack(item(), amount, meta == OreDictionary.WILDCARD_VALUE ? 0 : meta);

        stack.setTagCompound(getTag() == null ? null : (NBTTagCompound) getTag().copy());

        return stack;
    }
}
