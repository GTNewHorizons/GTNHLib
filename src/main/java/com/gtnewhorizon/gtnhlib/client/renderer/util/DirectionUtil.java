package com.gtnewhorizon.gtnhlib.client.renderer.util;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to be
 * cloned every time.
 */
public class DirectionUtil {

    public static ForgeDirection fromName(String name) {
        return switch (name) {
            case "up" -> ForgeDirection.UP;
            case "down" -> ForgeDirection.DOWN;
            case "north" -> ForgeDirection.NORTH;
            case "south" -> ForgeDirection.SOUTH;
            case "west" -> ForgeDirection.WEST;
            case "east" -> ForgeDirection.EAST;
            case "unknown" -> ForgeDirection.UNKNOWN;
            default -> null;
        };
    }
}
