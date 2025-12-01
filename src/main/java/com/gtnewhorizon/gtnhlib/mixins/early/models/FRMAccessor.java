package com.gtnewhorizon.gtnhlib.mixins.early.models;

import java.util.List;

import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizon.gtnhlib.client.model.loading.BackingResourceManager;

@Mixin(FallbackResourceManager.class)
public interface FRMAccessor extends BackingResourceManager {

    @Override
    @Accessor("resourcePacks")
    List<IResourcePack> nhlib$getResourcePacks();
}
