package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;

/**
 * Block state property for fence gates, vanilla draws them lower between two cobblestone walls. Uses the same property
 * as later versions: {@code in_wall}.
 *
 * <p>
 * It can only be set in a {@link BlockStateFakeWorld}, where the walls are placed on both sides. In other worlds
 * setting it does nothing.
 */
class FenceGateProperty implements BooleanBlockProperty {

    private static final FenceGateProperty INSTANCE = new FenceGateProperty();

    static void register() {
        registerProperty(BlockFenceGate.class, INSTANCE);
    }

    @Override
    public String getName() {
        return "in_wall";
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable -> true;
            default -> false;
        };
    }

    /** the same check as the vanilla renderer */
    @Override
    public Boolean getValue(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        int offsetX = getWallOffsetX(meta);
        int offsetZ = getWallOffsetZ(meta);
        return world.getBlock(x - offsetX, y, z - offsetZ) == Blocks.cobblestone_wall
                && world.getBlock(x + offsetX, y, z + offsetZ) == Blocks.cobblestone_wall;
    }

    @Override
    public void setValue(World world, int x, int y, int z, Boolean inWall) {
        if (!(world instanceof BlockStateFakeWorld) || !inWall) return;

        int meta = world.getBlockMetadata(x, y, z);
        int offsetX = getWallOffsetX(meta);
        int offsetZ = getWallOffsetZ(meta);
        world.setBlock(x - offsetX, y, z - offsetZ, Blocks.cobblestone_wall, 0, 2);
        world.setBlock(x + offsetX, y, z + offsetZ, Blocks.cobblestone_wall, 0, 2);
    }

    /** gates with direction 0 or 2 sit between walls to their west and east, the others between north and south */
    private static int getWallOffsetX(int meta) {
        return ((BlockDirectional.getDirection(meta) & 1) == 0) ? 1 : 0;
    }

    private static int getWallOffsetZ(int meta) {
        return ((BlockDirectional.getDirection(meta) & 1) == 0) ? 0 : 1;
    }
}
