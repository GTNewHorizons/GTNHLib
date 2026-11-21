package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStem;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for pumpkin and melon stems, a fully grown stem bends towards its fruit next to it. Uses the same
 * property as later versions: {@code facing}, the fruit's direction. Stems without a fruit have no data.
 */
class StemData implements BlockStateDataHandler {

    private static final StemData INSTANCE = new StemData();

    /** by the direction {@link BlockStem#getState} returns */
    private static final ForgeDirection[] FRUIT_DIRECTIONS = { ForgeDirection.WEST, ForgeDirection.EAST,
            ForgeDirection.NORTH, ForgeDirection.SOUTH };
    private static final String FACING_PREFIX = "facing=";

    static void register() {
        BlockStateDataRegistry.register(BlockStem.class, INSTANCE);
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
        int direction = ((BlockStem) block).getState(world, x, y, z);
        if (direction == -1) return null;

        return FACING_PREFIX + FRUIT_DIRECTIONS[direction].name().toLowerCase();
    }

    /** the stem's own fruit on that side */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        if (!data.startsWith(FACING_PREFIX)) return;

        String facing = data.substring(FACING_PREFIX.length());
        for (ForgeDirection direction : FRUIT_DIRECTIONS) {
            if (direction.name().toLowerCase().equals(facing)) {
                world.setBlock(direction.offsetX, 0, direction.offsetZ, ((BlockStem) block).field_149877_a, 0);
            }
        }
    }
}
