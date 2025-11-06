package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class FullTestBlock extends Block {
    public FullTestBlock() {
        super(Material.wood);
        // it's wood
        setHardness(2.0F);
        setResistance(5.0F);
        setLightOpacity(255);
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }
}
