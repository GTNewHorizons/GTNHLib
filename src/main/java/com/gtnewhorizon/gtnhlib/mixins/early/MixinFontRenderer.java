package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;
import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;

// Low priority to allow for other overrides
@Mixin(value = FontRenderer.class, priority = 200)
public abstract class MixinFontRenderer implements IFontParameters {

    @Shadow
    public abstract int getCharWidth(char chr);

    @Override
    public float getCharWidthFine(char chr) {
        return getCharWidth(chr);
    }

    /**
     * @author DeathFuel
     * @reason Replace with an implementation that respects color code reset rules and works with Angelica custom fonts
     */
    @Overwrite
    public int getStringWidth(String text) {
        return FontRendering.getStringWidth(text, (FontRenderer) (Object) this);
    }

    /**
     * @author DeathFuel
     * @reason (as above)
     */
    @Overwrite
    private int sizeStringToWidth(String str, int wrapWidth) {
        return FontRendering.sizeStringToWidth(str, wrapWidth, (FontRenderer) (Object) this);
    }

    /**
     * @author DeathFuel
     * @reason (as above)
     */
    @Overwrite
    public String trimStringToWidth(String str, int trimWidth, boolean reverse) {
        return FontRendering.trimStringToWidth(str, trimWidth, reverse, (FontRenderer) (Object) this);
    }
}
