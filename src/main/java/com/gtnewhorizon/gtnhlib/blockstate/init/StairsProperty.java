package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.lang.reflect.Type;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;

/**
 * Block state property for stairs, MC 1.7.10 doesn't store their corner shape in the meta. Uses the same property as
 * later versions: {@code shape}, with {@code straight}, {@code inner_left}, {@code inner_right}, {@code outer_left} or
 * {@code outer_right}. The checks are the same as vanilla's renderer.
 *
 * <p>
 * It can only be set in a {@link BlockStateFakeWorld}, where stairs making the corner are placed next to it. In other
 * worlds setting it does nothing.
 */
class StairsProperty implements BlockProperty<String> {

    private static final StairsProperty INSTANCE = new StairsProperty();

    private static final String STRAIGHT = "straight";
    private static final String OUTER_LEFT = "outer_left";
    private static final String OUTER_RIGHT = "outer_right";
    private static final String INNER_LEFT = "inner_left";
    private static final String INNER_RIGHT = "inner_right";

    private static final int HALF_BIT = 0b100;

    static void register() {
        registerProperty(BlockStairs.class, INSTANCE);
    }

    @Override
    public String getName() {
        return "shape";
    }

    @Override
    public Type getType() {
        return String.class;
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable -> true;
            default -> false;
        };
    }

    @Override
    public String getValue(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        ForgeDirection facing = getStairFacing(meta);

        // outer corners take precedence, vanilla only checks for inner corners without one
        ForgeDirection front = getSidewaysStairs(world, x, y, z, meta, facing);
        if (front != null && canTakeStairShape(world, x, y, z, meta, front.getOpposite())) {
            return front == getCounterClockwise(facing) ? OUTER_LEFT : OUTER_RIGHT;
        }

        ForgeDirection back = getSidewaysStairs(world, x, y, z, meta, facing.getOpposite());
        if (back != null && canTakeStairShape(world, x, y, z, meta, back)) {
            return back == getCounterClockwise(facing) ? INNER_LEFT : INNER_RIGHT;
        }

        return STRAIGHT;
    }

    /** a copy of the stairs facing sideways in the checked spot makes the same corner, the spots around it stay air */
    @Override
    public void setValue(World world, int x, int y, int z, String shape) {
        if (!(world instanceof BlockStateFakeWorld)) return;

        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        ForgeDirection facing = getStairFacing(meta);
        ForgeDirection side;
        ForgeDirection sideFacing;
        switch (shape) {
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

        world.setBlock(x + side.offsetX, y, z + side.offsetZ, block, (meta & HALF_BIT) | getStairMeta(sideFacing), 2);
    }

    @Override
    public String stringify(String value) {
        return value;
    }

    @Override
    public String parse(String text) throws InvalidPropertyTextException {
        return switch (text) {
            case STRAIGHT, OUTER_LEFT, OUTER_RIGHT, INNER_LEFT, INNER_RIGHT -> text;
            default -> throw new InvalidPropertyTextException("Illegal stairs shape: '" + text + "'");
        };
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
