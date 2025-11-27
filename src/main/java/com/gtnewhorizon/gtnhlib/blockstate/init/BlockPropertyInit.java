package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyFactory;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

public class BlockPropertyInit {

    public static void init() {
        MetaProperty metaProperty = new MetaProperty();

        BlockPropertyRegistry.registerProperty(new BlockPropertyFactory<Integer>() {

            @Override
            public BlockProperty<Integer> getProperty(IBlockAccess world, int x, int y, int z, Block block, int meta,
                    @Nullable TileEntity tile) {
                return metaProperty;
            }

            @Override
            public BlockProperty<Integer> getProperty(ItemStack stack, Item item, int meta) {
                return metaProperty;
            }
        });

        VanillaBlockProperties.initVanilla();
    }

    private static class MetaProperty implements IntegerBlockProperty, MetaBlockProperty<Integer> {

        @Override
        public String getName() {
            return "meta";
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, SupportsStacks, OnlyNeedsMeta, WorldMutable, StackMutable -> true;
                default -> false;
            };
        }

        @Override
        public int getMeta(Integer value, int existing) {
            return existing;
        }

        @Override
        public Integer getValue(int meta) {
            return meta;
        }
    }
}
