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
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import it.unimi.dsi.fastutil.Hash.Strategy;

@SuppressWarnings("unused")
public final class ItemId implements ImmutableItemStack {

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
    public NBTTagCompound getTag() {
        return tag;
    }

    @Override
    public int getStackSize() {
        return 0;
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

    /**
     * A hash strategy that only checks the item and metadata.
     */
    public static final Strategy<ItemStack> STACK_ITEM_META_STRATEGY = new Strategy<>() {

        @Override
        public int hashCode(ItemStack o) {
            int hash = Fnv1a32.initialState();

            if (o != null) {
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(o.getItem()));
                hash = Fnv1a32.hashStep(hash, ItemUtil.getStackMeta(o));
            }

            return hash;
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            if (a.getItem() != b.getItem()) return false;
            return ItemUtil.getStackMeta(a) == ItemUtil.getStackMeta(b);
        }
    };

    /**
     * A hash strategy that checks the item, metadata, and nbt.
     */
    public static final Strategy<ItemStack> STACK_ITEM_META_NBT_STRATEGY = new Strategy<>() {

        @Override
        public int hashCode(ItemStack o) {
            int hash = Fnv1a32.initialState();

            if (o != null) {
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(o.getItem()));
                hash = Fnv1a32.hashStep(hash, ItemUtil.getStackMeta(o));
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(o.getTagCompound()));
            }

            return hash;
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            if (a.getItem() != b.getItem()) return false;
            if (ItemUtil.getStackMeta(a) != ItemUtil.getStackMeta(b)) return false;
            return Objects.equals(a.getTagCompound(), b.getTagCompound());
        }
    };

    private static Item getGenericItem(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Item item) return item;
        if (obj instanceof ItemStack stack) return stack.getItem();
        // Includes ImmutableItemStack and ItemId
        if (obj instanceof ImmutableItemMeta im) return im.getItem();

        throw new IllegalArgumentException("Cannot extract item from object: " + obj);
    }

    private static int getGenericMeta(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof ItemStack stack) return ItemUtil.getStackMeta(stack);
        // Includes ImmutableItemStack and ItemId
        if (obj instanceof ImmutableItemMeta im) return im.getItemMeta();

        throw new IllegalArgumentException("Cannot extract item metadata from object: " + obj);
    }

    private static NBTTagCompound getGenericTag(Object obj) {
        if (obj == null) return null;
        if (obj instanceof ItemStack stack) return stack.getTagCompound();
        // Includes ItemId
        if (obj instanceof ImmutableItemStack stack) return stack.getTag();

        throw new IllegalArgumentException("Cannot extract item metadata from object: " + obj);
    }

    /// A hash strategy that only checks the item and metadata. Works with [ItemStack], [ItemId], [ImmutableItemMeta],
    /// and [ImmutableItemStack] - equivalent objects that represent the same 'stack' will have the same hash and equal
    /// each other.
    public static final Strategy<Object> GENERIC_ITEM_META_STRATEGY = new Strategy<>() {

        @Override
        public int hashCode(Object o) {
            int hash = Fnv1a32.initialState();

            if (o != null) {
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(getGenericItem(o)));
                hash = Fnv1a32.hashStep(hash, getGenericMeta(o));
            }

            return hash;
        }

        @Override
        public boolean equals(Object a, Object b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            if (getGenericItem(a) != getGenericItem(b)) return false;
            return getGenericMeta(a) == getGenericMeta(b);
        }
    };

    /// A hash strategy that checks the item, metadata, and tag. Works with [ItemStack], [ItemId], [ImmutableItemMeta],
    /// and [ImmutableItemStack] - equivalent objects that represent the same 'stack' will have the same hash and equal
    /// each other.
    public static final Strategy<Object> GENERIC_ITEM_META_NBT_STRATEGY = new Strategy<>() {

        @Override
        public int hashCode(Object o) {
            int hash = Fnv1a32.initialState();

            if (o != null) {
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(getGenericItem(o)));
                hash = Fnv1a32.hashStep(hash, getGenericMeta(o));
                hash = Fnv1a32.hashStep(hash, Objects.hashCode(getGenericTag(o)));
            }

            return hash;
        }

        @Override
        public boolean equals(Object a, Object b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            if (getGenericItem(a) != getGenericItem(b)) return false;
            if (getGenericMeta(a) == getGenericMeta(b)) return false;
            return Objects.equals(getGenericTag(a), getGenericTag(b));
        }
    };
}
