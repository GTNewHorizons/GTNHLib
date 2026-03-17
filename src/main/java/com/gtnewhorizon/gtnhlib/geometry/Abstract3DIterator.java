package com.gtnewhorizon.gtnhlib.geometry;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

/**
 * An iterator through positions in 3D space, typically starting at relative <0,0,0> and going outward. To use, call
 * {@link #next()} and then take the iterator's n, l, and m fields, using them as arbitrary coordinates. Offset
 * coordinates can be optionally supplied; n is treated as x, l as y, and m as z in methods that use those. One reason
 * n, l, and m are not internally x, y, and z is to highlight that their ordering is arbitrary; swap or invert them if
 * you like. I initially named them n, l, and m as they increase similarly to the three electronic quantum numbers (you
 * can think of the sign of n or l as the spin). In {@link com.gtnewhorizon.gtnhlib.blockpos.CubeIterator}, from which
 * this was adapted, aside from the symmetry transformations, m is bounded by l and l is bounded by n, similarly to
 * electron orbitals, in order to get unique non-ordered {n,l,m} sets. <br>
 * It is recommended to use direct field access, functions like {#getX()}, or a function such as
 * {@link #nextNLMPacked()} to avoid object spam.
 *
 * @author __felix__
 */
public abstract class Abstract3DIterator {

    /**
     * One of the three coordinates ran through by an iterator. Default is X. Can be X, Y, or Z, depending on what
     * arbitrary order you would like to prioritize. This should be the **first to be increased** value in implementing
     * classes (after symmetry operations). <br>
     * Inspired by the principal quantum number; it limits the values of l and m in CubeIterator.
     */
    public int n;
    /**
     * One of the three coordinates ran through by an iterator. Default is Y. Can be X, Y, or Z, depending on what
     * arbitrary order you would like to prioritize. This should be the **second to be increased** value in implementing
     * classes (after symmetry operations). <br>
     * Inspired by the azimuthal quantum number; it limits the value of m and can only go up to n in CubeIterator.
     */
    public int l;
    /**
     * One of the three coordinates ran through by an iterator. Default is Z. Can be X, Y, or Z, depending on what
     * arbitrary order you would like to prioritize. This should be the **last to be increased** value in implementing
     * classes (after symmetry operations). <br>
     * Inspired by the magnetic quantum number; it can only go up to l in CubeIterator.
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

    // public abstract boolean hasPrevious();

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
     * An {@link #Ordering} that applies to the order that values are increased. The default ordering is that X is the
     * first value to be increased, and positive goes before negative. This changes the output of functions such as
     * {@link #getX()} (anything that says "XYZ") If you want to ignore this, just use `iter.n`, `iter.l`, `iter.m` for
     * whichever X, Y, Z coord you want.
     */
    public int order = Ordering.XYZ;

    /**
     * An "enum" for the arbitrary ordering of X, Y, and Z coords. Capital letter means positive then negative,
     * lowercase means other way around. Ordering of coordinates affects priority (for example in a typical
     * implementation for range = 1 with Ordering.XYZ it goes +x, -x, +y, -y, +z, -z, +x+y, +x-y, -x+y, etc.) This does
     * not define a way to change the order in which the mirroring operation happens (so for example you can't do +x,
     * -y, -x, +y), as that could mess up subclasses. (So you can do North-South-Down-Up-West-East, but not
     * North-Up-South-East-Down-West). <br>
     * The format is three nibbles stuck together, the first bit of each being the sign (1 for neg first, 0 for pos
     * first) and the second three bits being which of n, l, m it uses (100 = n, 010 = l, 001 = m). This assumes that
     * implementing classes have decreasing priority for n, l, and m.
     */
    public static final class Ordering {

        public static final byte N = 0b100;
        public static final byte L = 0b010;
        public static final byte M = 0b001;
        public static final byte minus = 8;

        public static final byte X = 8;
        public static final byte Y = 4;
        public static final byte Z = 0;

        public static final short xyz = 0b1100_1010_1001;
        public static final short xyZ = 0b1100_1010_0001;
        public static final short xYz = 0b1100_0010_1001;
        public static final short xYZ = 0b1100_0010_0001;
        public static final short Xyz = 0b0100_1010_1001;
        public static final short XyZ = 0b0100_1010_0001;
        public static final short XYz = 0b0100_0010_1001;
        public static final short XYZ = 0b0100_0010_0001;

        public static final short xzy = 0b1100_1001_1010;
        public static final short xZy = 0b1100_1001_0010;
        public static final short xzY = 0b1100_0001_1010;
        public static final short xZY = 0b1100_0001_0010;
        public static final short Xzy = 0b0100_1001_1010;
        public static final short XZy = 0b0100_1001_0010;
        public static final short XzY = 0b0100_0001_1010;
        public static final short XZY = 0b0100_0001_0010;

        public static final short yxz = 0b1010_1100_1001;
        public static final short yxZ = 0b1010_1100_0001;
        public static final short Yxz = 0b1010_0100_1001;
        public static final short YxZ = 0b1010_0100_0001;
        public static final short yXz = 0b0010_1100_1001;
        public static final short yXZ = 0b0010_1100_0001;
        public static final short YXz = 0b0010_0100_1001;
        public static final short YXZ = 0b0010_0100_0001;

        public static final short zxy = 0b1010_1001_1100;
        public static final short Zxy = 0b1010_1001_0100;
        public static final short zxY = 0b1010_0001_1100;
        public static final short ZxY = 0b1010_0001_0100;
        public static final short zXy = 0b0010_1001_1100;
        public static final short ZXy = 0b0010_1001_0100;
        public static final short zXY = 0b0010_0001_1100;
        public static final short ZXY = 0b0010_0001_0100;

        public static final short yzx = 0b1001_1100_1010;
        public static final short yZx = 0b1001_1100_0010;
        public static final short Yzx = 0b1001_0100_1010;
        public static final short YZx = 0b1001_0100_0010;
        public static final short yzX = 0b0001_1100_1010;
        public static final short yZX = 0b0001_1100_0010;
        public static final short YzX = 0b0001_0100_1010;
        public static final short YZX = 0b0001_0100_0010;

        public static final short zyx = 0b1001_1010_1100;
        public static final short Zyx = 0b1001_1010_0100;
        public static final short zYx = 0b1001_0010_1100;
        public static final short ZYx = 0b1001_0010_0100;
        public static final short zyX = 0b0001_1010_1100;
        public static final short ZyX = 0b0001_1010_0100;
        public static final short zYX = 0b0001_0010_1100;
        public static final short ZYX = 0b0001_0010_0100;

    }

    /**
     * Initializes the iterator with only the max range. If you don't want a max range, put whatever here and ignore
     * {@link #hasNext()}. Ordering defaults to X -> Y -> Z (pos -> neg for all).
     *
     * @param range the maximum radius the iterator goes up to, used in {@link #hasNext()}
     */
    public Abstract3DIterator(int range) {
        this.range = range;
    }

    /**
     * Same as {@link #Abstract3DIterator(int)} except this sets {@link #order}.
     */
    public Abstract3DIterator(int range, short order) {
        this.range = range;
        this.order = order;
    }

    /**
     * Initializes the iterator with the max range and starting XYZ values. If you don't want a max range, put whatever
     * here and ignore {@link #hasNext()}. The XYZ values are not factored into {@link #n}, {@link #l}, or {@link #m};
     * those are only starting offsets. They are used in {@link #nextCoordTriple()} and {@link #nextBlockPos()}.
     * Ordering defaults to X -> Y -> Z (pos -> neg for all).
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
     * Same as {@link #Abstract3DIterator(int, int, int, int)} except this sets {@link #order}.
     */
    public Abstract3DIterator(int range, short order, int x, int y, int z) {
        this.range = range;
        this.order = order;
        startX = x;
        startY = y;
        startZ = z;
    }

    /**
     * Progresses the iterator ({@link n}, {@link l}, {@link m} values), but does not directly return them. This is
     * intended so that you don't have to spin up an entire object if you don't want to
     */
    public abstract void next();

    /**
     * Returns the next coords as an array, offset by the optional start values. The start values don't have to be set
     * for this to be used (they default to 0). Using the fields directly or their getters is better than spinning up an
     * array in most cases. Also note that this method progresses the iterator.
     *
     * @return an array containing the next xyz coordinates, offset if offsets were provided in init
     */
    public final int[] nextCoordTriple() {
        next();
        return new int[] { getX(), getY(), getZ() };
    }

    /**
     * Returns the next coords as an array, not offset by the optional start values if they were set. Identical to
     * {@link #nextCoordTriple()} if offsets weren't set. Using the n, l, m fields directly or their getters is better
     * than spinning up an array in most cases. Also note that this method progresses the iterator.
     *
     * @return an array containing the next xyz coordinates, offset if offsets were provided in init
     */
    public final int[] nextRelativeCoordTriple() {
        next();
        return new int[] { getRelativeX(), getRelativeY(), getRelativeZ() };
    }

    /**
     * Returns the next coords as a {@link com.gtnewhorizon.gtnhlib.blockpos.BlockPos}, offset by the optional start
     * values. The start values don't have to be set for this to be used (they default to 0). Using the fields directly
     * or the XYZ getters is usually better than this. Also note that this method progresses the iterator.
     *
     * @return a BlockPos of the next xyz coordinates, offset if offsets were provided in init
     */
    public final BlockPos nextBlockPos() {
        next();
        return new BlockPos(getX(), getY(), getZ());
    }

    /**
     * Returns the next coords as a {@link com.gtnewhorizon.gtnhlib.blockpos.BlockPos}, not offset by the optional start
     * values. Only different if the startX, etc. fields were set. Using the n, l, m fields directly or the XYZ getters
     * is usually better than this. Also note that this method progresses the iterator.
     *
     * @return a BlockPos of the next relative xyz coordinates, not offset even if offsets were provided in init
     */
    public final BlockPos nextRelativeBlockPos() {
        next();
        return new BlockPos(getRelativeX(), getRelativeY(), getRelativeZ());
    }

    /**
     * Returns the relative X coordinate according to the XYZ {@link #order}.
     *
     * @return the relative X coordinate according to the arbitrary ordering
     */
    public final int getRelativeX() {
        byte tmp = (byte) (order >> Ordering.X);
        int ret = (tmp & Ordering.N) != 0 ? n : (tmp & Ordering.L) != 0 ? l : m;
        if ((tmp & Ordering.minus) != 0) ret = -ret;
        return ret;
    }

    /**
     * Returns the relative Y coordinate according to the XYZ {@link #order}.
     *
     * @return the relative Y coordinate according to the arbitrary ordering
     */
    public final int getRelativeY() {
        byte tmp = (byte) (order >> Ordering.Y);
        int ret = (tmp & Ordering.L) != 0 ? l : (tmp & Ordering.N) != 0 ? n : m;
        if ((tmp & Ordering.minus) != 0) ret = -ret;
        return ret;
    }

    /**
     * Returns the relative Z coordinate according to the XYZ {@link #order}.
     *
     * @return the relative Z coordinate according to the arbitrary ordering
     */
    public final int getRelativeZ() {
        // If for whatever reason Ordering.Z changes to not be 0, update this
        byte tmp = (byte) order;
        int ret = (tmp & Ordering.M) != 0 ? m : (tmp & Ordering.L) != 0 ? l : n;
        if ((tmp & Ordering.minus) != 0) ret = -ret;
        return ret;
    }

    /**
     * Returns the absolute X coordinate according to the XYZ {@link #order}. Only different if {@link #startX} was set.
     *
     * @return the absolute X coordinate according to the arbitrary ordering
     */
    public final int getX() {
        return startX + getRelativeX();
    }

    /**
     * Returns the absolute Y coordinate according to the XYZ {@link #order}. Only different if {@link #startY} was set.
     *
     * @return the absolute Y coordinate according to the arbitrary ordering
     */
    public final int getY() {
        return startY + getRelativeY();
    }

    /**
     * Returns the absolute Z coordinate according to the XYZ {@link #order}. Only different if {@link #startZ} was set.
     *
     * @return the absolute Z coordinate according to the arbitrary ordering
     */
    public final int getZ() {
        return startZ + getRelativeZ();
    }

    /**
     * Outputs the current iterator relative position as a string, for debugging.
     *
     * @return &quot;&lt;n,l,m&gt;&quot; with the ordering applied
     */
    public String getCurrentRelativePos() {
        return String.format("<%d,%d,%d>", getRelativeX(), getRelativeY(), getRelativeZ());
    }

    /**
     * Outputs the current iterator absolute position as a string, for debugging.
     *
     * @return &quot;&lt;startX+n,startY+l,startZ+m&gt;&quot; with the ordering applied
     */
    public String getCurrentAbsolutePos() {
        return String.format("<%d,%d,%d>", getX(), getY(), getZ());
    }

    /**
     * Outputs the current iterator internal values as a string, for debugging.
     *
     * @return &quot;&lt;n,l,m&gt;&quot; WITHOUT the ordering applied
     */
    public String getCurrentNLM() {
        return String.format("<%d,%d,%d>", n, l, m);
    }

    /**
     * @hidden
     */
    @Override
    public String toString() {
        return getClass().getName() + "@" + getCurrentRelativePos();
    }

    /**
     * Progresses the iterator and gives the coordinates of n, l, and m, together, packed with
     * {@link com.gtnewhorizon.gtnhlib.util.CoordinatePacker}
     * 
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackX
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackY
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackZ
     *
     * @return n, l, and m, packed together into a long
     */
    public final long nextNLMPacked() {
        next();
        return com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack(n, l, m);
    }

    /**
     * Progresses the iterator and gives the coordinates of x, y, and z, together, packed with
     * {@link com.gtnewhorizon.gtnhlib.util.CoordinatePacker}
     * 
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackX
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackY
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackZ
     *
     * @return n, l, and m, packed together into a long
     */
    public final long nextXYZPacked() {
        next();
        return com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack(getX(), getY(), getZ());
    }

    /**
     * Progresses the iterator and gives the relative coordinates of x, y, and z, together, packed with
     * {@link com.gtnewhorizon.gtnhlib.util.CoordinatePacker}
     * 
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackX
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackY
     * @see com.gtnewhorizon.gtnhlib.util.CoordinatePacker#unpackZ
     *
     * @return n, l, and m, packed together into a long
     */
    public final long nextRelativeXYZPacked() {
        next();
        return com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack(getRelativeX(), getRelativeY(), getRelativeZ());
    }

}
