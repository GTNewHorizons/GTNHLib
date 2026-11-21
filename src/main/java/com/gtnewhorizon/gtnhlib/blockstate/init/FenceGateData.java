package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for fence gates, vanilla draws them lower between two cobblestone walls. Uses the same property as
 * later versions: {@code in_wall}, other gates have no data.
 */
class FenceGateData implements BlockStateDataHandler {

    private static final FenceGateData INSTANCE = new FenceGateData();

    private static final String IN_WALL_DATA = "in_wall=true";

    static void register() {
        BlockStateDataRegistry.register(BlockFenceGate.class, INSTANCE);
    }

    @Override
    public Type getType() {
        return Type.NEIGHBOR_DEPENDENT;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    /** the same check as the vanilla renderer */
    @Override
    public String encode(IBlockAccess world, int x, int y, int z, Block block, int meta) {
        int offsetX = getWallOffsetX(meta);
        int offsetZ = getWallOffsetZ(meta);
        boolean inWall = world.getBlock(x - offsetX, y, z - offsetZ) == Blocks.cobblestone_wall
                && world.getBlock(x + offsetX, y, z + offsetZ) == Blocks.cobblestone_wall;
        return inWall ? IN_WALL_DATA : null;
    }

    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        if (!data.equals(IN_WALL_DATA)) return;

        int offsetX = getWallOffsetX(meta);
        int offsetZ = getWallOffsetZ(meta);
        world.setBlock(-offsetX, 0, -offsetZ, Blocks.cobblestone_wall, 0);
        world.setBlock(offsetX, 0, offsetZ, Blocks.cobblestone_wall, 0);
    }

    /** gates with direction 0 or 2 sit between walls to their west and east, the others between north and south */
    private static int getWallOffsetX(int meta) {
        return ((BlockDirectional.getDirection(meta) & 1) == 0) ? 1 : 0;
    }

    private static int getWallOffsetZ(int meta) {
        return ((BlockDirectional.getDirection(meta) & 1) == 0) ? 0 : 1;
    }
}
