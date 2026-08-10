package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;

public interface IntegerBlockProperty extends BlockProperty<Integer> {

    @Override
    default Type getType() {
        return int.class;
    }

    /// Primitive (non-boxing) world getter. Implementors may override for efficiency.
    default int getIntValue(IBlockAccess world, int x, int y, int z) {
        return getValue(world, x, y, z);
    }

    /// Primitive (non-boxing) world setter. Implementors may override for efficiency.
    default void setIntValue(World world, int x, int y, int z, int value) {
        setValue(world, x, y, z, value);
    }

    /// Primitive (non-boxing) stack getter. Implementors may override for efficiency.
    default int getIntValue(ItemStack stack) {
        return getValue(stack);
    }

    /// Primitive (non-boxing) stack setter. Implementors may override for efficiency.
    default void setIntValue(ItemStack stack, int value) {
        setValue(stack, value);
    }

    default BlockProperty<String> map(List<String> values) {
        return new MappedBlockProperty(this, values);
    }

    /// Creates a property that occupies the full 4-bit metadata value (mask=-1, shift=0).
    /// Use this when the property is the only metadata user on this block.
    static IntegerBlockProperty meta(String name) {
        return new BitPackedIntegerMetaBlockProperty(name, -1, 0);
    }

    /// Creates a property backed by a specific bit range of block metadata.
    /// `mask` selects which bits belong to this property; `shift` is the number of low bits to skip.
    /// Example: bits 2-3 → mask=0b1100 (0xC), shift=2.
    static IntegerBlockProperty meta(String name, int mask, int shift) {
        return new BitPackedIntegerMetaBlockProperty(name, mask, shift);
    }

    interface IntegerMetaBlockProperty extends IntegerBlockProperty, MetaBlockProperty<Integer> {

        int getMetaPrimitive(int value, int existing);

        int getValuePrimitive(int meta);

        @Override
        default int getMeta(Integer value, int existing) {
            return getMetaPrimitive(value, existing);
        }

        @Override
        default Integer getValue(int meta) {
            return getValuePrimitive(meta);
        }

        @Override
        default int getIntValue(IBlockAccess world, int x, int y, int z) {
            return getValuePrimitive(world.getBlockMetadata(x, y, z));
        }

        @Override
        default void setIntValue(World world, int x, int y, int z, int value) {
            int meta = needsExisting() ? world.getBlockMetadata(x, y, z) : 0;

            int newMeta = getMetaPrimitive(value, meta);

            if (newMeta != meta) {
                world.setBlockMetadataWithNotify(x, y, z, newMeta, 2);
            }
        }

        @Override
        default int getIntValue(ItemStack stack) {
            return getValuePrimitive(stack.getItem().getMetadata(stack.getItemDamage()));
        }

        @Override
        default void setIntValue(ItemStack stack, int value) {
            int meta = needsExisting() ? stack.getItem().getMetadata(stack.getItemDamage()) : 0;

            int newMeta = getMetaPrimitive(value, meta);

            Items.feather.setDamage(stack, newMeta);
        }
    }

    /// Wraps an {@link IntegerBlockProperty} and maps its integer values to display strings.
    ///
    /// Note: {@link #appliesTo} delegates to the underlying property (not always {@code true}).
    @SuppressWarnings("unchecked")
    class MappedBlockProperty implements BlockProperty<String>, MetaBlockProperty<String> {

        private final IntegerBlockProperty base;
        private final List<String> values;

        public MappedBlockProperty(IntegerBlockProperty base, List<String> values) {
            this.base = base;
            this.values = values;
        }

        @Override
        public String getName() {
            return base.getName();
        }

        @Override
        public Type getType() {
            return String.class;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            if (trait == BlockPropertyTrait.Transformable) return false;

            return base.hasTrait(trait);
        }

        @Override
        public boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta,
                @Nullable TileEntity tile) {
            return base.appliesTo(world, x, y, z, block, meta, tile);
        }

        @Override
        public boolean appliesTo(ItemStack stack, Item item, int meta) {
            return base.appliesTo(stack, item, meta);
        }

        @Override
        public boolean needsExisting() {
            // Unchecked cast: this should only be called if the base is meta, so it's fine
            return ((MetaBlockProperty<Integer>) base).needsExisting();
        }

        @Override
        public String getValue(IBlockAccess world, int x, int y, int z) {
            return BlockProperty.getIndexSafe(values, base.getValue(world, x, y, z));
        }

        @Override
        public void setValue(World world, int x, int y, int z, String value) {
            int idx = values.indexOf(value);
            if (idx == -1) throw new IllegalArgumentException("Unknown mapped value: " + value);
            base.setValue(world, x, y, z, idx);
        }

        @Override
        public String getValue(ItemStack stack) {
            return BlockProperty.getIndexSafe(values, base.getValue(stack));
        }

        @Override
        public void setValue(ItemStack stack, String value) {
            int idx = values.indexOf(value);
            if (idx == -1) throw new IllegalArgumentException("Unknown mapped value: " + value);
            base.setValue(stack, idx);
        }

        @Override
        public int getMeta(String value, int existing) {
            return ((MetaBlockProperty<Integer>) base).getMeta(values.indexOf(value), existing);
        }

        @Override
        public String getValue(int meta) {
            return BlockProperty.getIndexSafe(values, ((MetaBlockProperty<Integer>) base).getValue(meta));
        }
    }

    /** @deprecated Use {@link BitPackedIntegerMetaBlockProperty} */
    @Deprecated
    class MetaIntegerBlockProperty extends BitPackedIntegerMetaBlockProperty {

        public MetaIntegerBlockProperty(String name, int mask, int shift) {
            super(name, mask, shift);
        }
    }

    class BitPackedIntegerMetaBlockProperty implements IntegerMetaBlockProperty {

        private final String name;
        private final int mask;
        private final int shift;

        public BitPackedIntegerMetaBlockProperty(String name, int mask, int shift) {
            this.name = name;
            this.mask = mask;
            this.shift = shift;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, SupportsStacks, OnlyNeedsMeta, WorldMutable, StackMutable -> true;
                default -> false;
            };
        }

        @Override
        public boolean needsExisting() {
            return mask != -1;
        }

        @Override
        public int getMetaPrimitive(int value, int existing) {
            int meta = existing & ~mask;

            meta |= (value << shift) & mask;

            return meta;
        }

        @Override
        public int getValuePrimitive(int meta) {
            return (meta & mask) >> shift;
        }
    }
}
