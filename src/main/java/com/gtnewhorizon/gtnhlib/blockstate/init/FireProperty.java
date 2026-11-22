package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import java.util.Locale;

import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;

/**
 * Block state properties for fire, its shape depends on the blocks around it. Fire on a solid or flammable block is
 * drawn standing on it and has all properties false, like the default state of later versions. Otherwise it's drawn on
 * the sides of the flammable blocks around it, using the same properties as later versions: {@code east},
 * {@code north}, {@code south}, {@code up} and {@code west}.
 *
 * <p>
 * They can only be set in a {@link BlockStateFakeWorld}, where flammable blocks are placed on the burning sides, or a
 * solid block below without any. In other worlds setting them does nothing.
 */
class FireProperty implements BooleanBlockProperty {

    private static final ForgeDirection[] SIDE_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.NORTH,
            ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.WEST };

    private final ForgeDirection side;

    private FireProperty(ForgeDirection side) {
        this.side = side;
    }

    static void register() {
        for (ForgeDirection side : SIDE_DIRECTIONS) {
            registerProperty(BlockFire.class, new FireProperty(side));
        }
    }

    @Override
    public String getName() {
        return this.side.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable -> true;
            default -> false;
        };
    }

    /** the same checks as the vanilla renderer */
    @Override
    public Boolean getValue(IBlockAccess world, int x, int y, int z) {
        if (World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                || Blocks.fire.canCatchFire(world, x, y - 1, z, ForgeDirection.UP)) {
            return false;
        }

        // the face of the neighbor that's facing the fire
        return Blocks.fire.canCatchFire(
                world,
                x + this.side.offsetX,
                y + this.side.offsetY,
                z + this.side.offsetZ,
                this.side.getOpposite());
    }

    /**
     * Planks are flammable, which is all the renderer checks. Fire without any burning side stands on the block below,
     * stone is solid. The properties are set one by one, so the stone is placed while no side burns yet and removed
     * again once one does.
     */
    @Override
    public void setValue(World world, int x, int y, int z, Boolean burning) {
        if (!(world instanceof BlockStateFakeWorld)) return;

        if (burning) {
            world.setBlock(x + this.side.offsetX, y + this.side.offsetY, z + this.side.offsetZ, Blocks.planks, 0, 2);
            world.setBlockToAir(x, y - 1, z);
            return;
        }

        for (ForgeDirection side : SIDE_DIRECTIONS) {
            if (world.getBlock(x + side.offsetX, y + side.offsetY, z + side.offsetZ) == Blocks.planks) return;
        }
        world.setBlock(x, y - 1, z, Blocks.stone, 0, 2);
    }
}
