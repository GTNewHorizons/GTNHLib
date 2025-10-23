package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;

public interface IntegerBlockProperty extends BlockProperty<Integer> {

    @Override
    default Type getType() {
        return int.class;
    }

    default BlockProperty<String> map(List<String> values) {
        return new MappedBlockProperty(this, values);
    }

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
        public String getValue(IBlockAccess world, int x, int y, int z) {
            return BlockProperty.getIndexSafe(values, base.getValue(world, x, y, z));
        }

        @Override
        public void setValue(World world, int x, int y, int z, String value) {
            base.setValue(world, x, y, z, values.indexOf(value));
        }

        @Override
        public String getValue(ItemStack stack) {
            return BlockProperty.getIndexSafe(values, base.getValue(stack));
        }

        @Override
        public void setValue(ItemStack stack, String value) {
            base.setValue(stack, values.indexOf(value));
        }

        @Override
        public boolean needsExisting() {
            return ((MetaBlockProperty<Integer>) base).needsExisting();
        }

        @Override
        public int getMeta(String value, int existing) {
            return ((MetaBlockProperty<Integer>) base).getMeta(values.indexOf(value), existing);
        }

        @Override
        public String getValue(int meta) {
            return values.get(((MetaBlockProperty<Integer>) base).getValue(meta));
        }
    }

    static IntegerBlockProperty meta(String name, int mask, int shift) {
        return new MetaIntegerBlockProperty(name, mask, shift);
    }

    class MetaIntegerBlockProperty implements IntegerBlockProperty, MetaBlockProperty<Integer> {

        private final String name;
        private final int mask;
        private final int shift;

        public MetaIntegerBlockProperty(String name, int mask, int shift) {
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
        public int getMeta(Integer value, int existing) {
            int meta = existing & ~mask;

            meta |= (value << shift) & mask;

            return meta;
        }

        @Override
        public Integer getValue(int meta) {
            return (meta & mask) >> shift;
        }
    }
}
