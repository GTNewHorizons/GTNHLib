package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockTest extends Block {

    public static final BlockTest INSTANCE = new BlockTest();

    public BlockTest() {
        super(Material.wood);
        setBlockName("block_test");
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

}
