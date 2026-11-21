package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for fences, panes and walls, MC 1.7.10 doesn't store their connections in the meta. Uses the same
 * properties as later versions: {@code east}, {@code north}, {@code south}, {@code west} and for walls {@code up},
 * which is false for straight walls with air above, since only those drop their post.
 */
class HorizontalConnectionData implements BlockStateDataHandler {

    private static final HorizontalConnectionData INSTANCE = new HorizontalConnectionData();

    /** sorted by name */
    private static final String[] SIDE_NAMES = { "east", "north", "south", "west" };
    private static final ForgeDirection[] SIDE_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.NORTH,
            ForgeDirection.SOUTH, ForgeDirection.WEST };
    private static final String UP_NAME = "up";

    static void register() {
        BlockStateDataRegistry.register(BlockFence.class, INSTANCE);
        BlockStateDataRegistry.register(BlockPane.class, INSTANCE);
        BlockStateDataRegistry.register(BlockWall.class, INSTANCE);
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
        boolean[] connected = new boolean[SIDE_NAMES.length];
        for (int side = 0; side < SIDE_NAMES.length; side++) {
            ForgeDirection direction = SIDE_DIRECTIONS[side];
            connected[side] = canConnect(block, world, x + direction.offsetX, y, z + direction.offsetZ, direction);
        }

        StringBuilder data = new StringBuilder();
        for (int side = 0; side < SIDE_NAMES.length; side++) {
            // up goes between south and west
            if (block instanceof BlockWall && SIDE_NAMES[side].equals("west")) {
                // same check as the vanilla renderer
                boolean straight = (connected[0] && connected[3] && !connected[1] && !connected[2])
                        || (connected[1] && connected[2] && !connected[0] && !connected[3]);
                appendValue(data, UP_NAME, !straight || !world.isAirBlock(x, y + 1, z));
            }
            appendValue(data, SIDE_NAMES[side], connected[side]);
        }

        return data.toString();
    }

    private static void appendValue(StringBuilder data, String name, boolean value) {
        if (data.length() > 0) data.append(',');
        data.append(name).append('=').append(value);
    }

    /** the same checks as the vanilla renderers */
    private static boolean canConnect(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        if (block instanceof BlockFence fence) return fence.canConnectFenceTo(world, x, y, z);
        if (block instanceof BlockPane pane) return pane.canPaneConnectTo(world, x, y, z, direction);
        if (block instanceof BlockWall wall) return wall.canConnectWallTo(world, x, y, z);
        return false;
    }

    /** vanilla connects fences, panes and walls to the same block, so copies of it rebuild the connections */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        for (String pair : data.split(",")) {
            int separator = pair.indexOf('=');
            if (!pair.substring(separator + 1).equals("true")) continue;

            String name = pair.substring(0, separator);
            if (name.equals(UP_NAME)) {
                // any block works, the renderer only checks that it isn't air
                world.setBlock(0, 1, 0, block, meta);
                continue;
            }

            for (int side = 0; side < SIDE_NAMES.length; side++) {
                if (SIDE_NAMES[side].equals(name)) {
                    ForgeDirection direction = SIDE_DIRECTIONS[side];
                    world.setBlock(direction.offsetX, 0, direction.offsetZ, block, meta);
                }
            }
        }
    }
}
