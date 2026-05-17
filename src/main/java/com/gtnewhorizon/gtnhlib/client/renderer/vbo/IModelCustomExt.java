package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import static cpw.mods.fml.relauncher.Side.CLIENT;

import net.minecraftforge.client.model.IModelCustom;

import cpw.mods.fml.relauncher.SideOnly;

@Deprecated // Replaced in favor for WavefrontVBOBuilder. Will be migrated into Angelica for internal use later on.
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
