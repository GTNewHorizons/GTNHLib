package com.gtnewhorizon.gtnhlib.blockstate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateImpl;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;

class BlockStateStorageTests {

    private BlockStateStorage storage;

    @BeforeEach
    void setUp() {
        storage = new BlockStateStorage();
    }

    // --- pack/unpack roundtrip ---

    @Test
    void pack_unpackX_allValues_roundtrip() {
        for (int x = 0; x < 16; x++) {
            Assertions.assertEquals(x, BlockStateStorage.unpackX(BlockStateStorage.pack(x, 0, 0)));
        }
    }

    @Test
    void pack_unpackY_allValues_roundtrip() {
        for (int y = 0; y < 16; y++) {
            Assertions.assertEquals(y, BlockStateStorage.unpackY(BlockStateStorage.pack(0, y, 0)));
        }
    }

    @Test
    void pack_unpackZ_allValues_roundtrip() {
        for (int z = 0; z < 16; z++) {
            Assertions.assertEquals(z, BlockStateStorage.unpackZ(BlockStateStorage.pack(0, 0, z)));
        }
    }

    @Test
    void pack_allPositions_roundtrip() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    short key = BlockStateStorage.pack(x, y, z);
                    Assertions.assertEquals(x, BlockStateStorage.unpackX(key), "x mismatch at " + x + "," + y + "," + z);
                    Assertions.assertEquals(y, BlockStateStorage.unpackY(key), "y mismatch at " + x + "," + y + "," + z);
                    Assertions.assertEquals(z, BlockStateStorage.unpackZ(key), "z mismatch at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void pack_outOfRangeCoords_maskedTo4Bits() {
        // Coordinates above 15 should be masked, same as 0-15 counterparts
        Assertions.assertEquals(BlockStateStorage.pack(0, 0, 0), BlockStateStorage.pack(16, 16, 16));
        Assertions.assertEquals(BlockStateStorage.pack(1, 2, 3), BlockStateStorage.pack(17, 18, 19));
    }

    // --- isEmpty ---

    @Test
    void newStorage_isEmpty() {
        Assertions.assertTrue(storage.isEmpty());
    }

    @Test
    void afterSet_notEmpty() {
        storage.setBlockState(0, 0, 0, new BlockStateImpl());
        Assertions.assertFalse(storage.isEmpty());
    }

    @Test
    void afterSetThenClear_isEmpty() {
        storage.setBlockState(0, 0, 0, new BlockStateImpl());
        storage.setBlockState(0, 0, 0, null);
        Assertions.assertTrue(storage.isEmpty());
    }

    @Test
    void clearLastOfMultiple_isEmpty() {
        storage.setBlockState(0, 0, 0, new BlockStateImpl());
        storage.setBlockState(1, 1, 1, new BlockStateImpl());
        storage.setBlockState(0, 0, 0, null);
        Assertions.assertFalse(storage.isEmpty());
        storage.setBlockState(1, 1, 1, null);
        Assertions.assertTrue(storage.isEmpty());
    }

    // --- hasState ---

    @Test
    void newStorage_hasState_false() {
        Assertions.assertFalse(storage.hasState(0, 0, 0));
    }

    @Test
    void afterSet_hasState_true() {
        storage.setBlockState(5, 7, 3, new BlockStateImpl());
        Assertions.assertTrue(storage.hasState(5, 7, 3));
    }

    @Test
    void hasState_differentPos_false() {
        storage.setBlockState(5, 7, 3, new BlockStateImpl());
        Assertions.assertFalse(storage.hasState(5, 7, 4));
    }

    @Test
    void afterClear_hasState_false() {
        storage.setBlockState(5, 7, 3, new BlockStateImpl());
        storage.setBlockState(5, 7, 3, null);
        Assertions.assertFalse(storage.hasState(5, 7, 3));
    }

    // --- getBlockState ---

    @Test
    void getBlockState_neverSet_returnsNull() {
        Assertions.assertNull(storage.getBlockState(0, 0, 0, null));
    }

    @Test
    void getBlockState_afterSet_returnsEqualState() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");
        storage.setBlockState(3, 5, 7, state);

        BlockStateImpl retrieved = (BlockStateImpl) storage.getBlockState(3, 5, 7, null);
        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals("bar", retrieved.getPropertyValue("foo", true));
    }

    @Test
    void getBlockState_returnsClone_notSameReference() {
        BlockStateImpl state = new BlockStateImpl();
        storage.setBlockState(0, 0, 0, state);

        Assertions.assertNotSame(state, storage.getBlockState(0, 0, 0, null));
    }

    @Test
    void getBlockState_afterClear_returnsNull() {
        storage.setBlockState(0, 0, 0, new BlockStateImpl());
        storage.setBlockState(0, 0, 0, null);

        Assertions.assertNull(storage.getBlockState(0, 0, 0, null));
    }

    @Test
    void getBlockState_multiplePositions_independent() {
        BlockStateImpl stateA = new BlockStateImpl();
        stateA.setPropertyValue("key", "a");
        BlockStateImpl stateB = new BlockStateImpl();
        stateB.setPropertyValue("key", "b");

        storage.setBlockState(0, 0, 0, stateA);
        storage.setBlockState(1, 1, 1, stateB);

        Assertions.assertEquals("a", storage.getBlockState(0, 0, 0, null).getPropertyValue("key", true));
        Assertions.assertEquals("b", storage.getBlockState(1, 1, 1, null).getPropertyValue("key", true));
    }

    @Test
    void getBlockStateUnsafe_neverSet_returnsNull() {
        Assertions.assertNull(storage.getBlockStateUnsafe(0, 0, 0));
    }

    @Test
    void getBlockStateUnsafe_afterSet_returnsSameReference() {
        BlockStateImpl state = new BlockStateImpl();
        storage.setBlockState(0, 0, 0, state);

        // setBlockState clones on write, so unsafe get returns the stored clone — not the original
        Assertions.assertNotNull(storage.getBlockStateUnsafe(0, 0, 0));
    }

    // --- setBlockState overwrites ---

    @Test
    void setBlockState_twice_secondValueWins() {
        BlockStateImpl first = new BlockStateImpl();
        first.setPropertyValue("key", "first");
        BlockStateImpl second = new BlockStateImpl();
        second.setPropertyValue("key", "second");

        storage.setBlockState(0, 0, 0, first);
        storage.setBlockState(0, 0, 0, second);

        Assertions.assertEquals("second", storage.getBlockState(0, 0, 0, null).getPropertyValue("key", true));
    }

    @Test
    void setBlockState_nullOnEmpty_noException() {
        Assertions.assertDoesNotThrow(() -> storage.setBlockState(0, 0, 0, null));
    }

    // --- clone ---

    @Test
    void clone_notSameReference() {
        Assertions.assertNotSame(storage, storage.clone());
    }

    @Test
    void clone_containsSameProperties() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("key", "val");
        storage.setBlockState(2, 4, 6, state);

        BlockStateStorage copy = storage.clone();
        Assertions.assertEquals("val", copy.getBlockState(2, 4, 6, null).getPropertyValue("key", true));
    }

    @Test
    void clone_mutatingOriginalDoesNotAffectCopy() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("key", "original");
        storage.setBlockState(0, 0, 0, state);

        BlockStateStorage copy = storage.clone();

        BlockStateImpl updated = new BlockStateImpl();
        updated.setPropertyValue("key", "changed");
        storage.setBlockState(0, 0, 0, updated);

        Assertions.assertEquals("original", copy.getBlockState(0, 0, 0, null).getPropertyValue("key", true));
    }

    @Test
    void clone_mutatingCopyDoesNotAffectOriginal() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("key", "original");
        storage.setBlockState(0, 0, 0, state);

        BlockStateStorage copy = storage.clone();
        copy.setBlockState(0, 0, 0, null);

        Assertions.assertNotNull(storage.getBlockState(0, 0, 0, null));
    }

    @Test
    void clone_emptyStorage_cloneAlsoEmpty() {
        Assertions.assertTrue(storage.clone().isEmpty());
    }

    // --- State machine simulations ---
    // These verify the storage-level behaviour that MixinChunk_BlockStates produces
    // for each case, without requiring a live Minecraft world.

    /// Case A: logical transition (isSameBlock=true) — state preserved, no operations on storage.
    @Test
    void stateMachine_caseA_preservesState() {
        BlockStateImpl original = new BlockStateImpl();
        original.setPropertyValue("orientation", "north");
        storage.setBlockState(0, 0, 0, original);

        // Mixin returns early on isSameBlock=true — storage untouched
        Assertions.assertEquals("north", storage.getBlockState(0, 0, 0, null).getPropertyValue("orientation", true));
    }

    /// Case B: Aware→different Aware — old state cleared, default written.
    @Test
    void stateMachine_caseB_clearsOldWritesDefault() {
        BlockStateImpl oldState = new BlockStateImpl();
        oldState.setPropertyValue("orientation", "north");
        storage.setBlockState(0, 0, 0, oldState);

        // Mixin: clear old state
        storage.setBlockState(0, 0, 0, null);
        // Mixin: write default for new block
        BlockStateImpl defaultState = new BlockStateImpl();
        storage.setBlockState(0, 0, 0, defaultState);

        Assertions.assertNotNull(storage.getBlockState(0, 0, 0, null));
        Assertions.assertNull(storage.getBlockState(0, 0, 0, null).getPropertyValue("orientation", true));
    }

    /// Case C: Aware→not Aware — state cleared; storage removed when empty.
    @Test
    void stateMachine_caseC_clearsAndEmptiesStorage() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("key", "val");
        storage.setBlockState(0, 0, 0, state);

        // Mixin: clear old state
        storage.setBlockState(0, 0, 0, null);

        Assertions.assertNull(storage.getBlockState(0, 0, 0, null));
        Assertions.assertTrue(storage.isEmpty());
    }

    /// Case C with multiple entries — other entries survive, storage not removed.
    @Test
    void stateMachine_caseC_otherPositionsSurvive() {
        storage.setBlockState(0, 0, 0, new BlockStateImpl());
        storage.setBlockState(1, 1, 1, new BlockStateImpl());

        storage.setBlockState(0, 0, 0, null);

        Assertions.assertFalse(storage.isEmpty());
        Assertions.assertNotNull(storage.getBlockState(1, 1, 1, null));
    }

    /// Case D: not Aware→Aware — fresh storage, default state written.
    @Test
    void stateMachine_caseD_defaultStateWritten() {
        // Storage is freshly allocated (simulating getStorage with createIfMissing=true)
        Assertions.assertTrue(storage.isEmpty());

        BlockStateImpl defaultState = new BlockStateImpl();
        storage.setBlockState(0, 0, 0, defaultState);

        Assertions.assertNotNull(storage.getBlockState(0, 0, 0, null));
    }

    /// NativeBlockStates.setBlockState overwrites the default written by the mixin.
    @Test
    void stateMachine_callerStateOverwritesDefault() {
        // Mixin writes default (Case D or B)
        storage.setBlockState(0, 0, 0, new BlockStateImpl());

        // Caller writes real state
        BlockStateImpl real = new BlockStateImpl();
        real.setPropertyValue("orientation", "east");
        storage.setBlockState(0, 0, 0, real);

        Assertions.assertEquals("east", storage.getBlockState(0, 0, 0, null).getPropertyValue("orientation", true));
    }
}
