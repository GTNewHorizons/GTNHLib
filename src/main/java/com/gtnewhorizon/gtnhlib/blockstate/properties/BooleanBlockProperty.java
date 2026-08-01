package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.util.data.BlockSupplier;

public interface BooleanBlockProperty extends BlockProperty<Boolean> {

    @Override
    default Type getType() {
        return boolean.class;
    }

    /// Primitive (non-boxing) world getter. Implementors may override for efficiency.
    default boolean getBooleanValue(IBlockAccess world, int x, int y, int z) {
        return getValue(world, x, y, z);
    }

    /// Primitive (non-boxing) world setter. Implementors may override for efficiency.
    default void setBooleanValue(World world, int x, int y, int z, boolean value) {
        setValue(world, x, y, z, value);
    }

    /// Primitive (non-boxing) stack getter. Implementors may override for efficiency.
    default boolean getBooleanValue(ItemStack stack) {
        return getValue(stack);
    }

    /// Primitive (non-boxing) stack setter. Implementors may override for efficiency.
    default void setBooleanValue(ItemStack stack, boolean value) {
        setValue(stack, value);
    }

    static FlagBooleanBlockProperty flag(String name, int flag) {
        return new FlagBooleanBlockProperty(name, flag);
    }

    static BooleanBlockProperty blocks(String name, Block falsey, Block truthy) {
        return blocks(name, () -> falsey, () -> truthy);
    }

    static BooleanBlockProperty blocks(String name, BlockSupplier falsey, BlockSupplier truthy) {
        return new BlockBooleanBlockProperty(name, truthy, falsey);
    }

    interface BooleanMetaBlockProperty extends BooleanBlockProperty, MetaBlockProperty<Boolean> {

        int getMetaPrimitive(boolean value, int existing);

        boolean getValuePrimitive(int meta);

        @Override
        default int getMeta(Boolean value, int existing) {
            return getMetaPrimitive(value, existing);
        }

        @Override
        default Boolean getValue(int meta) {
            return getValuePrimitive(meta);
        }

        @Override
        default boolean getBooleanValue(IBlockAccess world, int x, int y, int z) {
            return getValuePrimitive(world.getBlockMetadata(x, y, z));
        }

        @Override
        default void setBooleanValue(World world, int x, int y, int z, boolean value) {
            int meta = needsExisting() ? world.getBlockMetadata(x, y, z) : 0;

            int newMeta = getMetaPrimitive(value, meta);

            if (newMeta != meta) {
                world.setBlockMetadataWithNotify(x, y, z, newMeta, 2);
            }
        }

        @Override
        default boolean getBooleanValue(ItemStack stack) {
            return getValuePrimitive(stack.getItem().getMetadata(stack.getItemDamage()));
        }

        @Override
        default void setBooleanValue(ItemStack stack, boolean value) {
            int meta = needsExisting() ? stack.getItem().getMetadata(stack.getItemDamage()) : 0;

            int newMeta = getMetaPrimitive(value, meta);

            Items.feather.setDamage(stack, newMeta);
        }
    }

    class FlagBooleanBlockProperty implements BooleanMetaBlockProperty {

        public final String name;
        public final int flag;

        public FlagBooleanBlockProperty(String name, int flag) {
            this.name = name;
            this.flag = flag;
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
        public int getMetaPrimitive(boolean value, int existing) {
            existing &= ~flag;
            if (value) existing |= flag;

            return existing;
        }

        @Override
        public boolean getValuePrimitive(int meta) {
            return (meta & flag) == flag;
        }
    }

    /// Stupid name, I know, but I can't come up with anything better
    class BlockBooleanBlockProperty implements BooleanBlockProperty {

        public final String name;
        public final BlockSupplier truthy;
        public final BlockSupplier falsey;

        public BlockBooleanBlockProperty(String name, BlockSupplier truthy, BlockSupplier falsey) {
            this.name = name;
            this.truthy = truthy;
            this.falsey = falsey;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, SupportsStacks, WorldMutable, StackMutable -> true;
                default -> false;
            };
        }

        @Override
        public Boolean getValue(IBlockAccess world, int x, int y, int z) {
            return world.getBlock(x, y, z) == truthy.get();
        }

        @Override
        public void setValue(World world, int x, int y, int z, Boolean value) {
            world.setBlock(x, y, z, value ? truthy.get() : falsey.get(), world.getBlockMetadata(x, y, z), 2);
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            // noinspection DataFlowIssue
            return ((ItemBlock) stack.getItem()).field_150939_a == truthy.get();
        }

        @Override
        public void setValue(ItemStack stack, Boolean value) {
            stack.func_150996_a(Item.getItemFromBlock(value ? truthy.get() : falsey.get()));
        }
    }
}
