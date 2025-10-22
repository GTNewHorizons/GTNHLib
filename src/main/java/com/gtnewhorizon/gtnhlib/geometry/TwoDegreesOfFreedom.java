package com.gtnewhorizon.gtnhlib.geometry;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Contract;

public interface TwoDegreesOfFreedom<TSelf> {

    ForgeDirection getA();
    ForgeDirection getB();

    /// Sets the directions to the given values. May or may not mutate this, the returned value should always
    /// replace the 'current' value.
    @Contract(mutates = "this")
    TSelf withAB(ForgeDirection a, ForgeDirection b);
}
