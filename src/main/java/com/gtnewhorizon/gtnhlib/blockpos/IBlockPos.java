package com.gtnewhorizon.gtnhlib.blockpos;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public interface IBlockPos {

    int getX();

    int getY();

    int getZ();

    IBlockPos offset(ForgeDirection d);

    IBlockPos offset(int x, int y, int z);

    IBlockPos down();

    IBlockPos up();

    IBlockPos copy();

    long asLong();

    static long asLong(int x, int y, int z) {
        return CoordinatePacker.pack(x, y, z);
    }
}
