package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockRngTest extends Block {

    public BlockRngTest() {
        super(Material.wood);
    }

    public static void register() {
        final var testBlock = new BlockRngTest();
        testBlock.setBlockName("rng_test");
        GameRegistry.registerBlock(testBlock, "rng_test");
    }
}
