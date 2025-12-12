package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;

import net.minecraft.block.Block;
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

    static FlagBooleanBlockProperty flag(String name, int flag) {
        return new FlagBooleanBlockProperty(name, flag);
    }

    class FlagBooleanBlockProperty implements BooleanBlockProperty, MetaBlockProperty<Boolean> {

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
        public int getMeta(Boolean value, int existing) {
            existing &= ~flag;
            if (value) existing |= flag;

            return existing;
        }

        @Override
        public Boolean getValue(int meta) {
            return (meta & flag) == flag;
        }
    }

    static BooleanBlockProperty blocks(String name, Block falsey, Block truthy) {
        return blocks(name, () -> falsey, () -> truthy);
    }

    static BooleanBlockProperty blocks(String name, BlockSupplier falsey, BlockSupplier truthy) {
        return new BooleanBlockProperty() {

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
        };
    }
}
