package com.gtnewhorizon.gtnhlib.geometry;

/**
 * Abstract3DIterator that works outwards under the L infinity metric, forming a cube. (distance = max(x, y, z)) <br>
 * Uses the manhattan metric secondarially to order triples in the same shell. (distance = x+y+z) <br>
 * Use {@link com.gtnewhorizon.gtnhlib.geometry.Abstract3DIterator#getCurrentRelativePos} to debug.
 *
 * @author __felix__
 */
public class CubeIterator extends Abstract3DIterator {

    /**
     * Initializes without setting the offsets used by certain methods. <br>
     * Set the range to a negative number for {@link #hasNext()} and the like to always return true.
     * 
     * @see com.gtnewhorizon.gtnhlib.geometry.Abstract3DIterator#Abstract3DIterator(int)
     */
    public CubeIterator(int range) {
        super(range);
    }

    /**
     * Initializes without setting the offsets used by certain methods and adds an ordering. <br>
     * Set the range to a negative number for {@link #hasNext()} and the like to always return true.
     * 
     * @see com.gtnewhorizon.gtnhlib.geometry.Abstract3DIterator#Abstract3DIterator(int, short)
     */
    public CubeIterator(int range, short order) {
        super(range, order);
    }

    /**
     * Initializes with setting the offsets used by certain methods. <br>
     * Set the range to a negative number for {@link #hasNext()} and the like to always return true.
     * 
     * @see com.gtnewhorizon.gtnhlib.geometry.Abstract3DIterator#Abstract3DIterator(int,int,int,int)
     */
    public CubeIterator(int range, int x, int y, int z) {
        super(range, x, y, z);
    }

    /**
     * Initializes with setting the offsets used by certain methods and adds an ordering. <br>
     * Set the range to a negative number for {@link #hasNext()} and the like to always return true.
     * 
     * @see com.gtnewhorizon.gtnhlib.geometry.Abstract3DIterator#Abstract3DIterator(int,short,int,int,int)
     */
    public CubeIterator(int range, short order, int x, int y, int z) {
        super(range, order, x, y, z);
    }

    /**
     * Returns whether the iterator is at or past the end position. Use this if you use the values after or with
     * {@link #next()} in your loop.
     *
     * @return whether the iterator is at or past the end position: n, l, m >= -range
     */
    public boolean hasNext() {
        return (range < 0 || -n < range || -l < range || -m < range) && hasNextFrom0();
    }

    /**
     * Returns whether the iterator is past the end position, if you call {@link #next()} BEFORE using the coords in
     * your loop. Basically, the iterator starts at &lt;0,0,0&gt;, so if you want to use that position and not have it
     * end one iteration before the final corner, call this instead as the check in the loop.
     *
     * @return whether the iterator is past the end position
     */
    public boolean hasNextFrom0() {
        return range < 0 || (n >= -range && n <= range && l >= -range && l <= range && m >= -range && m <= range);
    }

    /**
     * Returns whether the iterator is at its start position.
     *
     * @return whether n, l, and m are all 0 (meaning the center block)
     */
    public boolean hasPrevious() {
        return n != 0 || l != 0 || m != 0;
    }

    /**
     * Progresses the CubeIterator forward by one step (the changes are in the n, l, and m fields). First inverts the
     * coords in a binary-counter fashion, then once they are all negative swaps them around until they are back in
     * descending order, then bumps m up unless it is equal to l, otherwise bumps l up and resets m, or if all three are
     * equal bumps n and resets the other two. Uses the L infinity norm/metric, forming a cube.
     */
    @Override
    public void next() {
        int g;
        m = -m;
        if (m < 0) return;
        l = -l;
        if (l < 0) return;
        n = -n;
        if (n < 0) return;
        if (l >= n || m > n) {
            if (m >= l) {
                if (n <= l) {
                    if (m > n) {
                        g = m;
                        m = n;
                        n = g;
                        if (l > m) {
                            ++m;
                            return;
                        }
                        m = 0;
                        ++l;
                        return;
                    }
                    l = 0;
                    m = 0;
                    ++n;
                    return;
                }
                g = l;
                l = n;
                n = g;
                return;
            }
            if (n < m) {
                g = m;
                m = n;
                n = g;
                return;
            }
            g = l;
            l = m;
            m = g;
            return;
        }
        if (l > m) {
            g = l;
            l = m;
            m = g;
            return;
        }
        g = l;
        l = n;
        n = g;
        return;
    }

}
