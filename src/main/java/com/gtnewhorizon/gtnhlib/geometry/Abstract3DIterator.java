package com.gtnewhorizon.gtnhlib.geometry;

import java.util./* List */Iterator;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

public abstract class Abstract3DIterator implements /* List */Iterator<Void> {

    public int n;
    public int l;
    public int m;

    public int range;

    public int startX;
    public int startY;
    public int startZ;

    public Abstract3DIterator(int range) {
        this.range = range;
    }

    public Abstract3DIterator(int range, int x, int y, int z) {
        this.range = range;
        startX = x;
        startY = y;
        startZ = z;
    }

    public abstract void __next();

    // public abstract void __previous();

    public final Void next() {
        __next();
        return null;
    }

    /*
     * public final Void previous() { __previous(); return null; } public final void add(Void v) {} public final void
     * set(Void v) {} public final void remove() {} public int nextIndex() { return 0; } public int previousIndex() {
     * return 0; }
     */

    public final int[] nextCoordTriple() {
        __next();
        return new int[] { startX + n, startY + l, startZ + m };
    }

    public final BlockPos nextBlockPos() {
        __next();
        return new BlockPos(startX + n, startY + l, startZ + m);
    }

    /*
     * public final int[] prevCoordTriple() { __previous(); return new int[] { n , l , m }; } public final BlockPos
     * prevBlockPos() { __previous(); return new BlockPos(n, l, m); }
     */

}
