package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import it.unimi.dsi.fastutil.Hash.Strategy;

@SuppressWarnings("unused")
public final class ItemId implements ImmutableItemMeta {

    private final Item item;
    private final int meta;
    private final NBTTagCompound tag;

    public ItemId(Item item, int meta, NBTTagCompound tag) {
        this.item = item;
        this.meta = meta;
        this.tag = tag;
    }

    @Override
    public @NotNull Item getItem() {
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

    /// Matches [ItemStack#writeToNBT(NBTTagCompound)].
    public static ItemId create(NBTTagCompound tag) {
        return new ItemId(
                Item.getItemById(tag.getInteger("id")),
                tag.getInteger("Damage"),
                tag.hasKey("tag", NBT.TAG_COMPOUND) ? tag.getCompoundTag("tag") : null);
    }

    public NBTTagCompound write(NBTTagCompound tag) {
        tag.setInteger("id", Item.getIdFromItem(this.item));
        tag.setInteger("Damage", this.meta);
        if (this.tag != null) tag.setTag("tag", tag.copy());

        return tag;
    }

    public static ItemId create(ItemStack stack) {
        return create(stack.getItem(), ItemUtil.getStackMeta(stack), stack.getTagCompound());
    }

    public static ItemId create(Item item, int metaData, @Nullable NBTTagCompound tag) {
        if (tag != null) {
            tag = (NBTTagCompound) tag.copy();
        }
        return new ItemId(item, metaData, tag);
    }

    public static ItemId createAsWildcard(ItemStack stack) {
        return new ItemId(stack.getItem(), OreDictionary.WILDCARD_VALUE, stack.getTagCompound());
    }

    public static ItemId createAsWildcardWithoutNBT(ItemStack stack) {
        return create(stack.getItem(), OreDictionary.WILDCARD_VALUE, null);
    }

    public static ItemId createWithoutNBT(ItemStack stack) {
        return new ItemId(stack.getItem(), ItemUtil.getStackMeta(stack), null);
    }

    public static ItemId createNoCopy(Item item, int metaData, @Nullable NBTTagCompound nbt) {
        return new ItemId(item, metaData, nbt);
    }

    public static ItemId createNoCopy(ItemStack stack) {
        return new ItemId(stack.getItem(), ItemUtil.getStackMeta(stack), stack.getTagCompound());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemId itemId)) return false;

        return ITEM_META_NBT_STRATEGY.equals(this, itemId);
    }

    @Override
    public int hashCode() {
        return ITEM_META_NBT_STRATEGY.hashCode(this);
    }

    @Override
    public String toString() {
        return "ItemId[" + "item=" + item + ", " + "meta=" + meta + ", " + "tag=" + tag + ']';
    }

    /**
     * A hash strategy that only checks the item and metadata.
     */
    public static final Strategy<ItemId> ITEM_META_STRATEGY = new Strategy<>() {

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

            return a.getItem() == b.getItem() && a.meta == b.meta;
        }
    };

    /**
     * A hash strategy that checks the item, metadata, and nbt.
     */
    public static final Strategy<ItemId> ITEM_META_NBT_STRATEGY = new Strategy<>() {

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
            if (a == b) return true;
            if (a == null || b == null) return false;

            return a.item == b.item && a.meta == b.meta && Objects.equals(a.tag, b.tag);
        }
    };
}
