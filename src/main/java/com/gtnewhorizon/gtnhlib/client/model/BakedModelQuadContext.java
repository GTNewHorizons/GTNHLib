package com.gtnewhorizon.gtnhlib.client.model;

import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

public interface BakedModelQuadContext {

    BlockState getBlockState();

    ModelQuadFacing getQuadFacing();

    Random getRandom();

    interface World extends BakedModelQuadContext {

        IBlockAccess getWorld();

        int getX();

        int getY();

        int getZ();
    }

    interface Item extends BakedModelQuadContext {

        ItemStack getItemStack();
    }
}
