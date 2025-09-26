package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

import it.unimi.dsi.fastutil.Hash;

@SuppressWarnings("unused")
public record ItemId(Item item, int meta, NBTTagCompound tag) implements ImmutableItemMeta {

    @Override
    public @NotNull Item item() {
        return item;
    }

    @Override
    public int getItemMeta() {
        return meta;
    }

    @Override
    public ItemStack toStack(int amount) {
        ItemStack stack = ImmutableItemMeta.super.toStack(amount);

        if (tag != null) {
            stack.setTagCompound((NBTTagCompound) tag.copy());
        }

        return stack;
    }

    public static ItemId create(NBTTagCompound tag) {
        return new ItemId(
                Item.getItemById(tag.getInteger("item")),
                tag.getInteger("meta"),
                tag.hasKey("tag", Constants.NBT.TAG_COMPOUND) ? tag.getCompoundTag("tag") : null);
    }

    public static ItemId create(ItemStack stack) {
        return create(stack.getItem(), Items.feather.getDamage(stack), stack.getTagCompound());
    }

    public static ItemId create(Item item, int metaData, @Nullable NBTTagCompound tag) {
        if (tag != null) {
            tag = (NBTTagCompound) tag.copy();
        }
        return new ItemId(item, metaData, tag);
    }

    public static ItemId createAsWildcard(ItemStack stack) {
        return new ItemId(stack.getItem(), OreDictionary.WILDCARD_VALUE, null);
    }

    public static ItemId createAsWildcardWithNBT(ItemStack stack) {
        return create(stack.getItem(), OreDictionary.WILDCARD_VALUE, stack.getTagCompound());
    }

    public static ItemId createWithoutNBT(ItemStack stack) {
        return new ItemId(stack.getItem(), Items.feather.getDamage(stack), null);
    }

    public static ItemId createNoCopy(Item item, int metaData, @Nullable NBTTagCompound nbt) {
        return new ItemId(item, metaData, nbt);
    }

    public static ItemId createNoCopy(ItemStack stack) {
        return new ItemId(stack.getItem(), Items.feather.getDamage(stack), stack.getTagCompound());
    }

    /**
     * A hash strategy that only checks the item and metadata.
     */
    public static final Hash.Strategy<ItemId> ITEM_META_STRATEGY = new Hash.Strategy<>() {

        @Override
        public int hashCode(ItemId o) {
            int hash = Fnv1a32.initialState();

            if (o != null) {
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(o.item));
                hash = Fnv1a32.hashStep(hash, o.meta);
            }

            return hash;
        }

        @Override
        public boolean equals(ItemId a, ItemId b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            return a.item() == b.item() && a.meta == b.meta;
        }
    };

    /**
     * A hash strategy that checks the item, metadata, and nbt.
     */
    public static final Hash.Strategy<ItemId> ITEM_META_NBT_STRATEGY = new Hash.Strategy<>() {

        @Override
        public int hashCode(ItemId o) {
            int hash = Fnv1a32.initialState();

            if (o != null) {
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(o.item));
                hash = Fnv1a32.hashStep(hash, o.meta);
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(o.tag));
            }

            return hash;
        }

        @Override
        public boolean equals(ItemId a, ItemId b) {
            return a.item == b.item && a.meta == b.meta && Objects.equals(a.tag, b.tag);
        }
    };
}
