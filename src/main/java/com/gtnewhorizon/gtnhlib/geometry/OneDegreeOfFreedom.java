package com.gtnewhorizon.gtnhlib.geometry;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Contract;

public interface OneDegreeOfFreedom<TSelf> {

    ForgeDirection getA();

    /// Sets the direction to the given value. May or may not mutate this, the returned value should always
    /// replace the 'current' value.
    @Contract(mutates = "this")
    TSelf withA(ForgeDirection direction);
}
