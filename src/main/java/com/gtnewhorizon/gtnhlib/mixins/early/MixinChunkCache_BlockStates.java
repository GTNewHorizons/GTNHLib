package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockState_IBAExt;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;
import com.gtnewhorizon.gtnhlib.util.EBSUtil;

@Mixin(ChunkCache.class)
public class MixinChunkCache_BlockStates implements BlockState_IBAExt {

    @Shadow private int chunkX;
    @Shadow private int chunkZ;
    @Shadow private Chunk[][] chunkArray;

    @Override
    public @Nullable BlockStateStorage gtnhlib$getBlockStateStorage(int x, int y, int z) {
        int cx = (x >> 4) - chunkX;
        int cz = (z >> 4) - chunkZ;
        if (cx < 0 || cz < 0 || cx >= chunkArray.length || cz >= chunkArray[cx].length) return null;
        Chunk chunk = chunkArray[cx][cz];
        if (chunk == null) return null;
        return BlockStateStorage.getStorage(EBSUtil.getEBS(chunk, y >> 4), false);
    }
}
