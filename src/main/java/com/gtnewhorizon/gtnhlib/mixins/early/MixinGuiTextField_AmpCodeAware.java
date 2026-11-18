package com.gtnewhorizon.gtnhlib.mixins.early;

import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.gtnhlib.util.font.IAmpCodeAwareField;

public class MixinGuiTextField_AmpCodeAware implements IAmpCodeAwareField {

    @Unique
    private boolean gtnhlib$ampCodeAware = true;

    @Override
    public boolean gtnhlib$isAmpCodeAware() {
        return this.gtnhlib$ampCodeAware;
    }

    @Override
    public void gtnhlib$setAmpCodeAware(boolean enabled) {
        this.gtnhlib$ampCodeAware = enabled;
    }
}
