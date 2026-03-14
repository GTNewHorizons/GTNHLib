package com.gtnewhorizon.gtnhlib.geometry;

public class CubeIterator extends Abstract3DIterator {

    CubeIterator(int range) {
        super(range);
    }

    CubeIterator(int range, int x, int y, int z) {
        super(range, x, y , z);
    }

    public boolean hasNext() {
        return -n < range || -l < range || -m < range;
    }

    public boolean hasPrevious() {
        return n != 0 && l != 0 && m != 0;
    }

    @Override
    public void __next() {
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
                        n ^= m;
                        m ^= n;
                        n ^= m;
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
                n ^= l;
                l ^= n;
                n ^= l;
                return;
            }
            if (n < m) {
                n ^= m;
                m ^= n;
                n ^= m;
                return;
            }
            m ^= l;
            l ^= m;
            m ^= l;
            return;
        }
        if (l > m) {
            m ^= l;
            l ^= m;
            m ^= l;
            return;
        }
        n ^= l;
        l ^= n;
        n ^= l;
        return;
    }

    /*
     * @Override //Not implemented yet (just the sign reversing part is, so far) public void __previous() { m = -m; if
     * (m > 0) return; l = -l; if (l > 0) return; n = -n; if (n > 0) return; if (l >= n || m > n) { if (m >= l) { if (n
     * <= l) { if (m > n) { n ^= m; m ^= n; n ^= m; if (l > m) { ++m; return; } m = 0; ++l; return; } l = 0; m = 0; ++n;
     * return; } n ^= l; l ^= n; n ^= l; return; } if (n < m) { n ^= m; m ^= n; n ^= m; return; } m ^= l; l ^= m; m ^=
     * l; return; } if (l > m) { m ^= l; l ^= m; m ^= l; return; } n ^= l; l ^= n; n ^= l; return; }
     */
}
