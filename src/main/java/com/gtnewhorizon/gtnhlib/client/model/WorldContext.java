package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

public class WorldContext implements BakedModelQuadContext.World {

    public IBlockAccess world;
    public int x, y, z;
    public BlockState blockState;
    public ModelQuadFacing quadFacing;
    public Random random;
    public Supplier<ModelQuadViewMutable> quadPool;

    public void reset() {
        this.world = null;
        if (blockState != null) blockState.close();
        this.blockState = null;
        this.quadFacing = null;
        this.random = null;
        this.quadPool = null;
    }

    public void set(IBlockAccess world, int x, int y, int z, Random random) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockState = BlockPropertyRegistry.getBlockState(world, x, y, z);
        this.random = random;
        random.setSeed(pack(x, y, z));
    }

    @Override
    public IBlockAccess getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
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
