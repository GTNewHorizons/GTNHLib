package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty;

/**
 * Block state property for double plants (IE double tall grass, sunflowers, lilacs). The upper half only knows it's an
 * upper half, its plant is the meta of the lower half, and without that half vanilla doesn't render it at all. The
 * property is {@code variant} with the lower half's meta as its value. Lower halves have their own meta as it, like the
 * variant of later versions.
 *
 * <p>
 * It can only be set in a {@link BlockStateFakeWorld}, where the lower half is placed below. In other worlds setting it
 * does nothing.
 */
class DoublePlantProperty implements IntegerBlockProperty {

    private static final DoublePlantProperty INSTANCE = new DoublePlantProperty();

    /** double tall grass, the most common */
    private static final int DEFAULT_LOWER_META = 2;

    static void register() {
        registerProperty(BlockDoublePlant.class, INSTANCE);
    }

    @Override
    public String getName() {
        return "variant";
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable -> true;
            default -> false;
        };
    }

    /** vanilla doesn't render an upper half without its lower half, so without one it gets double tall grass */
    @Override
    public Integer getValue(IBlockAccess world, int x, int y, int z) {
        // lower halves store their plant themselves
        int meta = world.getBlockMetadata(x, y, z);
        if (!BlockDoublePlant.func_149887_c(meta)) return meta;

        Block block = world.getBlock(x, y, z);
        if (world.getBlock(x, y - 1, z) != block) return DEFAULT_LOWER_META;

        int lowerMeta = world.getBlockMetadata(x, y - 1, z);
        if (BlockDoublePlant.func_149887_c(lowerMeta)) return DEFAULT_LOWER_META;

        return lowerMeta;
    }

    /** the lower half below gives the upper half its plant, lower halves store it themselves */
    @Override
    public void setValue(World world, int x, int y, int z, Integer lowerMeta) {
        if (!(world instanceof BlockStateFakeWorld)) return;

        if (!BlockDoublePlant.func_149887_c(world.getBlockMetadata(x, y, z))) return;
        if (lowerMeta < 0 || lowerMeta > 15 || BlockDoublePlant.func_149887_c(lowerMeta)) return;

        world.setBlock(x, y - 1, z, world.getBlock(x, y, z), lowerMeta, 2);
    }
}
