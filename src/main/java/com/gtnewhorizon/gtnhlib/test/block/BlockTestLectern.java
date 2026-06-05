package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockTestLectern extends BlockTest {

    public BlockTestLectern() {
        super();
        setHardness(2f);
    }

    public static void register() {
        final var testBlock = new BlockTestLectern();
        testBlock.setBlockName("model_test_lectern");
        GameRegistry.registerBlock(testBlock, "model_test_lectern");

        /// This method allows you to adapt any property to itemstacks, with some default value. This is mostly relevant
        /// for modeled blocks, as they may require some state to match *any* of their models. If you're defining a
        /// custom blockstate, it's better to add an ItemStack default yourself - see [BlockTestTintMul] for an example
        /// of that.
        BlockPropertyRegistry.registerBlockItemProperty(testBlock, testBlock.FACING_PROP, ForgeDirection.EAST);
    }
}
