package com.gtnewhorizon.gtnhlib.blockstate.init;

import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for stairs, MC 1.7.10 doesn't store their corner shape in the meta. Uses the same property as later
 * versions: {@code shape}, with {@code inner_left}, {@code inner_right}, {@code outer_left} or {@code outer_right}.
 * Straight stairs have no data. The checks are the same as vanilla's renderer.
 */
class StairsData implements BlockStateDataHandler {

    private static final StairsData INSTANCE = new StairsData();

    private static final String SHAPE_PREFIX = "shape=";
    private static final String OUTER_LEFT = "outer_left";
    private static final String OUTER_RIGHT = "outer_right";
    private static final String INNER_LEFT = "inner_left";
    private static final String INNER_RIGHT = "inner_right";

    private static final int HALF_BIT = 0b100;

    static void register() {
        BlockStateDataRegistry.register(BlockStairs.class, INSTANCE);
    }

    @Override
    public Type getType() {
        return Type.NEIGHBOR_DEPENDENT;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String encode(IBlockAccess world, int x, int y, int z, Block block, int meta) {
        ForgeDirection facing = getStairFacing(meta);

        // outer corners take precedence, vanilla only checks for inner corners without one
        ForgeDirection front = getSidewaysStairs(world, x, y, z, meta, facing);
        if (front != null && canTakeStairShape(world, x, y, z, meta, front.getOpposite())) {
            return SHAPE_PREFIX + (front == getCounterClockwise(facing) ? OUTER_LEFT : OUTER_RIGHT);
        }

        ForgeDirection back = getSidewaysStairs(world, x, y, z, meta, facing.getOpposite());
        if (back != null && canTakeStairShape(world, x, y, z, meta, back)) {
            return SHAPE_PREFIX + (back == getCounterClockwise(facing) ? INNER_LEFT : INNER_RIGHT);
        }

        return null;
    }

    /** a copy of the stairs facing sideways in the checked spot makes the same corner, the spots around it stay air */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        if (!data.startsWith(SHAPE_PREFIX)) return;

        ForgeDirection facing = getStairFacing(meta);
        ForgeDirection side;
        ForgeDirection sideFacing;
        switch (data.substring(SHAPE_PREFIX.length())) {
            case OUTER_LEFT -> {
                side = facing;
                sideFacing = getCounterClockwise(facing);
            }
            case OUTER_RIGHT -> {
                side = facing;
                sideFacing = getCounterClockwise(facing).getOpposite();
            }
            case INNER_LEFT -> {
                side = facing.getOpposite();
                sideFacing = getCounterClockwise(facing);
            }
            case INNER_RIGHT -> {
                side = facing.getOpposite();
                sideFacing = getCounterClockwise(facing).getOpposite();
            }
            default -> {
                return;
            }
        }

        world.setBlock(side.offsetX, 0, side.offsetZ, block, (meta & HALF_BIT) | getStairMeta(sideFacing));
    }

    /** @return the facing of the stairs on that side if they face across this one and have the same half */
    private static @Nullable ForgeDirection getSidewaysStairs(IBlockAccess world, int x, int y, int z, int meta,
            ForgeDirection side) {
        x += side.offsetX;
        z += side.offsetZ;
        if (!(world.getBlock(x, y, z) instanceof BlockStairs)) return null;

        int sideMeta = world.getBlockMetadata(x, y, z);
        if ((sideMeta & HALF_BIT) != (meta & HALF_BIT)) return null;

        ForgeDirection sideFacing = getStairFacing(sideMeta);
        return (sideFacing.offsetX == 0) != (getStairFacing(meta).offsetX == 0) ? sideFacing : null;
    }

    private static boolean canTakeStairShape(IBlockAccess world, int x, int y, int z, int meta, ForgeDirection side) {
        x += side.offsetX;
        z += side.offsetZ;
        return !(world.getBlock(x, y, z) instanceof BlockStairs) || world.getBlockMetadata(x, y, z) != meta;
    }

    private static ForgeDirection getStairFacing(int meta) {
        return switch (meta & 0b11) {
            case 0 -> EAST;
            case 1 -> WEST;
            case 2 -> SOUTH;
            default -> NORTH;
        };
    }

    private static int getStairMeta(ForgeDirection facing) {
        return switch (facing) {
            case EAST -> 0;
            case WEST -> 1;
            case SOUTH -> 2;
            default -> 3;
        };
    }

    private static ForgeDirection getCounterClockwise(ForgeDirection dir) {
        return switch (dir) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            default -> NORTH;
        };
    }
}
