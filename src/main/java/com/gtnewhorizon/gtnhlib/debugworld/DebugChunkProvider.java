package com.gtnewhorizon.gtnhlib.debugworld;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Generates the debug world: an empty void with every block laid out on a single layer, above a barrier floor when Et
 * Futurum is installed.
 * <p>
 * Blocks are written straight into the chunk's storage instead of using {@link World#setBlock}. That skips all the
 * usual "a block was placed" logic, so sand does not fall, water does not flow and torches do not pop off.
 */
public class DebugChunkProvider implements IChunkProvider {

    /** The floor sits under the grid, with two blocks of air in between. */
    private static final int FLOOR_Y = DebugWorldLayout.GRID_Y - 3;
    /** Blocks are placed as if clicking the top of the block below. */
    private static final int SIDE_TOP = 1;

    private final World world;
    private final DebugWorldLayout layout;
    /** Et Futurum's barrier, or null when that mod isn't installed, in which case there is no floor. */
    private final Block floorBlock;

    public DebugChunkProvider(World world) {
        this.world = world;
        this.layout = DebugWorldLayout.create();
        this.floorBlock = GameRegistry.findBlock("etfuturum", "barrier");
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(world, chunkX, chunkZ);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (floorBlock != null) setBlockInChunk(chunk, x, FLOOR_Y, z, floorBlock, 0);

                int index = layout.getIndexAt(chunkX * 16 + x, chunkZ * 16 + z);
                // Blocks that need their item are placed later, in populate()
                if (index < 0 || layout.getPlacementItem(index) != null) continue;
                setBlockInChunk(chunk, x, DebugWorldLayout.GRID_Y, z, layout.getBlock(index), layout.getMeta(index));
            }
        }

        // The biome array is left untouched on purpose. Unset entries are looked up from DebugWorldType's chunk
        // manager (plains everywhere), and EndlessIDs crashes if a mod writes the vanilla biome array directly.
        generateSkylight(chunk);
        return chunk;
    }

    private void setBlockInChunk(Chunk chunk, int x, int y, int z, Block block, int meta) {
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        ExtendedBlockStorage section = sections[y >> 4];
        if (section == null) {
            section = new ExtendedBlockStorage(y & ~15, !world.provider.hasNoSky);
            sections[y >> 4] = section;
        }
        section.func_150818_a(x, y & 15, z, block);
        section.setExtBlockMetadata(x, y & 15, z, meta);
        section.setExtBlocklightValue(x, y & 15, z, block.getLightValue());
    }

    /**
     * Replaces {@link Chunk#generateSkylightMap()}, which asks every block how much light it blocks using the world
     * position. Some blocks (like Railcraft machines) answer that by looking themselves up in the world, but this chunk
     * is not in the world yet, so the game tries to generate it again, forever. Here the position-free version of the
     * question is used instead, which is enough for a few layers of blocks in a void.
     */
    private void generateSkylight(Chunk chunk) {
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        int topY = chunk.getTopFilledSegment() + 15;
        int lowestHeight = Integer.MAX_VALUE;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = 0;
                int light = 15;

                for (int y = topY; y >= 0; y--) {
                    ExtendedBlockStorage section = sections[y >> 4];
                    int opacity = section == null ? 0 : section.getBlockByExtId(x, y & 15, z).getLightOpacity();
                    // The height map points at the air block above the highest block that blocks light
                    if (opacity > 0 && height == 0) height = y + 1;

                    // Same rule as vanilla: full sunlight until the first block, then one level less for every
                    // block below
                    if (opacity == 0 && light != 15) opacity = 1;
                    light = Math.max(0, light - opacity);
                    if (section != null && !world.provider.hasNoSky) section.setExtSkylightValue(x, y & 15, z, light);
                }

                chunk.heightMap[z << 4 | x] = height;
                lowestHeight = Math.min(lowestHeight, height);
            }
        }

        chunk.heightMapMinimum = lowestHeight;
        chunk.isModified = true;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return provideChunk(chunkX, chunkZ);
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return true;
    }

    /**
     * Runs once per chunk, after it has been added to the world. Nothing is decorated here (no trees, ores or animals).
     * The only thing that happens is placing the blocks that need their item, like GregTech machines, which can't be
     * set up while the chunk is still being generated.
     */
    @Override
    public void populate(IChunkProvider provider, int chunkX, int chunkZ) {
        FakePlayer placer = null;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int index = layout.getIndexAt(worldX, worldZ);
                if (index < 0) continue;

                ItemStack placementItem = layout.getPlacementItem(index);
                if (placementItem == null) continue;

                if (placer == null) placer = FakePlayerFactory.getMinecraft((WorldServer) world);
                placeWithItem(placer, placementItem.copy(), worldX, DebugWorldLayout.GRID_Y, worldZ);
            }
        }
    }

    /**
     * Places a block the same way a player would with the item in hand, so the item gets to set up the block's tile
     * entity (for GregTech machines, which machine it is).
     */
    private void placeWithItem(FakePlayer placer, ItemStack stack, int x, int y, int z) {
        ItemBlock item = (ItemBlock) stack.getItem();
        placer.inventory.setInventorySlotContents(placer.inventory.currentItem, stack);
        try {
            int meta = item.getMetadata(stack.getItemDamage());
            if (!item.placeBlockAt(stack, placer, world, x, y, z, SIDE_TOP, 0.5F, 1.0F, 0.5F, meta)) {
                GTNHLib.LOG.warn(
                        "{} (damage {}) refused to be placed in the debug world",
                        Item.itemRegistry.getNameForObject(item),
                        stack.getItemDamage());
            }
        } catch (Throwable t) {
            GTNHLib.LOG.warn(
                    "Could not place {} (damage {}) in the debug world, add it to DebugWorldLayout.SKIPPED if it keeps failing",
                    Item.itemRegistry.getNameForObject(item),
                    stack.getItemDamage(),
                    t);
            // A half set up block is more likely to crash later than an empty spot
            world.setBlockToAir(x, y, z);
        } finally {
            placer.inventory.setInventorySlotContents(placer.inventory.currentItem, null);
        }
    }

    /** Empty list, so no mobs ever spawn. */
    @Override
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        return Collections.emptyList();
    }

    @Override
    public ChunkPosition func_147416_a(World world, String structureName, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean saveChunks(boolean saveAll, IProgressUpdate progress) {
        return true;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "DebugLevelSource";
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void recreateStructures(int chunkX, int chunkZ) {}

    @Override
    public void saveExtraData() {}
}
