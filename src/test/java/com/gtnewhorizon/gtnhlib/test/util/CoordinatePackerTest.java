package com.gtnewhorizon.gtnhlib.test.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.ChunkCoordinates;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public class CoordinatePackerTest {

    @Test
    void testPackUnpack() {
        int[][] coordinates = {
                // 0
                { 0, 0, 0 },
                // MC min, [0, 255], MC min
                { -30_000_000, 0, -30_000_000 }, { -30_000_000, 255, -30_000_000 },
                // MC max, [0, 255], MC max
                { 30_000_000, 0, 30_000_000 }, { 30_000_000, 255, 30_000_000 },
                // MC min, [0, 255], MC max
                { -30_000_000, 0, 30_000_000 }, { -30_000_000, 255, 30_000_000 },
                // MC max, [0, 255], MC min
                { 30_000_000, 0, -30_000_000 }, { 30_000_000, 255, -30_000_000 },
                // Java Min/Max based on bits
                { -33_554_432, -2048, -33_554_432 }, { 33_554_431, 2047, 33_554_431 },
                // bunch of 255's
                { 255, 255, 255 }, { 255, 255, 0 }, { 255, 0, 255 }, { 0, 255, 255 }, { 0, 0, 255 }, { 0, 255, 0 },
                { 255, 0, 0 }, { 0, 255, 0 }, { 0, 0, 255 },
                // 0, min/max, 0
                { 0, -2048, 0 }, { 0, 2047, 0 }, };
        final ChunkCoordinates chunkCoordinates = new ChunkCoordinates();
        final ChunkCoordinates unpackedCoordinates = new ChunkCoordinates();
        for (int[] coordinate : coordinates) {
            chunkCoordinates.set(coordinate[0], coordinate[1], coordinate[2]);
            long packed = CoordinatePacker.pack(chunkCoordinates);
            CoordinatePacker.unpack(packed, unpackedCoordinates);
            assertTrue(
                    coordinate[0] == unpackedCoordinates.posX && coordinate[1] == unpackedCoordinates.posY
                            && coordinate[2] == unpackedCoordinates.posZ,
                    "Failed for " + coordinate[0]
                            + ", "
                            + coordinate[1]
                            + ", "
                            + coordinate[2]
                            + " -> "
                            + packed
                            + " -> "
                            + unpackedCoordinates.posX
                            + ", "
                            + unpackedCoordinates.posY
                            + ", "
                            + unpackedCoordinates.posZ);
        }
    }

}
