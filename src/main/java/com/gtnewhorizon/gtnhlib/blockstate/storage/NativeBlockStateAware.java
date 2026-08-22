package com.gtnewhorizon.gtnhlib.blockstate.storage;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

/// Implemented on [Block]s that can have a [BlockState] stored in the native block state storage system.
/// Blocks without this interface will never have a native [BlockState].
/// <p>
/// When a block implements this interface, the [BlockState] in a [BlockStateStorage] is always the source-of-truth for
/// a given block property's value. [#getMetaForState(BlockState, int)] is used to update the metadata value in the world so
/// that meta block properties work transparently. Non-meta block properties MUST use the [BlockState] in the
/// [BlockStateStorage] as their backing data source - relying on other fields (such as [TileEntity] fields) for their
/// value is undefined behaviour, and likely will not work well.
/// <p>
/// Implementing this interface on a [Block] precludes third-party property registration via
/// [BlockPropertyRegistry]. The two systems are mutually exclusive and cannot operate in tandem.
public interface NativeBlockStateAware {

    /// Gets the default state for a [Block]. Must create a new instance (optionally from the pool, if one is provided).
    @Contract("_ -> new")
    BlockState getDefaultState(@Nullable BlockStatePool pool);

    /// Called when a [Block] changes in the world to determine whether the next block is logically equivalent to the
    /// current block (`this`). When this returns false, the [BlockState] will be discarded and optionally re-created if
    /// the new block also implements [NativeBlockStateAware].
    /// If this returns true, `newBlock` must also implement [NativeBlockStateAware].
    /// This function must be commutative - `this.isSameBlock(other) == other.isSameBlock(this)` must always be true.
    default boolean isSameBlock(Block newBlock) {
        return newBlock == this;
    }

    /// Called when a [Block] is initially placed into the world, to determine what [BlockState] should be used. This is
    /// primarily used for third-party compatibility, in case something places your block with a specific meta value.
    default BlockState getInitialState(@Nullable BlockStatePool pool, Block placedBlock, int placedMeta) {
        return getDefaultState(pool);
    }

    /// Called when the block or metadata for a location changes, without changing its identity. This is only called when
    /// [#isSameBlock(Block)] returns true.
    /// @returns Null when nothing should be updated, or a new [BlockState] otherwise
    @Nullable
    default BlockState onBlockChanged(@Nullable BlockStatePool pool, BlockState state, Block oldBlock, Block newBlock, int oldMeta, int newMeta) {
        return null;
    }

    /// Gets the [Block] for a given [BlockState], when the [BlockState] is placed into a [World].
    default Block getBlockForState(BlockState state) {
        return state.getBlock();
    }

    /// Gets the metadata for a given [BlockState], when the [BlockState] is placed into a [World].
    default int getMetaForState(BlockState state) {
        return state.getBlockMeta(0);
    }
}
