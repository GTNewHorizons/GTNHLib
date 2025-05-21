package com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid;

public class Int3 {

    public int x;
    public int y;
    public int z;

    public Int3() {

    }

    public Int3(int x, int y, int z) {
        set(x, y, z);
    }

    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
