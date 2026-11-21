package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for doors. Each half only stores part of the door, the facing and open state are in the lower half's
 * meta and the hinge side is in the upper half's meta. Vanilla combines both halves and doesn't render a half at all
 * without the other one. Uses the same properties as later versions: upper halves get {@code facing} and {@code open}
 * of their lower half, lower halves get {@code hinge} of their upper half.
 */
class DoorData implements BlockStateDataHandler {

    private static final DoorData INSTANCE = new DoorData();

    private static final int UPPER_HALF_BIT = 0b1000;
    /** in the lower half */
    private static final int FACING_BITS = 0b11;
    /** in the lower half */
    private static final int OPEN_BIT = 0b100;
    /** in the upper half */
    private static final int HINGE_BIT = 0b1;

    /** by the facing bits, the same as later versions */
    private static final String[] FACING_NAMES = { "east", "south", "west", "north" };
    private static final String FACING_PREFIX = "facing=";
    private static final String OPEN_PREFIX = "open=";
    private static final String HINGE_PREFIX = "hinge=";

    static void register() {
        BlockStateDataRegistry.register(BlockDoor.class, INSTANCE);
    }

    @Override
    public Type getType() {
        return Type.LOCAL_ONLY;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String encode(IBlockAccess world, int x, int y, int z, Block block, int meta) {
        boolean upperHalf = (meta & UPPER_HALF_BIT) != 0;
        int otherHalfY = upperHalf ? y - 1 : y + 1;
        if (world.getBlock(x, otherHalfY, z) != block) return null;

        int otherHalfMeta = world.getBlockMetadata(x, otherHalfY, z);
        if (upperHalf) {
            return FACING_PREFIX + FACING_NAMES[otherHalfMeta & FACING_BITS]
                    + ","
                    + OPEN_PREFIX
                    + ((otherHalfMeta & OPEN_BIT) != 0);
        }
        return HINGE_PREFIX + (((otherHalfMeta & HINGE_BIT) != 0) ? "right" : "left");
    }

    /** the other half gives this half the rest of the door */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        int otherHalfBits = 0;
        for (String pair : data.split(",")) {
            if (pair.startsWith(FACING_PREFIX)) {
                String facing = pair.substring(FACING_PREFIX.length());
                for (int bits = 0; bits < FACING_NAMES.length; bits++) {
                    if (FACING_NAMES[bits].equals(facing)) otherHalfBits |= bits;
                }
            } else if (pair.equals(OPEN_PREFIX + "true")) {
                otherHalfBits |= OPEN_BIT;
            } else if (pair.equals(HINGE_PREFIX + "right")) {
                otherHalfBits |= HINGE_BIT;
            }
        }
        setOtherHalf(block, meta, otherHalfBits, world);
    }

    /** vanilla doesn't render a half without the other one, so it gets one with all bits 0: closed, east, left hinge */
    @Override
    public void decodeDefault(Block block, int meta, BlockStateDataWorld world) {
        setOtherHalf(block, meta, 0, world);
    }

    private static void setOtherHalf(Block block, int meta, int otherHalfBits, BlockStateDataWorld world) {
        if ((meta & UPPER_HALF_BIT) != 0) {
            world.setBlock(0, -1, 0, block, otherHalfBits);
        } else {
            world.setBlock(0, 1, 0, block, UPPER_HALF_BIT | otherHalfBits);
        }
    }
}
