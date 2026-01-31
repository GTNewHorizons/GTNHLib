package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;

public interface IModelProvider {

    BakedModel getModel(@Nullable IBlockAccess world, Block block, int meta, int x, int y, int z);
}
