package com.gtnewhorizon.gtnhlib.client.renderer.util;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.properties.ModelQuadFacing;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to be
 * cloned every time.
 */
public class DirectionUtil {

    public static final ForgeDirection[] ALL_DIRECTIONS = ForgeDirection.values();

    // Provides the same order as enumerating ForgeDirection and checking the axis of each value
    public static final ForgeDirection[] HORIZONTAL_DIRECTIONS = new ForgeDirection[] { ForgeDirection.NORTH,
            ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST };

    public static final Vector3i[] STEP = {

            // DOWN
            new Vector3i(0, -1, 0),
            // UP
            new Vector3i(0, 1, 0),
            // NORTH
            new Vector3i(0, 0, -1),
            // SOUTH
            new Vector3i(0, 0, 1),
            // WEST
            new Vector3i(-1, 0, 0),
            // EAST
            new Vector3i(1, 0, 0),
            // UNKNOWN
            new Vector3i(0, 0, 0) };

    public static ForgeDirection rotateDir(ForgeDirection in, Matrix4f rotMat) {

        final Vector3f v = new Vector3f(in.offsetX, in.offsetY, in.offsetZ);
        v.mulPosition(rotMat);
        return ModelQuadFacing.toDirection(ModelQuadFacing.fromVector(v));
    }

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
