package com.gtnewhorizon.gtnhlib.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;

public class BlockTest extends Block {

    public BlockTest() {
        super(Material.wood);
    }

    @Override
    public int getRenderType() {
        return ModelISBRH.MODEL_ISBRH.JSON_ISBRH_ID;
    }
}
