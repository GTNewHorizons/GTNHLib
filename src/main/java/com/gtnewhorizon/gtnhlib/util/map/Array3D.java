package com.gtnewhorizon.gtnhlib.util.map;

public class Array3D<T> {

    private final int spanx;
    private final int spany;
    private final int spanz;
    private final int spanslice;
    private final int offsetx;
    private final int offsety;
    private final int offsetz;
    private final T[] data;

    public Array3D(int spanx, int spany, int spanz, int offsetx, int offsety, int offsetz, T[] data) {
        this.spanx = spanx;
        this.spany = spany;
        this.spanz = spanz;
        this.spanslice = spanx * spany;
        this.offsetx = offsetx;
        this.offsety = offsety;
        this.offsetz = offsetz;
        this.data = data;
    }

    public final void set(final int x, final int y, final int z, final T value) {
        final int relx = x - offsetx;
        final int rely = y - offsety;
        final int relz = z - offsetz;

        if (relx < 0 || relx >= spanx) return;
        if (rely < 0 || rely >= spany) return;
        if (relz < 0 || relz >= spanz) return;

        data[relx + (rely * spanx) + (relz * spanslice)] = value;
    }

    public final T get(final int x, final int y, final int z) {
        final int relx = x - offsetx;
        final int rely = y - offsety;
        final int relz = z - offsetz;

        if (relx < 0 || relx >= spanx) return null;
        if (rely < 0 || rely >= spany) return null;
        if (relz < 0 || relz >= spanz) return null;

        return data[relx + (rely * spanx) + (relz * spanslice)];
    }
}
