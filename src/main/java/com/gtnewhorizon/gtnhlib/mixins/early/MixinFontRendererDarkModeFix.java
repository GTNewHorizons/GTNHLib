package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix.DarkModeFixColorProcessor;

@Mixin(value = FontRenderer.class, priority = 1100)
public abstract class MixinFontRendererDarkModeFix {

    @ModifyVariable(method = "drawString(Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true, index = 4)
    private int gtnhlib$adjustDrawStringColor(int color) {
        return DarkModeFixColorProcessor.adjustColorOpaque(color);
    }

    @ModifyVariable(method = "renderString(Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true, index = 4)
    private int gtnhlib$adjustRenderStringColor(int color) {
        return DarkModeFixColorProcessor.adjustColorOpaque(color);
    }
}
