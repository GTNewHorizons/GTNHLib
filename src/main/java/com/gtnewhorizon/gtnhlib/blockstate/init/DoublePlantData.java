package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for double plants (IE double tall grass, sunflowers, lilacs). The upper half only knows it's an
 * upper half, its plant is the meta of the lower half, and without that half vanilla doesn't render it at all. The data
 * is {@code variant} with the lower half's meta as its value, only upper halves have data.
 */
class DoublePlantData implements BlockStateDataHandler {

    private static final DoublePlantData INSTANCE = new DoublePlantData();

    private static final String VARIANT_PREFIX = "variant=";

    static void register() {
        BlockStateDataRegistry.register(BlockDoublePlant.class, INSTANCE);
    }

    @Override
    public Type getType() {
        return Type.LOCAL_ONLY;
    }

    @Override
    public String encode(IBlockAccess world, int x, int y, int z, Block block, int meta) {
        // lower halves store their plant themselves
        if (!BlockDoublePlant.func_149887_c(meta)) return null;

        if (world.getBlock(x, y - 1, z) != block) return null;

        int lowerMeta = world.getBlockMetadata(x, y - 1, z);
        if (BlockDoublePlant.func_149887_c(lowerMeta)) return null;

        return VARIANT_PREFIX + lowerMeta;
    }

    /** the lower half below gives the upper half its plant */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        if (!data.startsWith(VARIANT_PREFIX)) return;

        int lowerMeta;
        try {
            lowerMeta = Integer.parseInt(data.substring(VARIANT_PREFIX.length()));
        } catch (NumberFormatException e) {
            return;
        }
        if (lowerMeta < 0 || lowerMeta > 15 || BlockDoublePlant.func_149887_c(lowerMeta)) return;

        world.setBlock(0, -1, 0, block, lowerMeta);
    }
}
