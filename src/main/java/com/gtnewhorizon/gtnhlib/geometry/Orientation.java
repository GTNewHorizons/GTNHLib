package com.gtnewhorizon.gtnhlib.geometry;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.util.EnumMap;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Contract;

public enum Orientation implements TwoDegreesOfFreedom<Orientation> {

    UNKNOWN(ForgeDirection.UNKNOWN, ForgeDirection.UNKNOWN),
    DOWN_DOWN(DOWN, DOWN),
    UP_DOWN(UP, DOWN),
    NORTH_DOWN(NORTH, DOWN),
    SOUTH_DOWN(SOUTH, DOWN),
    WEST_DOWN(WEST, DOWN),
    EAST_DOWN(EAST, DOWN),
    DOWN_UP(DOWN, UP),
    UP_UP(UP, UP),
    NORTH_UP(NORTH, UP),
    SOUTH_UP(SOUTH, UP),
    WEST_UP(WEST, UP),
    EAST_UP(EAST, UP),
    DOWN_NORTH(DOWN, NORTH),
    UP_NORTH(UP, NORTH),
    NORTH_NORTH(NORTH, NORTH),
    SOUTH_NORTH(SOUTH, NORTH),
    WEST_NORTH(WEST, NORTH),
    EAST_NORTH(EAST, NORTH),
    DOWN_SOUTH(DOWN, SOUTH),
    UP_SOUTH(UP, SOUTH),
    NORTH_SOUTH(NORTH, SOUTH),
    SOUTH_SOUTH(SOUTH, SOUTH),
    WEST_SOUTH(WEST, SOUTH),
    EAST_SOUTH(EAST, SOUTH),
    DOWN_WEST(DOWN, WEST),
    UP_WEST(UP, WEST),
    NORTH_WEST(NORTH, WEST),
    SOUTH_WEST(SOUTH, WEST),
    WEST_WEST(WEST, WEST),
    EAST_WEST(EAST, WEST),
    DOWN_EAST(DOWN, EAST),
    UP_EAST(UP, EAST),
    NORTH_EAST(NORTH, EAST),
    SOUTH_EAST(SOUTH, EAST),
    WEST_EAST(WEST, EAST),
    EAST_EAST(EAST, EAST);

    public final ForgeDirection a, b;

    Orientation(ForgeDirection a, ForgeDirection b) {
        this.a = a;
        this.b = b;
    }

    private static final EnumMap<ForgeDirection, EnumMap<ForgeDirection, Orientation>> ORIENTATIONS = new EnumMap<>(
            ForgeDirection.class);

    static {
        for (Orientation o : values()) {
            ORIENTATIONS.computeIfAbsent(o.a, x -> new EnumMap<>(ForgeDirection.class)).put(o.b, o);
        }
    }

    public static Orientation getOrientation(ForgeDirection a, ForgeDirection b) {
        if (a == null || a == ForgeDirection.UNKNOWN) {
            return Orientation.UNKNOWN;
        }

        if (b == null || b == ForgeDirection.UNKNOWN) {
            return Orientation.UNKNOWN;
        }

        return ORIENTATIONS.get(a).get(b);
    }

    @Override
    public ForgeDirection getA() {
        return a;
    }

    @Override
    public ForgeDirection getB() {
        return b;
    }

    @Contract(mutates = "this")
    @Override
    public Orientation withAB(ForgeDirection a, ForgeDirection b) {
        return getOrientation(a, b);
    }
}
