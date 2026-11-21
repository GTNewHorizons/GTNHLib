package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for fire, its shape depends on the blocks around it. Fire on a solid or flammable block is drawn
 * standing on it and has no data, like the default state of later versions. Otherwise it's drawn on the sides of the
 * flammable blocks around it, using the same properties as later versions: {@code east}, {@code north}, {@code south},
 * {@code up} and {@code west}.
 */
class FireData implements BlockStateDataHandler {

    private static final FireData INSTANCE = new FireData();

    /** sorted by name */
    private static final String[] SIDE_NAMES = { "east", "north", "south", "up", "west" };
    private static final ForgeDirection[] SIDE_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.NORTH,
            ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.WEST };

    static void register() {
        BlockStateDataRegistry.register(BlockFire.class, INSTANCE);
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
        if (World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                || Blocks.fire.canCatchFire(world, x, y - 1, z, ForgeDirection.UP)) {
            return null;
        }

        StringBuilder data = new StringBuilder();
        for (int side = 0; side < SIDE_NAMES.length; side++) {
            ForgeDirection direction = SIDE_DIRECTIONS[side];
            // the face of the neighbor that's facing the fire
            boolean burning = Blocks.fire.canCatchFire(
                    world,
                    x + direction.offsetX,
                    y + direction.offsetY,
                    z + direction.offsetZ,
                    direction.getOpposite());
            if (data.length() > 0) data.append(',');
            data.append(SIDE_NAMES[side]).append('=').append(burning);
        }
        return data.toString();
    }

    /** planks are flammable, which is all the renderer checks */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        for (String pair : data.split(",")) {
            int separator = pair.indexOf('=');
            if (!pair.substring(separator + 1).equals("true")) continue;

            String name = pair.substring(0, separator);
            for (int side = 0; side < SIDE_NAMES.length; side++) {
                if (SIDE_NAMES[side].equals(name)) {
                    ForgeDirection direction = SIDE_DIRECTIONS[side];
                    world.setBlock(direction.offsetX, direction.offsetY, direction.offsetZ, Blocks.planks, 0);
                }
            }
        }
    }

    /** fire without data stands on the block below, stone is solid */
    @Override
    public void decodeDefault(Block block, int meta, BlockStateDataWorld world) {
        world.setBlock(0, -1, 0, Blocks.stone, 0);
    }
}
