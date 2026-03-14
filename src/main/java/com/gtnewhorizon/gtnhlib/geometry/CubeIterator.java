package com.gtnewhorizon.gtnhlib.geometry;

public class CubeIterator extends Abstract3DIterator {

    // public int range = 0;

    // wow, it's just like electron orbitals. and the spin is the sign. how beautiful
    // public int n = 0;
    // public int l = 0;
    // public int m = 0;

    CubeIterator(int range) {
        super(range);
    }

    CubeIterator(int range, int x, int y, int z) {
        super(range, x, y , z);
    }

    // maybe i could put this in next() and make it an Iterator<Boolean>?
    public boolean hasNext() {
        return -n < range || -l < range || -m < range;
    }

    public boolean hasPrevious() {
        return n != 0 && l != 0 && m != 0;
    }

    @Override
    public void __next() {
        // this shit looks like the decompile of an obfuscated assembly but i assure you it is hand written
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
    } // i have just found out that Java has a `when` statement, but primitive pattern matching is preview
      // and the syntax sucks (case boolean b when a>6)
      // i genuinely would rather have written this bytecode by bytecode but here we are

    /*
     * @Override //Not implemented yet (just the sign reversing part is, so far) public void __previous() { m = -m; if
     * (m > 0) return; l = -l; if (l > 0) return; n = -n; if (n > 0) return; if (l >= n || m > n) { if (m >= l) { if (n
     * <= l) { if (m > n) { n ^= m; m ^= n; n ^= m; if (l > m) { ++m; return; } m = 0; ++l; return; } l = 0; m = 0; ++n;
     * return; } n ^= l; l ^= n; n ^= l; return; } if (n < m) { n ^= m; m ^= n; n ^= m; return; } m ^= l; l ^= m; m ^=
     * l; return; } if (l > m) { m ^= l; l ^= m; m ^= l; return; } n ^= l; l ^= n; n ^= l; return; }
     */
}
