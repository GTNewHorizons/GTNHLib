package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import static cpw.mods.fml.relauncher.Side.CLIENT;

import net.minecraftforge.client.model.IModelCustom;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

import cpw.mods.fml.relauncher.SideOnly;

public interface IModelCustomExt extends IModelCustom {

    @SideOnly(CLIENT)
    void rebuildVBO();

    @SideOnly(CLIENT)
    void renderAllVBO();

    void setVertexFormat(VertexFormat format);
}
