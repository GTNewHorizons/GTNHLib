package com.gtnewhorizon.gtnhlib.test.block;

import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockUVRotationTest extends BlockTest
{

    public BlockUVRotationTest()
    {
        super();
    }

    // Note this model is just rotating the +-y texture to check rotation with UVLock
    public static void register() {
        final var testBlock = new BlockUVRotationTest();
        testBlock.setBlockName("rotation_test");
        GameRegistry.registerBlock(testBlock, "rotation_test");
        BlockPropertyRegistry.registerBlockItemProperty(testBlock, testBlock.FACING_PROP, ForgeDirection.EAST);
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }
}
