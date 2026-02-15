package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import static cpw.mods.fml.relauncher.Side.CLIENT;

import net.minecraftforge.client.model.IModelCustom;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

import cpw.mods.fml.relauncher.SideOnly;

public interface IModelCustomExt extends IModelCustom {

    // There's no reason for another mod to call this, it should only be used internally.
    @Deprecated
    @SideOnly(CLIENT)
    void rebuildVBO();

    @SideOnly(CLIENT)
    void renderAllVBO();

    @Deprecated // Same as renderAllVBO
    @SideOnly(CLIENT)
    void renderAllVAO();
}
