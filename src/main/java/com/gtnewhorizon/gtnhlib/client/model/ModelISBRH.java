package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraft.block.Block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

// TODO: integrate Roadhog's ISBRH
public abstract class ModelISBRH implements ISimpleBlockRenderingHandler {

    /**
     * Any blocks using a JSON model should return this for {@link Block#getRenderType()}.
     */
    public int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    @Override
    public int getRenderId() {
        return JSON_ISBRH_ID;
    }
}
