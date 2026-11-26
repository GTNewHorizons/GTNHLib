package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import java.lang.reflect.Type;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;

/**
 * Block state property for doors. Each half only stores part of the door, the facing and open state are in the lower
 * half's meta (see the {@code facing} and {@code open} properties) and the hinge side is in the upper half's meta.
 * Vanilla combines both halves and doesn't render a half at all without the other one. Uses the same property as later
 * versions: {@code hinge}, with {@code left} or {@code right}.
 *
 * <p>
 * In a {@link BlockStateFakeWorld} setting any of them places the missing other half.
 */
class DoorHingeProperty implements BlockProperty<String> {

    private static final DoorHingeProperty INSTANCE = new DoorHingeProperty();

    private static final int UPPER_HALF_BIT = 0b1000;
    /** in the upper half */
    private static final int HINGE_BIT = 0b1;

    private static final String LEFT = "left";
    private static final String RIGHT = "right";

    static void register() {
        registerProperty(BlockDoor.class, INSTANCE);
    }

    /**
     * For properties of the lower half that are set on the upper half.
     *
     * @return whether there's a door at the position of the lower half, in a {@link BlockStateFakeWorld} a missing one
     *         is placed there with all bits 0: closed, facing west
     */
    static boolean getOrPlaceLowerHalf(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) instanceof BlockDoor) return true;
        if (!(world instanceof BlockStateFakeWorld)) return false;

        world.setBlock(x, y, z, world.getBlock(x, y + 1, z), 0, 2);
        return true;
    }

    @Override
    public String getName() {
        return "hinge";
    }

    @Override
    public Type getType() {
        return String.class;
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable, Config -> true;
            default -> false;
        };
    }

    @Override
    public String getValue(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & UPPER_HALF_BIT) == 0) {
            y++;
            if (!(world.getBlock(x, y, z) instanceof BlockDoor)) return LEFT;
            meta = world.getBlockMetadata(x, y, z);
        }
        return ((meta & HINGE_BIT) != 0) ? RIGHT : LEFT;
    }

    /** the upper half gives the lower half its hinge */
    @Override
    public void setValue(World world, int x, int y, int z, String value) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & UPPER_HALF_BIT) == 0) {
            Block block = world.getBlock(x, y, z);
            y++;
            if (!(world.getBlock(x, y, z) instanceof BlockDoor)) {
                if (!(world instanceof BlockStateFakeWorld)) return;
                world.setBlock(x, y, z, block, UPPER_HALF_BIT, 2);
            }
            meta = world.getBlockMetadata(x, y, z);
        }

        int newMeta = value.equals(RIGHT) ? (meta | HINGE_BIT) : (meta & ~HINGE_BIT);
        if (newMeta != meta) {
            world.setBlockMetadataWithNotify(x, y, z, newMeta, 2);
        }
    }

    @Override
    public String stringify(String value) {
        return value;
    }

    @Override
    public String parse(String text) throws InvalidPropertyTextException {
        if (!text.equals(LEFT) && !text.equals(RIGHT)) {
            throw new InvalidPropertyTextException("Illegal door hinge: '" + text + "'");
        }
        return text;
    }
}
