package com.gtnewhorizon.gtnhlib.client.model;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

public class ItemContext implements BakedModelQuadContext.Item {

    public ItemStack stack;
    public BlockState blockState;
    public ModelQuadFacing quadFacing;
    public Random random;
    public Supplier<ModelQuadViewMutable> quadPool;

    public void reset() {
        stack = null;
        if (blockState != null) blockState.close();
        blockState = null;
        quadFacing = null;
        random = null;
        this.quadPool = null;
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }

    @Override
    public BlockState getBlockState() {
        return blockState;
    }

    @Override
    public ModelQuadFacing getQuadFacing() {
        return quadFacing;
    }

    @Override
    public Random getRandom() {
        return random;
    }

    @Override
    public @Nullable Supplier<ModelQuadViewMutable> getQuadPool() {
        return quadPool;
    }
}
