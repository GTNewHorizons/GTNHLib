package com.gtnewhorizon.gtnhlib.util;

public class CoordinatePacker2D {

    private static final long XZ_INT = 0xFFFFFFFFL;

    public static long packChunk(int x, int z) {
        return (z & XZ_INT) << 32 | (x & XZ_INT);
    }

    public static int unpackChunkX(long key) {
        return (int) key;
    }

    public static int unpackChunkZ(long key) {
        return (int) (key >> 32);
    }
}
