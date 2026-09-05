package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.client.model.loading.TexHelper;
import com.llamalad7.mixinextras.sugar.Local;

@SuppressWarnings("UnusedMixin")
@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

    @Shadow
    protected abstract ResourceLocation completeResourceLocation(ResourceLocation location, int mipId);

    @Redirect(
            method = "loadTextureAtlas",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureMap;completeResourceLocation(Lnet/minecraft/util/ResourceLocation;I)Lnet/minecraft/util/ResourceLocation;",
                    ordinal = 0))
    private ResourceLocation nhlib$unbaseTextures(TextureMap instance, ResourceLocation location, int mipId,
            @Local(name = "textureatlassprite") TextureAtlasSprite sprite) {
        if (((TexHelper.BaseCheckedTex) sprite).nhlib$hasBase()) return completeResourceLocation(location, mipId);

        // I don't *like* completely replacing the internals, but it's better than an overwrite.
        // And more importantly, I'm really not sure how I'd do this (cleanly) with a different method.
        if (mipId == 0) {
            return new ResourceLocation(
                    location.getResourceDomain(),
                    String.format("textures/%s%s", location.getResourcePath(), ".png"));
        }

        var path = location.getResourcePath().split("/", 2);
        return new ResourceLocation(
                location.getResourceDomain(),
                String.format("textures/%s/mipmaps/%s.%d%s", path[0], path[1], mipId, ".png"));
    }
}
