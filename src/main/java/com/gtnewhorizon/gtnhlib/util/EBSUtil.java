package com.gtnewhorizon.gtnhlib.util;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class EBSUtil {

    /// Gets an [ExtendedBlockStorage] from the world.
    public static ExtendedBlockStorage getEBS(World world, int chunkX, int ebsY, int chunkZ) {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);

        return getEBS(chunk, ebsY);
    }

    /// Gets an [ExtendedBlockStorage] from a specific chunk. Note: this method is overwritten by cubic chunks.
    public static ExtendedBlockStorage getEBS(Chunk chunk, int ebsY) {
        return ebsY < 0 || ebsY >= chunk.getBlockStorageArray().length ? null : chunk.getBlockStorageArray()[ebsY];
    }
}
