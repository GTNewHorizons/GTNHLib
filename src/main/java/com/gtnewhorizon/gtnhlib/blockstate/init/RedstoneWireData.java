package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for redstone wire, MC 1.7.10 doesn't store its connections. Uses the same properties as later
 * versions: {@code east}, {@code north}, {@code south} and {@code west}, with {@code side} for flat connections and
 * {@code up} where the wire climbs the block on that side. Wire without connections has no data.
 */
class RedstoneWireData implements BlockStateDataHandler {

    private static final RedstoneWireData INSTANCE = new RedstoneWireData();

    /** sorted by name */
    private static final String[] SIDE_NAMES = { "east", "north", "south", "west" };
    private static final ForgeDirection[] SIDE_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.NORTH,
            ForgeDirection.SOUTH, ForgeDirection.WEST };
    /** the direction passed to {@link BlockRedstoneWire#isPowerProviderOrWire}, the same as the vanilla renderer */
    private static final int[] SIDE_POWER_DIRECTIONS = { 3, 2, 0, 1 };

    private static final String NONE = "none";
    private static final String SIDE = "side";
    private static final String UP = "up";

    static void register() {
        BlockStateDataRegistry.register(BlockRedstoneWire.class, INSTANCE);
    }

    @Override
    public Type getType() {
        return Type.NEIGHBOR_DEPENDENT;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    /** the same checks as the vanilla renderer */
    @Override
    public String encode(IBlockAccess world, int x, int y, int z, Block block, int meta) {
        boolean canClimb = !world.getBlock(x, y + 1, z).isBlockNormalCube();

        StringBuilder data = new StringBuilder();
        boolean connected = false;
        for (int side = 0; side < SIDE_NAMES.length; side++) {
            int sideX = x + SIDE_DIRECTIONS[side].offsetX;
            int sideZ = z + SIDE_DIRECTIONS[side].offsetZ;
            boolean sideIsNormalCube = world.getBlock(sideX, y, sideZ).isBlockNormalCube();

            String value = NONE;
            // the renderer only draws the wire up the side for wire, not for other power providers above
            if (canClimb && sideIsNormalCube && world.getBlock(sideX, y + 1, sideZ) == Blocks.redstone_wire) {
                value = UP;
            } else if (BlockRedstoneWire.isPowerProviderOrWire(world, sideX, y, sideZ, SIDE_POWER_DIRECTIONS[side])
                    || (!sideIsNormalCube && BlockRedstoneWire.isPowerProviderOrWire(world, sideX, y - 1, sideZ, -1))
                    || (canClimb && sideIsNormalCube
                            && BlockRedstoneWire.isPowerProviderOrWire(world, sideX, y + 1, sideZ, -1))) {
                                value = SIDE;
                            }

            connected |= !value.equals(NONE);
            if (data.length() > 0) data.append(',');
            data.append(SIDE_NAMES[side]).append('=').append(value);
        }
        return connected ? data.toString() : null;
    }

    /** wire next to it connects flat, a full block with wire on top makes it climb */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        for (String pair : data.split(",")) {
            int separator = pair.indexOf('=');
            String name = pair.substring(0, separator);
            String value = pair.substring(separator + 1);

            for (int side = 0; side < SIDE_NAMES.length; side++) {
                if (!SIDE_NAMES[side].equals(name)) continue;

                ForgeDirection direction = SIDE_DIRECTIONS[side];
                if (value.equals(SIDE)) {
                    world.setBlock(direction.offsetX, 0, direction.offsetZ, Blocks.redstone_wire, 0);
                } else if (value.equals(UP)) {
                    world.setBlock(direction.offsetX, 0, direction.offsetZ, Blocks.stone, 0);
                    world.setBlock(direction.offsetX, 1, direction.offsetZ, Blocks.redstone_wire, 0);
                }
            }
        }
    }
}
