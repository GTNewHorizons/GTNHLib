package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockTest extends Block {

    public BlockTest() {
        super(Material.wood);
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }
}
