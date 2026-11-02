package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.gtnhlib.client.model.loading.TexHelper;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite implements TexHelper.BaseCheckedTex {

    @Unique
    private boolean nhlib$hasBase = true;

    @Override
    public void nhlib$setHasBase(boolean hasBase) {
        nhlib$hasBase = hasBase;
    }

    @Override
    public boolean nhlib$hasBase() {
        return nhlib$hasBase;
    }
}
