package com.gtnewhorizon.gtnhlib.blockpos;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public interface IBlockPos {

    int getX();

    int getY();

    int getZ();

    IBlockPos offset(ForgeDirection d);

    IBlockPos down();

    IBlockPos up();

    long asLong();

    static long asLong(int x, int y, int z) {
        return CoordinatePacker.pack(x, y, z);
    }
}
