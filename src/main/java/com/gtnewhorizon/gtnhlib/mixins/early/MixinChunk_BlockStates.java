package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;
import com.gtnewhorizon.gtnhlib.blockstate.storage.NativeBlockStateAware;
import com.gtnewhorizon.gtnhlib.util.EBSUtil;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

@Mixin(Chunk.class)
public class MixinChunk_BlockStates {

    @Unique
    private BlockState gtnhlib$tempState;

    @Inject(method = "func_150807_a", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/chunk/Chunk;getBlockMetadata(III)I", shift = Shift.AFTER, ordinal = 0))
    private void gtnhlib$checkMetaChanged(
        int x, int y, int z,
        Block newBlock, int newMeta,
        CallbackInfoReturnable<Boolean> cir,
        @Local(name = "block1") Block oldBlock, @Local(type = int.class, name = "k1") int oldMeta,
        @Local(type = Block.class, argsOnly = true, name = "p_150807_4_") LocalRef<Block> newBlockRef,
        @Local(type = int.class, argsOnly = true, name = "p_150807_5_") LocalIntRef newMetaRef
    ) {
        // When the metadata or block for a location changes, we need to hook into it in case `onBlockChanged` needs to be called.
        // This will also intercept the provided block/meta and change it according to the [BlockState].
        // This breaks a set->get assumption for Chunks (the set block will always be what's fetched on the next get) so I'm not sure this is worth it.
        // In theory, the state should properly handle all mutations (i.e. changing to a lit Block from an unlit Block should set the lit property to true, which causes the lit Block to be placed) but it's very easy to get this wrong.

        Chunk self = (Chunk) (Object) this;

        boolean oldAware = oldBlock instanceof NativeBlockStateAware;
        boolean newAware = newBlock instanceof NativeBlockStateAware;

        if (!oldAware && !newAware) return;

        if (oldBlock instanceof NativeBlockStateAware bsa && newAware && bsa.isSameBlock(newBlock)) {
            // Old and new blocks are logically equivalent.
            // Call onBlockChanged and optionally update the state.

            ExtendedBlockStorage ebs = EBSUtil.getEBS(self, y >> 4);
            BlockStateStorage storage = BlockStateStorage.getStorage(ebs, true);

            BlockState state = storage.getBlockStateUnsafe(x & 0xF, y & 0xF, z & 0xF);

            if (state == null) {
                state = bsa.getDefaultState(null);
            }

            BlockState newState = bsa.onBlockChanged(null, state, oldBlock, newBlock, oldMeta, newMeta);

            if (newState == null) {
                newState = state;
            }

            storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, newState);

            newBlockRef.set(bsa.getBlockForState(newState));
            newMetaRef.set(bsa.getMetaForState(newState));
        } else if (newBlock instanceof NativeBlockStateAware bsa) {
            // New block is unrelated to the old block.
            // Fetch its initial state, cache it, and intercept the requested block/meta for the correct values.
            gtnhlib$tempState = bsa.getInitialState(null, newBlock, newMeta);

            newBlockRef.set(bsa.getBlockForState(gtnhlib$tempState));
            newMetaRef.set(bsa.getMetaForState(gtnhlib$tempState));
        }
    }

    @Inject(method = "func_150807_a", at = @At(value = "RETURN"))
    private void gtnhlib$resetTempState(
        int x, int y, int z,
        Block newBlock, int newMeta,
        CallbackInfoReturnable<Boolean> cir
    ) {
        // Reset cached state to avoid memory leaks/logic bugs
        gtnhlib$tempState = null;
    }

    @Inject(method = "func_150807_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBlockAdded(Lnet/minecraft/world/World;III)V", shift = Shift.BEFORE))
    private void gtnhlib$onBlockChanged(int x, int y, int z, Block newBlock, int newMetadata, CallbackInfoReturnable<Boolean> cir,
        @Local(name = "block1") Block oldBlock) {
        // This method detects newly placed [NativeBlockStateAware] Blocks and creates or removes the [BlockStateStorage] as needed.

        Chunk self = (Chunk) (Object) this;

        boolean oldAware = oldBlock instanceof NativeBlockStateAware;
        boolean newAware = newBlock instanceof NativeBlockStateAware;

        // Neither is aware, bail since we don't care about them
        if (!oldAware && !newAware) return;

        // Logical transition (e.g. furnace <-> lit_furnace) — preserve existing state
        if (oldAware && newAware && ((NativeBlockStateAware) oldBlock).isSameBlock(newBlock)) return;

        // Get storage; create it only if the incoming block will need it
        ExtendedBlockStorage ebs = EBSUtil.getEBS(self, y >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, newAware);

        // Clear the outgoing block's state since the two blocks are logically different.
        // We don't want to pass the previous block's state to the new block
        if (oldAware && storage != null) {
            storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, null);

            // Incoming block is not Aware — remove storage if now empty
            if (!newAware && storage.isEmpty()) {
                BlockStateStorage.removeStorage(ebs);
            }
        }

        // Install the default state for the incoming block.
        // This ensures a valid state is present even when a third party places a
        // BlockStateAware block via world.setBlock directly. NativeBlockStates will
        // overwrite this with the caller-supplied state immediately after setBlock returns.
        if (newAware && storage != null) {
            NativeBlockStateAware bsa = (NativeBlockStateAware) newBlock;

            // Consume the cached default state, or fetch another one if there is none
            if (gtnhlib$tempState == null) {
                gtnhlib$tempState = bsa.getDefaultState(null);
            }

            storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, gtnhlib$tempState);

            gtnhlib$tempState = null;
        }
    }
}
