package com.gtnewhorizon.gtnhlib.mixins.early.models.particles;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderGlobal.class)
public interface RenderGlobalAccessor {

    @Accessor
    WorldClient getTheWorld();
}
