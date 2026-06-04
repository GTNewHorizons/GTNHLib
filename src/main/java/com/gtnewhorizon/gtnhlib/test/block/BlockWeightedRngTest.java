package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockWeightedRngTest extends Block {

    public BlockWeightedRngTest() {
        super(Material.wood);
    }

    public static void register() {
        final var testBlock = new BlockWeightedRngTest();
        testBlock.setBlockName("weighted_rng_test");
        GameRegistry.registerBlock(testBlock, "weighted_rng_test");
    }
}
