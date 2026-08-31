package com.gtnewhorizon.gtnhlib.blockstate.storage;

import java.util.BitSet;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import lombok.Getter;
import lombok.SneakyThrows;

/// This is the core data structure for native block states. It is the block state equivalent of an [ExtendedBlockStorage],
/// except instead of using discrete IDs it relies on a Short2Object map with bitpacked keys.
/// This does not manage the lifetime of native states at all - that is typically handled by the various mixins, or [BSSDataManager].
public class BlockStateStorage implements Cloneable {

    /// A bit will be true when the corresponding voxel contains a block state.
    /// This is used to skip pointless map operations, i.e. when getting a state from a location that doesn't have one.
    @Getter
    private BitSet presence = new BitSet(16*16*16);

    @Getter
    private Short2ObjectOpenHashMap<BlockState> blockStates = new Short2ObjectOpenHashMap<>();

    public static short pack(int x, int y, int z) {
        x &= 0xF;
        y &= 0xF;
        z &= 0xF;

        return (short) (y << 8 | z << 4 | x);
    }

    public static int unpackX(int l) {
        return l & 0xF;
    }

    public static int unpackY(int l) {
        return l >> 8 & 0xF;
    }

    public static int unpackZ(int l) {
        return l >> 4 & 0xF;
    }

    public boolean hasState(int rx, int ry, int rz) {
        return presence.get(pack(rx, ry, rz));
    }

    /// Allocation-free getter. Returned value must be treated as immutable and not modified.
    public @Nullable BlockState getBlockStateUnsafe(int rx, int ry, int rz) {
        if (!hasState(rx, ry, rz)) return null;

        return blockStates.get(pack(rx, ry, rz));
    }

    public @Nullable BlockState getBlockState(int rx, int ry, int rz, @Nullable BlockStatePool pool) {
        BlockState state = getBlockStateUnsafe(rx, ry, rz);

        return state == null ? null : state.clone(pool);
    }

    public void setBlockState(int rx, int ry, int rz, @Nullable BlockState state) {
        if (state == null) {
            blockStates.remove(pack(rx, ry, rz));
            presence.clear(pack(rx, ry, rz));
        } else {
            blockStates.put(pack(rx, ry, rz), state.clone(null));
            presence.set(pack(rx, ry, rz));
        }
    }

    public boolean isEmpty() {
        return blockStates.isEmpty();
    }

    @SneakyThrows
    @Override
    public BlockStateStorage clone() {
        BlockStateStorage copy = (BlockStateStorage) super.clone();

        copy.blockStates = copy.blockStates.clone();
        copy.presence = (BitSet) copy.presence.clone();

        copy.blockStates.replaceAll((loc, state) -> state.clone(null));

        return copy;
    }

    public static @Nullable BlockStateStorage getStorage(ExtendedBlockStorage ebs, boolean createIfMissing) {
        if (ebs == null) return null;
        if (!(ebs instanceof BlockState_EBSExt ext)) return null;

        BlockStateStorage storage = ext.gtnhlib$getBlockStateStorage();

        if (createIfMissing && storage == null) {
            storage = new BlockStateStorage();
            ext.gtnhlib$setBlockStateStorage(storage);
        }

        return storage;
    }

    public static boolean removeStorage(ExtendedBlockStorage ebs) {
        if (ebs == null) return false;
        if (!(ebs instanceof BlockState_EBSExt ext)) return false;

        if (ext.gtnhlib$getBlockStateStorage() != null) {
            ext.gtnhlib$setBlockStateStorage(null);
            return true;
        } else {
            return false;
        }
    }
}
