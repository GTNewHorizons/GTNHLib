package com.gtnewhorizon.gtnhlib.blockstate.core;

import java.io.File;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

/**
 * A world that only contains the blocks around one position, everything else is air. Used to place a {@link BlockState}
 * without the real world, IE to read a block's model, colors or bounds.
 *
 * <p>
 * Blocks can be set at offsets of at most {@link #MAX_OFFSET} on each axis from the position, setting them doesn't
 * notify any neighbors. Setting a block doesn't create its tile entity, since one created from the meta wouldn't have
 * the real one's contents (IE flower pots would get the plant of their legacy meta). Properties that need one set it.
 *
 * <p>
 * Creating a world is expensive, so keep one and {@link #reset} it for each position. Not thread safe, only use it on
 * one thread at a time.
 */
public class BlockStateFakeWorld extends World {

    /** Blocks can be set at offsets of at most this on each axis from the position. */
    public static final int MAX_OFFSET = 1;

    private static final int WIDTH = (MAX_OFFSET * 2) + 1;

    private int x;
    private int y;
    private int z;
    private BiomeGenBase biome;

    /** null where nothing was set */
    private final Block[] blockByIndex = new Block[WIDTH * WIDTH * WIDTH];
    private final int[] metaByIndex = new int[WIDTH * WIDTH * WIDTH];
    private final TileEntity[] tileEntityByIndex = new TileEntity[WIDTH * WIDTH * WIDTH];

    /** @param biome null for plains */
    public BlockStateFakeWorld(int x, int y, int z, @Nullable BiomeGenBase biome) {
        super(new ISaveHandler() {

            @Override
            public void saveWorldInfoWithPlayer(WorldInfo worldInfo, NBTTagCompound nbtTagCompound) {}

            @Override
            public void saveWorldInfo(WorldInfo worldInfo) {}

            @Override
            public WorldInfo loadWorldInfo() {
                return null;
            }

            @Override
            public IPlayerFileData getSaveHandler() {
                return null;
            }

            @Override
            public File getMapFileFromName(String mapName) {
                return null;
            }

            @Override
            public IChunkLoader getChunkLoader(WorldProvider worldProvider) {
                return null;
            }

            @Override
            public void flush() {}

            @Override
            public void checkSessionLock() {}

            @Override
            public String getWorldDirectoryName() {
                return null;
            }

            @Override
            public File getWorldDirectory() {
                return null;
            }
        }, "DUMMY_DIMENSION", null, new WorldSettings(new WorldInfo(new NBTTagCompound())), new Profiler());

        this.reset(x, y, z, biome);
    }

    /**
     * Removes all blocks and moves the world to a new position.
     *
     * @param biome null for plains
     */
    public void reset(int x, int y, int z, @Nullable BiomeGenBase biome) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biome = (biome != null) ? biome : BiomeGenBase.plains;

        Arrays.fill(this.blockByIndex, null);
        Arrays.fill(this.metaByIndex, 0);
        Arrays.fill(this.tileEntityByIndex, null);
    }

    // writing

    /** @return false outside the world */
    @Override
    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        int index = this.getIndex(x, y, z);
        if (index == -1) return false;

        this.blockByIndex[index] = block;
        this.metaByIndex[index] = meta;
        this.tileEntityByIndex[index] = null;
        return true;
    }

    /** @return false outside the world or where no block was set */
    @Override
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
        int index = this.getIndex(x, y, z);
        if (index == -1 || this.blockByIndex[index] == null) return false;

        this.metaByIndex[index] = meta;
        return true;
    }

    /** Ignored outside the world. */
    @Override
    public void setTileEntity(int x, int y, int z, TileEntity tileEntity) {
        int index = this.getIndex(x, y, z);
        if (index == -1) return;

        if (tileEntity != null) {
            tileEntity.setWorldObj(this);
            tileEntity.xCoord = x;
            tileEntity.yCoord = y;
            tileEntity.zCoord = z;
        }

        this.tileEntityByIndex[index] = tileEntity;
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        this.setTileEntity(x, y, z, null);
    }

    // reading

    @Override
    public Block getBlock(int x, int y, int z) {
        int index = this.getIndex(x, y, z);
        if (index != -1 && this.blockByIndex[index] != null) return this.blockByIndex[index];

        return Blocks.air;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        int index = this.getIndex(x, y, z);
        if (index != -1 && this.blockByIndex[index] != null) return this.metaByIndex[index];

        return 0;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        int index = this.getIndex(x, y, z);
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
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return this.biome;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return this.getBlock(x, y, z).isSideSolid(this, x, y, z, side);
    }

    // World

    @Override
    protected IChunkProvider createChunkProvider() {
        return null;
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
    }

    @Override
    protected int func_152379_p() {
        return 0;
    }

    /** @return -1 outside the world */
    private int getIndex(int x, int y, int z) {
        int offsetX = x - this.x;
        int offsetY = y - this.y;
        int offsetZ = z - this.z;
        if (Math.abs(offsetX) > MAX_OFFSET || Math.abs(offsetY) > MAX_OFFSET || Math.abs(offsetZ) > MAX_OFFSET) {
            return -1;
        }

        return ((((offsetX + MAX_OFFSET) * WIDTH) + (offsetY + MAX_OFFSET)) * WIDTH) + (offsetZ + MAX_OFFSET);
    }
}
