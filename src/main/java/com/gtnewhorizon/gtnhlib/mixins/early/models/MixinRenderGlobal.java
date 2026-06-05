package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.GTNHLibConfig.testCrackTexture;
import static com.gtnewhorizon.gtnhlib.client.model.Textures.ALT_CRACK_TEX;
import static com.gtnewhorizon.gtnhlib.client.model.Textures.BLOCK_CRACK_TEXS;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Shadow
    @Final
    private TextureManager renderEngine;

    @Inject(
            method = "drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockUsingTexture(Lnet/minecraft/block/Block;IIILnet/minecraft/util/IIcon;)V",
                    shift = At.Shift.AFTER))
    private void nhlib$wrapBlockCrackTex(Tessellator tessellator, EntityLivingBase entity, float partialTicks,
            CallbackInfo ci, @Local Block block, @Local DestroyBlockProgress progress, @Local(name = "d0") double x,
            @Local(name = "d1") double y, @Local(name = "d2") double z) {
        if (!((BlockModelInfo) block).nhlib$isModeled()) return;

        this.renderEngine
                .bindTexture(testCrackTexture ? ALT_CRACK_TEX : BLOCK_CRACK_TEXS[progress.getPartialBlockDamage()]);
        tessellator.draw();

        this.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        tessellator.startDrawingQuads();
        tessellator.setTranslation(-x, -y, -z);
        tessellator.disableColor();
    }
}
