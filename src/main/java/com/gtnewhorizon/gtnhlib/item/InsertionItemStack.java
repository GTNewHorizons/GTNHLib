package com.gtnewhorizon.gtnhlib.item;

import java.util.Objects;

import javax.annotation.Nonnegative;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;

/// A [ImmutableItemStack] implementation that's meant to be passed to the various insert methods.
/// @see InventoryIterator#insert(ImmutableItemStack, boolean)
/// @see com.gtnewhorizon.gtnhlib.capability.item.ItemSink#store(ImmutableItemStack)
public class InsertionItemStack implements ImmutableItemStack {

    private boolean useImmutable = false;
    private ItemStack stack;
    private ImmutableItemStack immutable;
    @Nonnegative
    private int amountToInsert;

    /// Must be initialized later
    public InsertionItemStack() {

    }

    public InsertionItemStack(@NotNull ItemStack stack) {
        set(stack, stack.stackSize);
    }

    public InsertionItemStack(@NotNull ItemStack stack, @Nonnegative int amountToInsert) {
        set(stack, amountToInsert);
    }

    public InsertionItemStack(@NotNull ImmutableItemStack stack) {
        set(stack, stack.getStackSize());
    }

    public InsertionItemStack(@NotNull ImmutableItemStack stack, @Nonnegative int amountToInsert) {
        set(stack, amountToInsert);
    }

    public InsertionItemStack set(ItemStack stack) {
        this.useImmutable = false;
        this.stack = Objects.requireNonNull(stack, "stack cannot be null");
        this.amountToInsert = Math.max(0, stack.stackSize);
        return this;
    }

    public InsertionItemStack set(ItemStack stack, int amount) {
        this.useImmutable = false;
        this.stack = Objects.requireNonNull(stack, "stack cannot be null");
        this.amountToInsert = Math.max(0, amount);
        return this;
    }

    public InsertionItemStack set(ImmutableItemStack stack) {
        this.useImmutable = true;
        this.immutable = Objects.requireNonNull(stack, "stack cannot be null");
        this.amountToInsert = Math.max(0, stack.getStackSize());
        return this;
    }

    public InsertionItemStack set(ImmutableItemStack stack, int amount) {
        this.useImmutable = true;
        this.immutable = Objects.requireNonNull(stack, "stack cannot be null");
        this.amountToInsert = Math.max(0, amount);
        return this;
    }

    public InsertionItemStack set(int amount) {
        this.amountToInsert = Math.max(0, amount);
        return this;
    }

    public InsertionItemStack decrement(int amount) {
        this.amountToInsert = Math.max(0, amountToInsert - amount);
        return this;
    }

    @Override
    public int getStackSize() {
        return amountToInsert;
    }

    @Override
    public @NotNull Item getItem() {
        return Objects.requireNonNull(useImmutable ? immutable.getItem() : stack.getItem(), "item cannot be null");
    }

    @Override
    public int getItemMeta() {
        return useImmutable ? immutable.getItemMeta() : ItemUtil.getStackMeta(stack);
    }

    @Override
    public NBTTagCompound getTag() {
        return useImmutable ? immutable.getTag() : stack.getTagCompound();
    }
}
