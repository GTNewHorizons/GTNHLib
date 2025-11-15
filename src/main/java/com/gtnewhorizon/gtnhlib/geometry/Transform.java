package com.gtnewhorizon.gtnhlib.geometry;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/// An arbitrary transform.
@SuppressWarnings("unused")
@EqualsAndHashCode
@ToString
public class Transform implements DirectionTransform, VectorTransform {

    public final Matrix4f matrix = new Matrix4f();

    public Transform rotate(ForgeDirection vector, int amount) {
        matrix.rotate((float) (Math.PI / 2 * amount), v(vector));

        return this;
    }

    public Transform flip(Axis axis) {
        switch (axis) {
            case X -> matrix.scale(-1, 1, 1);
            case Y -> matrix.scale(1, -1, 1);
            case Z -> matrix.scale(1, 1, -1);
        }

        return this;
    }

    public Axis transform(Axis axis) {
        ForgeDirection out = transform(switch (axis) {
            case X -> EAST;
            case Y -> UP;
            case Z -> SOUTH;
            case UNKNOWN -> UNKNOWN;
        });

        return switch (out) {
            case EAST, WEST -> Axis.X;
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
            case UNKNOWN -> Axis.UNKNOWN;
        };
    }

    @Override
    public ForgeDirection apply(ForgeDirection direction) {
        return transform(direction);
    }

    /// Transforms a ForgeDirection
    public ForgeDirection transform(ForgeDirection dir) {
        return transform(dir, matrix);
    }

    /// Transforms a bitmask of ForgeDirections
    public int transform(int bitmask) {
        if (bitmask == 0) return 0;

        int out = 0;

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if ((bitmask & dir.flag) != 0) {
                out |= transform(dir).flag;
            }
        }

        return out;
    }

    /// Transforms an int vector
    public Vector3i transform(Vector3i v) {
        Vector3f v2 = new Vector3f(v).mulTransposeDirection(matrix);

        v.x = Math.round(v2.x);
        v.y = Math.round(v2.y);
        v.z = Math.round(v2.z);

        return v;
    }

    @Override
    public Vector3f transform(Vector3f v) {
        return v.mulTransposeDirection(matrix);
    }

    @Override
    public Vector3f inverse(Vector3f v) {
        Matrix4f inverse = new Matrix4f(matrix).invert();

        return v.mulTransposeDirection(inverse);
    }

    private static Vector3f v(ForgeDirection dir) {
        return new Vector3f(dir.offsetX, dir.offsetY, dir.offsetZ);
    }

    private static ForgeDirection vprime(Vector3f dir) {
        return switch (dir.maxComponent()) {
            case 0 -> dir.x > 0 ? EAST : WEST;
            case 1 -> dir.y > 0 ? UP : DOWN;
            case 2 -> dir.z > 0 ? SOUTH : NORTH;
            default -> throw new AssertionError();
        };
    }

    public static ForgeDirection transform(ForgeDirection dir, Matrix4f transform) {
        if (dir == null) return null;
        if (dir == UNKNOWN) return UNKNOWN;

        return vprime(v(dir).mulTransposeDirection(transform));
    }
}
