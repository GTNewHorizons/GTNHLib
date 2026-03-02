package com.gtnewhorizon.gtnhlib.blockstate.registry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;

public interface BlockPropertyFactory<TValue> {

    @Nullable
    default BlockProperty<TValue> getProperty(IBlockAccess world, int x, int y, int z, Block block, int meta,
            @Nullable TileEntity tile) {
        return null;
    }

    @Nullable
    default BlockProperty<TValue> getProperty(ItemStack stack, Item item, int meta) {
        return null;
    }
}
