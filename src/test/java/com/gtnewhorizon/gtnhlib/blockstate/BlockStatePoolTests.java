package com.gtnewhorizon.gtnhlib.blockstate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateImpl;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;

class BlockStatePoolTests {

    @Test
    void emptyPool_getInstance_returnsNonNull() {
        BlockStatePool pool = new BlockStatePool();

        Assertions.assertNotNull(pool.getInstance());
    }

    @Test
    void releaseAndGet_returnsSameInstance() {
        BlockStatePool pool = new BlockStatePool();
        BlockStateImpl state = new BlockStateImpl();

        pool.releaseInstance(state);

        Assertions.assertSame(state, pool.getInstance());
    }

    @Test
    void releaseInstance_resetsState() {
        BlockStatePool pool = new BlockStatePool();
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");

        pool.releaseInstance(state);

        BlockStateImpl retrieved = pool.getInstance();
        Assertions.assertNull(retrieved.getPropertyValue("foo", true));
    }

    /// When pool capacity is 1, releasing a second instance must not store it.
    @Test
    void releaseOverMax_extraInstanceDiscarded() {
        BlockStatePool pool = new BlockStatePool(1);
        BlockStateImpl first = new BlockStateImpl();
        BlockStateImpl second = new BlockStateImpl();

        pool.releaseInstance(first);
        pool.releaseInstance(second);

        // first was stored; second was discarded
        Assertions.assertSame(first, pool.getInstance());
        // pool is now empty: next get creates a new instance, not second
        Assertions.assertNotSame(second, pool.getInstance());
    }

    @Test
    void releaseNull_doesNotThrow() {
        BlockStatePool pool = new BlockStatePool();

        Assertions.assertDoesNotThrow(() -> pool.releaseInstance(null));
    }

    /// close() on a BlockStateImpl created without a pool must not throw.
    @Test
    void closeNonPooledState_doesNotThrow() {
        BlockStateImpl state = new BlockStateImpl();

        Assertions.assertDoesNotThrow(state::close);
    }
}
