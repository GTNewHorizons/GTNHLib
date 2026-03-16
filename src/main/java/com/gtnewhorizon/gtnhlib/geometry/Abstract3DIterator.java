package com.gtnewhorizon.gtnhlib.geometry;

// import java.util./* List */Iterator;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

/**
 * An iterator through positions in 3D space, typically starting at relative <0,0,0> and going outward. To use, call
 * {@link #next()} and then take the iterator's n, l, and m fields, using them as arbitrary coordinates. Offset
 * coordinates can be optionally supplied; n is treated as x, l as y, and m as z in methods that use those. One reason
 * n, l, and m are not internally x, y, and z is to highlight that their ordering is arbitrary; swap or invert them if
 * you like. I initially named them n, l, and m as they increase similarly to the three electronic quantum numbers (you
 * can think of the sign of n or l as the spin). In {@link com.gtnewhorizon.gtnhlib.blockpos.CubeIterator}, from which
 * this was adapted, aside from the symmetry transformations, m is bounded by l and l is bounded by n, similarly to
 * electron orbitals, in order to get unique non-ordered {n,l,m} sets.
 *
 * @author __felix__
 */
public abstract class Abstract3DIterator { // implements /* List */Iterator<Void> {

    /**
     * One of the three coordinates ran through by an iterator. Default is X. Can be X, Y, or Z, depending on what
     * arbitrary order you would like to prioritize (Invert them if you would like to change the priority of neg vs
     * positive directions) <br>
     * Inspired by the principal quantum number; it limits the values of l and m.
     */
    public int n;
    /**
     * One of the three coordinates ran through by an iterator. Default is Y. Can be X, Y, or Z, depending on what
     * arbitrary order you would like to prioritize (Invert them if you would like to change the priority of neg vs
     * positive directions) <br>
     * Inspired by the azimuthal quantum number; it limits the value of m and can only go up to n.
     */
    public int l;
    /**
     * One of the three coordinates ran through by an iterator. Default is Z. Can be X, Y, or Z, depending on what
     * arbitrary order you would like to prioritize (Invert them if you would like to change the priority of neg vs
     * positive directions) <br>
     * Inspired by the magnetic quantum number; it can only go up to l.
     */
    public int m;

    /**
     * The maximum distance or radius that the iterator can go through, used in {@link #hasNext()}.
     */
    public int range;

    /**
     * Whether the iterator is not specifically at the last value before it exceeds {@link #range}. Does NOT have to be
     * truthful for values *beyond* that last value (and isn't in current impl).
     *
     * @return Whether the iterator is at the last value before it exceeds the provided range
     */
    public abstract boolean hasNext();

    // public boolean hasPrevious();

    /**
     * The (optional) starting X coordinate, used with {@link #nextCoordTriple()} and {@link #nextBlockPos()}.
     */
    public int startX;
    /**
     * The (optional) starting Y coordinate, used with {@link #nextCoordTriple()} and {@link #nextBlockPos()}.
     */
    public int startY;
    /**
     * The (optional) starting Z coordinate, used with {@link #nextCoordTriple()} and {@link #nextBlockPos()}.
     */
    public int startZ;

    /**
     * Initializes the iterator with only the max range. If you don't want a max range, put whatever here and ignore
     * {@link #hasNext()}.
     *
     * @param range the maximum radius the iterator goes up to, used in {@link #hasNext()}
     */
    public Abstract3DIterator(int range) {
        this.range = range;
    }

    /**
     * Initializes the iterator with the max range and starting XYZ values. If you don't want a max range, put whatever
     * here and ignore {@link #hasNext()}. The XYZ values are not factored into {@link #n}, {@link #l}, or {@link #m};
     * those are only starting offsets. They are used in {@link #nextCoordTriple()} and {@link #nextBlockPos()}.
     *
     * @param range the maximum radius the iterator goes up to, used in {@link #hasNext()}
     * @param x     the x offset used in {@link #nextCoordTriple()} and {@link #nextBlockPos()}
     * @param x     the y offset used in {@link #nextCoordTriple()} and {@link #nextBlockPos()}
     * @param x     the z offset used in {@link #nextCoordTriple()} and {@link #nextBlockPos()}
     */
    public Abstract3DIterator(int range, int x, int y, int z) {
        this.range = range;
        startX = x;
        startY = y;
        startZ = z;
    }

    /**
     * The INTERNAL implementation of {@link #next()}.&nbsp;Override this when making a subclass. (Unless you want to
     * have to spam `return null` everywhere...)
     * @hidden
     * @see #next()
     */
    // protected abstract void __next();

    // protected abstract void __previous();

    /**
     * Progresses the iterator ({@link n}, {@link l}, {@link m} values), but does not directly return them. This is
     * intended so that you don't have to spin up an entire object if you don't want to
     *
     * <!-- @return returns null just to comply with Iterator<E> -->
     */
    public abstract void next();

    /*
     * public final Void previous() { __previous(); return null; } public final void add(Void v) {} public final void
     * set(Void v) {} public final void remove() {} public int nextIndex() { return 0; } public int previousIndex() {
     * return 0; }
     */

    /**
     * Returns the next coords as an array, using the default of x for n, etc., offset by the optional start values. The
     * start values don't have to be set for this to be used (they default to 0). Using the fields directly or their
     * getters is better than spinning up an array in most cases. Also note that this method *progresses* the iterator.
     *
     * @return an array containing the next xyz coordinates, offset if offsets were provided in init
     */
    public final int[] nextCoordTriple() {
        next();
        return new int[] { startX + n, startY + l, startZ + m };
    }

    /**
     * Returns the next coords as a {@link com.gtnewhorizon.gtnhlib.blockpos.BlockPos}, using the default of x for n,
     * etc., offset by the optional start values. The start values don't have to be set for this to be used (they
     * default to 0). Using the fields directly or their getters is usually better than this. Also note that this method
     * progresses the iterator.
     *
     * @return a BlockPos of the next xyz coordinates, offset if offsets were provided in init
     */
    public final BlockPos nextBlockPos() {
        next();
        return new BlockPos(startX + n, startY + l, startZ + m);
    }

    /*
     * public final int[] prevCoordTriple() { __previous(); return new int[] { n , l , m }; } public final BlockPos
     * prevBlockPos() { __previous(); return new BlockPos(n, l, m); }
     */

    /**
     * Getter for {@link #n}
     *
     * @return {@link #n}
     */
    public final int getN() {
        return n;
    }

    /**
     * Getter for {@link #l}
     *
     * @return {@link #l}
     */
    public final int getL() {
        return l;
    }

    /**
     * Getter for {@link #m}
     *
     * @return {@link #m}
     */
    public final int getM() {
        return m;
    }

    /**
     * Getter for {@link #range}
     *
     * @return {@link #range}
     */
    public final int getRange() {
        return range;
    }

    /**
     * Getter for {@link #startX}
     *
     * @return {@link #startX}
     */
    public final int getStartX() {
        return startX;
    }

    /**
     * Getter for {@link #startY}
     *
     * @return {@link #startY}
     */
    public final int getStartY() {
        return startY;
    }

    /**
     * Getter for {@link #startZ}
     *
     * @return {@link #startZ}
     */
    public final int getStartZ() {
        return startZ;
    }

    /**
     * Setter for {@link #n}
     *
     * @param n {@link #n}
     */
    public final void setN(int n) {
        this.n = n;
    }

    /**
     * Setter for {@link #l}
     *
     * @param l {@link #l}
     */
    public final void setL(int l) {
        this.l = l;
    }

    /**
     * Setter for {@link #m}
     *
     * @param m {@link #m}
     */
    public final void setM(int m) {
        this.m = m;
    }

    /**
     * Setter for {@link #range}
     *
     * @param range {@link #range}
     */
    public final void setRange(int range) {
        this.range = range;
    }

    /**
     * Setter for {@link #startX}
     *
     * @param startX {@link #startX}
     */
    public final void setStartX(int startX) {
        this.startX = startX;
    }

    /**
     * Setter for {@link #startY}
     *
     * @param startY {@link #startY}
     */
    public final void setStartY(int startY) {
        this.startY = startY;
    }

    /**
     * Setter for {@link #startZ}
     *
     * @param startZ {@link #startZ}
     */
    public final void setStartZ(int startZ) {
        this.startZ = startZ;
    }

    /**
     * Outputs the current iterator relative position as a string, for debugging.
     *
     * @return &quot;&lt;n,l,m&gt;&quot;
     */
    public String getCurrentRelativePos() {
        return String.format("<%d,%d,%d>", n, l, m);
    }

    /**
     * Outputs the current iterator absolute position as a string, for debugging.
     *
     * @return &quot;&lt;startX+n,startY+l,startZ+m&gt;&quot;
     */
    public String getCurrentAbsolutePos() {
        return String.format("<%d,%d,%d>", startX + n, startY + l, startZ + m);
    }

    /**
     * @hidden
     */
    @Override
    public String toString() {
        return getClass().getName() + "@" + getCurrentRelativePos();
    }

    /**
     * Progresses the iterator and gives the coordinates of n, l, and m, together, truncated to signed 21 bit. For when
     * you know the values of n, l, and m are going to be small, and don't want to spam arrays, since each iteration
     * with {@link #nextCoordTriple()} makes a whole new array. Output format is 0 + sign of m + last 20 bits of m +
     * sign of l + last 20 bits of l + etc. for n
     *
     * @return n, l, and m, truncated with sign as s21 ints, added together with offsets, fitting into a long
     */
    public final long nextAs21Bit() {
    next();
        return (long) (n & 0x000FFFFF | (n & -0x80000000) >> 11) | (long) (l & 0x000FFFFF) << 21
                | (long) (l & -0x80000000) << 10
                | (long) (m & 0x000FFFFF) << 42
                | (long) (m & -0x80000000) << 31;
    }

}
