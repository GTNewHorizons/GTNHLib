package com.gtnewhorizon.gtnhlib.util;

import net.minecraft.util.ChunkCoordinates;

import org.joml.Vector3i;

public final class CoordinatePacker {

    private static final int SIZE_BITS_X = 26; // range in MC: -30,000,000 to 30,000,000; Range here - [-33554432,
                                               // 33554431]
    private static final int SIZE_BITS_Z = SIZE_BITS_X; // Same as X
    private static final int SIZE_BITS_Y = 64 - SIZE_BITS_X - SIZE_BITS_Z; // range in MC: [0, 255]; Range here -
                                                                           // [-2048, 2047]

    private static final long BITS_X = (1L << SIZE_BITS_X) - 1L;
    private static final long BITS_Y = (1L << SIZE_BITS_Y) - 1L;
    private static final long BITS_Z = (1L << SIZE_BITS_Z) - 1L;

    private static final int BIT_SHIFT_X = SIZE_BITS_Y + SIZE_BITS_Z;
    private static final int BIT_SHIFT_Z = SIZE_BITS_Y;
    private static final int BIT_SHIFT_Y = 0;

    public static long pack(int x, int y, int z) {
        long l = 0L;
        l |= ((long) x & BITS_X) << BIT_SHIFT_X;
        l |= ((long) y & BITS_Y) << BIT_SHIFT_Y;
        l |= ((long) z & BITS_Z) << BIT_SHIFT_Z;
        return l;
    }

    public static long pack(ChunkCoordinates coords) {
        return pack(coords.posX, coords.posY, coords.posZ);
    }

    public static long pack(Vector3i coords) {
        return pack(coords.x, coords.y, coords.z);
    }

    public static int unpackX(long packed) {
        return (int) (packed << 64 - BIT_SHIFT_X - SIZE_BITS_X >> 64 - SIZE_BITS_X);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 64 - SIZE_BITS_Y >> 64 - SIZE_BITS_Y);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 64 - BIT_SHIFT_Z - SIZE_BITS_Z >> 64 - SIZE_BITS_Z);
    }

    public static void unpack(long packed, ChunkCoordinates coords) {
        coords.posX = unpackX(packed);
        coords.posY = unpackY(packed);
        coords.posZ = unpackZ(packed);
    }

    public static void unpack(long packed, Vector3i coords) {
        coords.x = unpackX(packed);
        coords.y = unpackY(packed);
        coords.z = unpackZ(packed);
    }
}
