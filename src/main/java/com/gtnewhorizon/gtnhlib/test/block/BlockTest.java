package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import cpw.mods.fml.common.registry.GameRegistry;

/// A plain cube, nothing special about it.
public class BlockTest extends Block {

    public BlockTest() {
        super(Material.wood);
    }

    public static void register() {
        final var testBlock = new BlockTest();
        testBlock.setBlockName("model_test");
        GameRegistry.registerBlock(testBlock, "model_test");
    }

    /// Okay so it's transparent, but that's it. That's the only special thing about it.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }
}
