package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStem;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;

/**
 * Block state property for pumpkin and melon stems, a fully grown stem bends towards its fruit next to it. Uses the
 * same property as later versions: {@code facing}, the fruit's direction, {@code up} for stems without a fruit.
 *
 * <p>
 * It can only be set in a {@link BlockStateFakeWorld}, where the fruit is placed on that side. In other worlds setting
 * it does nothing.
 */
class StemProperty implements DirectionBlockProperty {

    private static final StemProperty INSTANCE = new StemProperty();

    /** in the order {@link BlockStem#getState} checks them */
    private static final ForgeDirection[] FRUIT_DIRECTIONS = { ForgeDirection.WEST, ForgeDirection.EAST,
            ForgeDirection.NORTH, ForgeDirection.SOUTH };

    static void register() {
        registerProperty(BlockStem.class, INSTANCE);
    }

    @Override
    public String getName() {
        return "facing";
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable -> true;
            default -> false;
        };
    }

    @Override
    public boolean isValidDirection(ForgeDirection value) {
        return value != ForgeDirection.DOWN && value != ForgeDirection.UNKNOWN;
    }

    /** the same check as {@link BlockStem#getState}, which only exists on the client */
    @Override
    public ForgeDirection getValue(IBlockAccess world, int x, int y, int z) {
        if (world.getBlockMetadata(x, y, z) < 7) return ForgeDirection.UP;

        Block fruit = ((BlockStem) world.getBlock(x, y, z)).field_149877_a;
        for (ForgeDirection direction : FRUIT_DIRECTIONS) {
            if (world.getBlock(x + direction.offsetX, y, z + direction.offsetZ) == fruit) return direction;
        }
        return ForgeDirection.UP;
    }

    /** the stem's own fruit on that side */
    @Override
    public void setValue(World world, int x, int y, int z, ForgeDirection facing) {
        if (!(world instanceof BlockStateFakeWorld)) return;

        for (ForgeDirection direction : FRUIT_DIRECTIONS) {
            if (direction == facing) {
                Block block = world.getBlock(x, y, z);
                world.setBlock(x + direction.offsetX, y, z + direction.offsetZ, ((BlockStem) block).field_149877_a, 0, 2);
            }
        }
    }
}
