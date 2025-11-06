package com.gtnewhorizon.gtnhlib.blockpos;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Contract;

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

    @Contract(mutates = "this")
    IBlockPos add(ForgeDirection d);

    @Contract(mutates = "this")
    IBlockPos sub(ForgeDirection d);

    long asLong();

    static long asLong(int x, int y, int z) {
        return CoordinatePacker.pack(x, y, z);
    }
}
