package com.gtnewhorizon.gtnhlib.util.map;

import java.util.BitSet;
import java.util.Iterator;

import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.google.common.collect.AbstractIterator;

public class BooleanArray3D extends BitSet implements Iterable<Vector3ic> {

    private final int spanx, spany, spanz, spanslice;

    public BooleanArray3D(int spanx, int spany, int spanz) {
        this.spanx = spanx;
        this.spany = spany;
        this.spanz = spanz;
        this.spanslice = spanx * spany;
    }

    public BooleanArray3D(int spanx, int spany, int spanz, boolean[] data) {
        this.spanx = spanx;
        this.spany = spany;
        this.spanz = spanz;
        this.spanslice = spanx * spany;

        for (int i = 0; i < data.length; i++) {
            if (data[i]) {
                set(i);
            }
        }
    }

    public void set(int x, int y, int z) {
        set(index(x, y, z));
    }

    public void clear(int x, int y, int z) {
        clear(index(x, y, z));
    }

    public boolean get(int x, int y, int z) {
        return get(index(x, y, z));
    }

    public Iterator<Vector3ic> iterator() {
        return new AbstractIterator<>() {

            private boolean init = false;
            private int index;
            private final Vector3i v = new Vector3i();

            @Override
            protected Vector3ic computeNext() {
                if (!init) {
                    init = true;
                    index = BooleanArray3D.this.nextSetBit(0);
                } else {
                    index = BooleanArray3D.this.nextSetBit(index + 1);
                }

                if (index == -1) {
                    this.endOfData();
                    return null;
                }

                v.set(index % spanx, (index / spanx) % spany, index / spanslice);

                return v;
            }
        };
    }

    private int index(int x, int y, int z) {
        return x + (y * spanx) + (z * spanslice);
    }
}
