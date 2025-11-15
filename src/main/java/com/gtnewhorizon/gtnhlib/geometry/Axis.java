package com.gtnewhorizon.gtnhlib.geometry;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import net.minecraftforge.common.util.ForgeDirection;

public enum Axis {

    X,
    Y,
    Z,
    UNKNOWN;

    public static Axis fromDirection(ForgeDirection dir) {
        return switch (dir) {
            case EAST, WEST -> Axis.X;
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
            case UNKNOWN -> UNKNOWN;
        };
    }

    public ForgeDirection positive() {
        return switch (this) {
            case X -> EAST;
            case Y -> UP;
            case Z -> SOUTH;
            case UNKNOWN -> ForgeDirection.UNKNOWN;
        };
    }

    public ForgeDirection negative() {
        return switch (this) {
            case X -> WEST;
            case Y -> DOWN;
            case Z -> NORTH;
            case UNKNOWN -> ForgeDirection.UNKNOWN;
        };
    }

    public ForgeDirection flip(ForgeDirection dir) {
        switch (this) {
            case X -> {
                if (dir == WEST) return EAST;
                if (dir == EAST) return WEST;
            }
            case Y -> {
                if (dir == DOWN) return UP;
                if (dir == UP) return DOWN;
            }
            case Z -> {
                if (dir == NORTH) return SOUTH;
                if (dir == SOUTH) return NORTH;
            }
            case UNKNOWN -> {
                return ForgeDirection.UNKNOWN;
            }
        }

        return dir;
    }
}
