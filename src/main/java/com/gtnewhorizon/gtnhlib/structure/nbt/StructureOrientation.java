package com.gtnewhorizon.gtnhlib.structure.nbt;

import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.util.Locale;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;

/**
 * How a structure is placed: mirrored first, then rotated clockwise as seen from above. {@link Mirror#EAST_WEST} swaps
 * east and west (flips x), {@link Mirror#NORTH_SOUTH} swaps north and south (flips z).
 */
public final class StructureOrientation implements DirectionTransform {

    public enum Rotation {

        NONE,
        ROTATE_90,
        ROTATE_180,
        ROTATE_270;

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Mirror {

        NONE(false, false),
        EAST_WEST(true, false),
        NORTH_SOUTH(false, true),
        BOTH(true, true);

        private final boolean flipX;
        private final boolean flipZ;

        Mirror(boolean flipX, boolean flipZ) {
            this.flipX = flipX;
            this.flipZ = flipZ;
        }

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The number of distinct layouts, and the bound of {@link #getIndex()}. */
    public static final int COUNT = 8;

    private static final ForgeDirection[] CLOCKWISE = { NORTH, EAST, SOUTH, WEST };
    private static final StructureOrientation[] VALUES = new StructureOrientation[Rotation.values().length
            * Mirror.values().length];

    static {
        for (Rotation rotation : Rotation.values()) {
            for (Mirror mirror : Mirror.values()) {
                VALUES[rotation.ordinal() * Mirror.values().length + mirror.ordinal()] = new StructureOrientation(
                        rotation,
                        mirror);
            }
        }
    }

    public static final StructureOrientation NONE = of(Rotation.NONE, Mirror.NONE);

    private final Rotation rotation;
    private final Mirror mirror;

    private StructureOrientation(Rotation rotation, Mirror mirror) {
        this.rotation = rotation;
        this.mirror = mirror;
    }

    public static StructureOrientation of(Rotation rotation, Mirror mirror) {
        return VALUES[rotation.ordinal() * Mirror.values().length + mirror.ordinal()];
    }

    public static StructureOrientation of(Rotation rotation) {
        return of(rotation, Mirror.NONE);
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Mirror getMirror() {
        return mirror;
    }

    /**
     * An index in {@code [0, COUNT)} that is equal for orientations producing the same layout, e.g. mirroring both axes
     * and rotating 180 degrees.
     */
    public int getIndex() {
        int turns = rotation.ordinal() + (mirror.flipZ ? 2 : 0);
        return (turns % 4) * 2 + (isMirrored() ? 1 : 0);
    }

    public boolean isMirrored() {
        return mirror.flipX != mirror.flipZ;
    }

    public boolean swapsAxes() {
        return rotation == Rotation.ROTATE_90 || rotation == Rotation.ROTATE_270;
    }

    @Override
    public ForgeDirection apply(ForgeDirection dir) {
        if (dir.offsetY != 0 || dir == ForgeDirection.UNKNOWN) {
            return dir;
        }
        if ((mirror.flipX && dir.offsetX != 0) || (mirror.flipZ && dir.offsetZ != 0)) {
            dir = dir.getOpposite();
        }
        for (int i = 0; i < CLOCKWISE.length; i++) {
            if (CLOCKWISE[i] == dir) {
                return CLOCKWISE[(i + rotation.ordinal()) % CLOCKWISE.length];
            }
        }
        return dir;
    }

    public BlockPos transformSize(BlockPos size) {
        return swapsAxes() ? new BlockPos(size.getZ(), size.getY(), size.getX()) : size;
    }

    /** Moves a position inside a structure of the given untransformed size to its place in the transformed one. */
    public BlockPos transformPos(BlockPos pos, BlockPos size) {
        int maxX = size.getX() - 1;
        int maxZ = size.getZ() - 1;
        int x = mirror.flipX ? maxX - pos.getX() : pos.getX();
        int y = pos.getY();
        int z = mirror.flipZ ? maxZ - pos.getZ() : pos.getZ();
        return switch (rotation) {
            case NONE -> new BlockPos(x, y, z);
            case ROTATE_90 -> new BlockPos(maxZ - z, y, x);
            case ROTATE_180 -> new BlockPos(maxX - x, y, maxZ - z);
            case ROTATE_270 -> new BlockPos(z, y, maxX - x);
        };
    }

    /** Transforms a sign or skull style rotation, 0 to 15 clockwise starting at south. */
    public int transformRotation16(int value) {
        if (mirror.flipX) {
            value = 16 - value;
        }
        if (mirror.flipZ) {
            value = 8 - value;
        }
        return Math.floorMod(value + 4 * rotation.ordinal(), 16);
    }

    @Override
    public String toString() {
        return rotation.getName() + ", mirror " + mirror.getName();
    }
}
