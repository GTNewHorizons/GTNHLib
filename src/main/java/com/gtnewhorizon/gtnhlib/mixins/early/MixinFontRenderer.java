package com.gtnewhorizon.gtnhlib.mixins.early;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;
import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Low priority to allow for other overrides
@Mixin(value = FontRenderer.class, priority = 200)
public abstract class MixinFontRenderer implements IFontParameters {

    @Shadow
    public abstract int getCharWidth(char chr);

    @Override
    public float getCharWidthFine(char chr) {
        return getCharWidth(chr);
    }

    @Inject(method = "sizeStringToWidth(Ljava/lang/String;I)I", at = @At("HEAD"), cancellable = true)
    private void sizeStringToWidth(String str, int wrapWidth, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FontRendering.sizeStringToWidth(str, wrapWidth, (FontRenderer) (Object) this));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    public void trimStringToWidth(String str, int trimWidth, boolean reverse, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(FontRendering.trimStringToWidth(str, trimWidth, reverse, (FontRenderer) (Object) this));
    }
}
