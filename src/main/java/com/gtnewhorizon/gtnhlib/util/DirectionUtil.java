package com.gtnewhorizon.gtnhlib.util;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.MathUtil.roughlyEqual;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

public class DirectionUtil {

    public static ForgeDirection yawToDirection(float yaw) {
        return switch (Math.round(MathUtil.mod(yaw, 360) / 360 * 4)) {
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            case 3 -> ForgeDirection.WEST;
            default -> ForgeDirection.NORTH;
        };
    }

    public static ModelQuadFacing rotateFacing(ModelQuadFacing facing, Matrix4fc rotation) {
        // We can't meaningfully rotate an unknown vector
        if (!facing.isDirection()) return ModelQuadFacing.UNASSIGNED;

        final var vecFacing = new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ())
                .mulDirection(rotation);
        // Only one of these three vector coordinates should be a value other than near-zero.
        if (roughlyEqual(vecFacing.x, 1)) {
            return ModelQuadFacing.POS_X;
        } else if (roughlyEqual(vecFacing.x, -1)) {
            return ModelQuadFacing.NEG_X;
        } else if (roughlyEqual(vecFacing.y, 1)) {
            return ModelQuadFacing.POS_Y;
        } else if (roughlyEqual(vecFacing.y, -1)) {
            return ModelQuadFacing.NEG_Y;
        } else if (roughlyEqual(vecFacing.z, 1)) {
            return ModelQuadFacing.POS_Z;
        } else if (roughlyEqual(vecFacing.z, -1)) {
            return ModelQuadFacing.NEG_Z;
        }

        // We rotated it and didn't get something axis-aligned!
        return ModelQuadFacing.UNASSIGNED;
    }
}
