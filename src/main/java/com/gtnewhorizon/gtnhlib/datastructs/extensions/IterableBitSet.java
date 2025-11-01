package com.gtnewhorizon.gtnhlib.datastructs.extensions;

import java.util.BitSet;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;

public class IterableBitSet extends BitSet implements IntIterable {

    @Override
    public @NotNull IntIterator iterator() {
        return new IntIterator() {

            private int current = nextSetBit(0);

            @Override
            public int nextInt() {
                int value = current;

                current = nextSetBit(current + 1);

                return value;
            }

            @Override
            public boolean hasNext() {
                return current >= 0;
            }

            @Override
            public void remove() {
                clear(current);
            }
        };
    }
}
