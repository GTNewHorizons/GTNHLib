package com.gtnewhorizon.gtnhlib.blockstate.core;

import com.gtnewhorizon.gtnhlib.util.IObjectPool;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/// A reusable pool of [BlockStateImpl] instances to reduce allocation pressure in hot paths.
///
/// Not thread-safe. Intended for single-thread use (e.g. render thread or server tick thread).
/// Do not share a pool across threads. Don't bother putting these in a ThreadLocal or behind
/// synchronization, it's probably not worth it.
public class BlockStatePool implements IObjectPool<BlockStateImpl> {

    private final ObjectArrayList<BlockStateImpl> availableInstances = new ObjectArrayList<>();
    private final int maxInstances;

    public BlockStatePool() {
        this(32);
    }

    public BlockStatePool(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    @Override
    public BlockStateImpl getInstance() {
        if (this.availableInstances.isEmpty()) {
            return new BlockStateImpl();
        }

        // noinspection resource
        return this.availableInstances.remove(this.availableInstances.size() - 1).assertIsDefault();
    }

    @Override
    public void releaseInstance(BlockStateImpl instance) {
        if (instance == null) return;

        if (this.availableInstances.size() < maxInstances) {
            this.availableInstances.add(instance.reset());
        }
    }
}
