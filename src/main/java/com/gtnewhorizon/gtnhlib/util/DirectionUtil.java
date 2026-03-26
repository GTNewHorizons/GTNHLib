package com.gtnewhorizon.gtnhlib.util;

import net.minecraftforge.common.util.ForgeDirection;

public class DirectionUtil {

    public static ForgeDirection yawToDirection(float yaw) {
        return switch (Math.round(MathUtil.mod(yaw, 360) / 360 * 4)) {
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            case 3 -> ForgeDirection.WEST;
            default -> ForgeDirection.NORTH;
        };
    }
}
