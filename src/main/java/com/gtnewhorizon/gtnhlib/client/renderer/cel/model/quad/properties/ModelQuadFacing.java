package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties;

import java.util.Arrays;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.ForgeDirection;

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
    public static final ModelQuadFacing[] HORIZONTAL_DIRECTIONS = Arrays.stream(DIRECTIONS)
            .filter(facing -> facing.axis != Axis.Y).toArray(ModelQuadFacing[]::new);
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
    }

    public static ModelQuadFacing fromForgeDir(ForgeDirection dir) {
        return switch (dir) {
            case UP -> POS_Y;
            case DOWN -> NEG_Y;
            case EAST -> POS_X;
            case WEST -> NEG_X;
            case SOUTH -> POS_Z;
            case NORTH -> NEG_Z;
            case UNKNOWN -> UNASSIGNED;
        };
    }

    public static ModelQuadFacing fromEnumFacing(EnumFacing dir) {
        return switch (dir) {
            case UP -> POS_Y;
            case DOWN -> NEG_Y;
            case WEST -> POS_X;
            case EAST -> NEG_X;
            case NORTH -> NEG_Z;
            case SOUTH -> POS_Z;
        };
    }

    public ForgeDirection toForgeDir() {
        return switch (this) {
            case POS_Y -> ForgeDirection.UP;
            case NEG_Y -> ForgeDirection.DOWN;
            case POS_X -> ForgeDirection.EAST;
            case NEG_X -> ForgeDirection.WEST;
            case POS_Z -> ForgeDirection.SOUTH;
            case NEG_Z -> ForgeDirection.NORTH;
            case UNASSIGNED -> ForgeDirection.UNKNOWN;
        };
    }

    public EnumFacing toEnumFacing() {
        return switch (this) {
            case POS_Y -> EnumFacing.UP;
            case NEG_Y -> EnumFacing.DOWN;
            case POS_X -> EnumFacing.WEST;
            case NEG_X -> EnumFacing.EAST;
            case POS_Z -> EnumFacing.SOUTH;
            case NEG_Z -> EnumFacing.NORTH;
            case UNASSIGNED -> throw new IllegalArgumentException(
                    "Cannot convert UNASSIGNED ModelQuadFacing to EnumFacing!");
        };
    }
}
