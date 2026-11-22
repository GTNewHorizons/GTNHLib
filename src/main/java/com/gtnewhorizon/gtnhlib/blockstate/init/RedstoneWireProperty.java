package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import java.lang.reflect.Type;
import java.util.Locale;

import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;

/**
 * Block state properties for redstone wire, MC 1.7.10 doesn't store its connections. Uses the same properties as later
 * versions: {@code east}, {@code north}, {@code south} and {@code west}, with {@code none}, {@code side} for flat
 * connections and {@code up} where the wire climbs the block on that side.
 *
 * <p>
 * They can only be set in a {@link BlockStateFakeWorld}, where wire is placed next to it, or a full block with wire on
 * top. In other worlds setting them does nothing.
 */
class RedstoneWireProperty implements BlockProperty<String> {

    private static final ForgeDirection[] SIDE_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.NORTH,
            ForgeDirection.SOUTH, ForgeDirection.WEST };
    /** the direction passed to {@link BlockRedstoneWire#isPowerProviderOrWire}, the same as the vanilla renderer */
    private static final int[] SIDE_POWER_DIRECTIONS = { 3, 2, 0, 1 };

    private static final String NONE = "none";
    private static final String SIDE = "side";
    private static final String UP = "up";

    private final ForgeDirection side;
    private final int powerDirection;

    private RedstoneWireProperty(ForgeDirection side, int powerDirection) {
        this.side = side;
        this.powerDirection = powerDirection;
    }

    static void register() {
        for (int side = 0; side < SIDE_DIRECTIONS.length; side++) {
            registerProperty(
                    BlockRedstoneWire.class,
                    new RedstoneWireProperty(SIDE_DIRECTIONS[side], SIDE_POWER_DIRECTIONS[side]));
        }
    }

    @Override
    public String getName() {
        return this.side.name().toLowerCase(Locale.ROOT);
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

    /** the same checks as the vanilla renderer */
    @Override
    public String getValue(IBlockAccess world, int x, int y, int z) {
        boolean canClimb = !world.getBlock(x, y + 1, z).isBlockNormalCube();

        int sideX = x + this.side.offsetX;
        int sideZ = z + this.side.offsetZ;
        boolean sideIsNormalCube = world.getBlock(sideX, y, sideZ).isBlockNormalCube();

        // the renderer only draws the wire up the side for wire, not for other power providers above
        if (canClimb && sideIsNormalCube && world.getBlock(sideX, y + 1, sideZ) == Blocks.redstone_wire) {
            return UP;
        }
        if (BlockRedstoneWire.isPowerProviderOrWire(world, sideX, y, sideZ, this.powerDirection)
                || (!sideIsNormalCube && BlockRedstoneWire.isPowerProviderOrWire(world, sideX, y - 1, sideZ, -1))
                || (canClimb && sideIsNormalCube
                        && BlockRedstoneWire.isPowerProviderOrWire(world, sideX, y + 1, sideZ, -1))) {
            return SIDE;
        }
        return NONE;
    }

    /** wire next to it connects flat, a full block with wire on top makes it climb */
    @Override
    public void setValue(World world, int x, int y, int z, String value) {
        if (!(world instanceof BlockStateFakeWorld)) return;

        int sideX = x + this.side.offsetX;
        int sideZ = z + this.side.offsetZ;
        if (value.equals(SIDE)) {
            world.setBlock(sideX, y, sideZ, Blocks.redstone_wire, 0, 2);
        } else if (value.equals(UP)) {
            world.setBlock(sideX, y, sideZ, Blocks.stone, 0, 2);
            world.setBlock(sideX, y + 1, sideZ, Blocks.redstone_wire, 0, 2);
        }
    }

    @Override
    public String stringify(String value) {
        return value;
    }

    @Override
    public String parse(String text) throws InvalidPropertyTextException {
        if (!text.equals(NONE) && !text.equals(SIDE) && !text.equals(UP)) {
            throw new InvalidPropertyTextException("Illegal redstone wire connection: '" + text + "'");
        }
        return text;
    }
}
