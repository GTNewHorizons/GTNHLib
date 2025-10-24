package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

/**
 * An ImmutableItemStack backed by an ItemStack. This is meant to be allocated once, then modified and returned from an
 * API repeatedly.
 */
public class FastImmutableItemStack implements ImmutableItemStack {

    public ItemStack stack;

    public FastImmutableItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public @NotNull Item item() {
        // noinspection DataFlowIssue
        return stack.getItem();
    }

    @Override
    public int getItemMeta() {
        return Items.feather.getDamage(stack);
    }

    @Override
    public int getStackSize() {
        return stack.stackSize;
    }

    @Override
    public NBTTagCompound getTag() {
        return stack.getTagCompound();
    }
}
