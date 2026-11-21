package com.gtnewhorizon.gtnhlib.blockstate.core;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

/**
 * A world that only contains one block, everything else is air until a {@link BlockStateDataHandler#decode} adds to it.
 * Used to read a block's model, colors or bounds without the real world.
 *
 * <p>
 * Create one per block, pass it to {@link com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry#decode}
 * and then use it as the {@link IBlockAccess}. Not thread safe, only use it on one thread at a time.
 */
public class BlockStateDataWorld implements IBlockAccess {

    /** Decoding can set blocks at offsets of at most this on each axis from the block. */
    public static final int MAX_OFFSET = 1;

    private static final int WIDTH = (MAX_OFFSET * 2) + 1;

    private final int x;
    private final int y;
    private final int z;
    private final Block block;
    private final int meta;
    private final BiomeGenBase biome;

    /** null where nothing was set */
    private final Block[] blockByIndex = new Block[WIDTH * WIDTH * WIDTH];
    private final int[] metaByIndex = new int[WIDTH * WIDTH * WIDTH];
    /** null where nothing was set, the block itself included */
    private final TileEntity[] tileEntityByIndex = new TileEntity[WIDTH * WIDTH * WIDTH];

    /** @param biome null for plains */
    public BlockStateDataWorld(int x, int y, int z, Block block, int meta, @Nullable BiomeGenBase biome) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.meta = meta;
        this.biome = (biome != null) ? biome : BiomeGenBase.plains;
    }

    // decoding

    /**
     * For {@link BlockStateDataHandler#decode}. Larger offsets than {@link #MAX_OFFSET} and the block itself (offset 0,
     * 0, 0) throw an {@link IllegalArgumentException}.
     */
    public void setBlock(int offsetX, int offsetY, int offsetZ, Block block, int meta) {
        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) {
            throw new IllegalArgumentException("The block itself can't be replaced.");
        }

        int index = getIndexForOffset(offsetX, offsetY, offsetZ);
        if (index == -1) {
            throw new IllegalArgumentException(
                    "Offset [" + offsetX
                            + ","
                            + offsetY
                            + ","
                            + offsetZ
                            + "] is outside the fake world, at most ["
                            + MAX_OFFSET
                            + "] is allowed on each axis.");
        }

        this.blockByIndex[index] = block;
        this.metaByIndex[index] = meta;
    }

    /**
     * For {@link BlockStateDataHandler#decode}, the block itself (offset 0, 0, 0) can have one too. Larger offsets than
     * {@link #MAX_OFFSET} throw an {@link IllegalArgumentException}.
     */
    public void setTileEntity(int offsetX, int offsetY, int offsetZ, TileEntity tileEntity) {
        int index = getIndexForOffset(offsetX, offsetY, offsetZ);
        if (index == -1) {
            throw new IllegalArgumentException(
                    "Offset [" + offsetX
                            + ","
                            + offsetY
                            + ","
                            + offsetZ
                            + "] is outside the fake world, at most ["
                            + MAX_OFFSET
                            + "] is allowed on each axis.");
        }

        this.tileEntityByIndex[index] = tileEntity;
    }

    // IBlockAccess

    @Override
    public Block getBlock(int x, int y, int z) {
        if (x == this.x && y == this.y && z == this.z) return this.block;

        int index = getIndexForOffset(x - this.x, y - this.y, z - this.z);
        if (index != -1 && this.blockByIndex[index] != null) return this.blockByIndex[index];

        return Blocks.air;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (x == this.x && y == this.y && z == this.z) return this.meta;

        int index = getIndexForOffset(x - this.x, y - this.y, z - this.z);
        if (index != -1 && this.blockByIndex[index] != null) return this.metaByIndex[index];

        return 0;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        int index = getIndexForOffset(x - this.x, y - this.y, z - this.z);
        return (index != -1) ? this.tileEntityByIndex[index] : null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minLight) {
        return 0;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return this.getBlock(x, y, z).isAir(this, x, y, z);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return this.biome;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return this.getBlock(x, y, z).isSideSolid(this, x, y, z, side);
    }

    /** @return -1 if the offset is outside the fake world */
    private static int getIndexForOffset(int offsetX, int offsetY, int offsetZ) {
        if (Math.abs(offsetX) > MAX_OFFSET || Math.abs(offsetY) > MAX_OFFSET || Math.abs(offsetZ) > MAX_OFFSET) {
            return -1;
        }

        return ((((offsetX + MAX_OFFSET) * WIDTH) + (offsetY + MAX_OFFSET)) * WIDTH) + (offsetZ + MAX_OFFSET);
    }
}
