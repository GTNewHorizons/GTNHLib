package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;

/**
 * The connections of fences, panes and walls, MC 1.7.10 doesn't store them in the meta. Uses the same names as later
 * versions: {@code east}, {@code north}, {@code south}, {@code west} and for walls {@code up}, which is false for
 * straight walls with air above, since only those drop their post.
 *
 * <p>
 * The connections come from the blocks around it, so they can only be set in a {@link BlockStateFakeWorld}, where a
 * copy of the block is placed where it connects. In other worlds setting them does nothing.
 */
class HorizontalConnectionProperty implements BooleanBlockProperty {

    private static final ForgeDirection[] SIDES = { ForgeDirection.EAST, ForgeDirection.NORTH, ForgeDirection.SOUTH,
            ForgeDirection.WEST };

    /** null for {@code up} */
    private final ForgeDirection side;

    private HorizontalConnectionProperty(ForgeDirection side) {
        this.side = side;
    }

    static void register() {
        for (ForgeDirection side : SIDES) {
            HorizontalConnectionProperty property = new HorizontalConnectionProperty(side);
            registerProperty(BlockFence.class, property);
            registerProperty(BlockPane.class, property);
            registerProperty(BlockWall.class, property);
        }
        registerProperty(BlockWall.class, new HorizontalConnectionProperty(null));
    }

    @Override
    public String getName() {
        return (this.side != null) ? this.side.name().toLowerCase(Locale.ROOT) : "up";
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable -> true;
            default -> false;
        };
    }

    @Override
    public Boolean getValue(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (this.side != null) return canConnect(block, world, x, y, z, this.side);

        // same check as the vanilla renderer
        boolean east = canConnect(block, world, x, y, z, ForgeDirection.EAST);
        boolean north = canConnect(block, world, x, y, z, ForgeDirection.NORTH);
        boolean south = canConnect(block, world, x, y, z, ForgeDirection.SOUTH);
        boolean west = canConnect(block, world, x, y, z, ForgeDirection.WEST);
        boolean straight = (east && west && !north && !south) || (north && south && !east && !west);
        return !straight || !world.isAirBlock(x, y + 1, z);
    }

    /** the same checks as the vanilla renderers */
    private static boolean canConnect(Block block, IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        int sideX = x + side.offsetX;
        int sideZ = z + side.offsetZ;
        if (block instanceof BlockFence fence) return fence.canConnectFenceTo(world, sideX, y, sideZ);
        if (block instanceof BlockPane pane) return pane.canPaneConnectTo(world, sideX, y, sideZ, side);
        if (block instanceof BlockWall wall) return wall.canConnectWallTo(world, sideX, y, sideZ);
        return false;
    }

    /**
     * vanilla connects fences, panes and walls to the same block, so a copy of it rebuilds the connection. For
     * {@code up} any block works, the renderer only checks that it isn't air.
     */
    @Override
    public void setValue(World world, int x, int y, int z, Boolean value) {
        if (!(world instanceof BlockStateFakeWorld) || !value) return;

        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (this.side != null) {
            world.setBlock(x + this.side.offsetX, y, z + this.side.offsetZ, block, meta, 2);
        } else {
            world.setBlock(x, y + 1, z, block, meta, 2);
        }
    }
}
