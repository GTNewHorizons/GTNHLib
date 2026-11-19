package com.gtnewhorizon.gtnhlib.client.model;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

public class ItemContext implements BakedModelQuadContext.Item {

    public ItemStack stack;
    public BlockState blockState;
    public ModelQuadFacing quadFacing;
    public Random random;
    public Supplier<ModelQuadViewMutable> quadPool;

    private final BlockStatePool pool = new BlockStatePool(4);

    public void reset() {
        stack = null;
        if (blockState != null) blockState.close();
        blockState = null;
        quadFacing = null;
        random = null;
        this.quadPool = null;
    }

    public void set(ItemStack stack, Random random) {
        this.stack = stack;
        this.blockState = BlockPropertyRegistry.getBlockState(pool, stack);
        this.random = random;
        // I mean, I *could* pack 0, 0, 0. But that seems like a waste when I know the answer...
        this.random.setSeed(0);
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
