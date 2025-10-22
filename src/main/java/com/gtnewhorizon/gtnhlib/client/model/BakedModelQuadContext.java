package com.gtnewhorizon.gtnhlib.client.model;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.color.ImmutableColor;

public interface BakedModelQuadContext {

    BlockState getBlockState();

    ModelQuadFacing getQuadFacing();

    Random getRandom();

    /// The value returned from [BakedModel#getColor(BakedModelQuadContext)]. Note that this value is null within the
    /// execution of [BakedModel#getColor(BakedModelQuadContext)].
    ImmutableColor getColor();

    /// The quad pool to obtain quads from and where the quads should be released after use.
    /// Only non-null when [BakedModel#isDynamic()] returns true.
    @Nullable
    Supplier<ModelQuadViewMutable> getQuadPool();

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
