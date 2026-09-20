package com.gtnewhorizon.gtnhlib.datastructs.extensions;

import java.util.BitSet;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;

public class IterableBitSet extends BitSet implements IntIterable {

    @Override
    public @NotNull IntIterator iterator() {
        return new IntIterator() {

            private int current = nextSetBit(0);
            private int lastReturned = -1;

            @Override
            public int nextInt() {
                if (current < 0) {
                    throw new NoSuchElementException();
                }
                lastReturned = current;
                current = nextSetBit(current + 1);
                return lastReturned;
            }

            @Override
            public boolean hasNext() {
                return current >= 0;
            }

            @Override
            public void remove() {
                if (lastReturned < 0) {
                    throw new IllegalStateException();
                }
                
                clear(lastReturned);
                lastReturned = -1;
            }
        };
    }
}
