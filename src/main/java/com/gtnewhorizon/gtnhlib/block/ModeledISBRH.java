package com.gtnewhorizon.gtnhlib.block;

import net.minecraft.block.Block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

// TODO: integrate Roadhog's ISBRH
public abstract class ModeledISBRH implements ISimpleBlockRenderingHandler {

    /**
     * Any blocks using a JSON model should return this for {@link Block#getRenderType()}.
     */
    int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    @Override
    public int getRenderId() {
        return JSON_ISBRH_ID;
    }
}
