package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties;

import java.util.Arrays;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8;

import lombok.Getter;

public enum ModelQuadFacing {

    POS_X(1, 0, 0, Axis.X),
    POS_Y(0, 1, 0, Axis.Y),
    POS_Z(0, 0, 1, Axis.Z),
    NEG_X(-1, 0, 0, Axis.X),
    NEG_Y(0, -1, 0, Axis.Y),
    NEG_Z(0, 0, -1, Axis.Z),
    UNASSIGNED(0, 0, 0, null);

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();
    public static final ModelQuadFacing[] DIRECTIONS = Arrays.stream(VALUES).filter(facing -> facing != UNASSIGNED)
            .toArray(ModelQuadFacing[]::new);
    public static final Axis[] AXES = Axis.values();

    public static final int COUNT = VALUES.length;

    public static final int NONE = 0;
    public static final int ALL = (1 << COUNT) - 1;

    @Getter
    private final int packedNormal;

    @Getter
    private final int stepX, stepY, stepZ;

    @Getter
    private final Axis axis;

    ModelQuadFacing(int stepX, int stepY, int stepZ, Axis axis) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
        this.axis = axis;
        this.packedNormal = NormI8.pack(stepX, stepY, stepZ);
    }

    public ModelQuadFacing getOpposite() {
        return switch (this) {
            case POS_Y -> NEG_Y;
            case NEG_Y -> POS_Y;
            case POS_X -> NEG_X;
            case NEG_X -> POS_X;
            case POS_Z -> NEG_Z;
            case NEG_Z -> POS_Z;
            default -> UNASSIGNED;
        };
    }

    public boolean isDirection() {
        return this != UNASSIGNED;
    }

    public enum Axis {

        X,
        Y,
        Z;

        public ModelQuadFacing getFacing(boolean positive) {
            return switch (this) {
                case X -> positive ? POS_X : NEG_X;
                case Y -> positive ? POS_Y : NEG_Y;
                case Z -> positive ? POS_Z : NEG_Z;
            };
        }
    };
}
