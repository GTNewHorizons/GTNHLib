package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;

@Mixin(Block.class)
public class MixinBlock_ModelInfo implements BlockModelInfo {

    /// This is a shadowed field, not a unique one, because we injected it with ASM. See
    /// {@link com.gtnewhorizon.gtnhlib.core.fml.transformers.BlockIconTransformer}
    @SuppressWarnings("MixinAnnotationTarget")
    @Shadow(remap = false)
    private boolean nhlib$isModeled = false;

    @Override
    public boolean nhlib$isModeled() {
        return nhlib$isModeled;
    }

    @Override
    public void nhlib$setModeled(boolean modeled) {
        nhlib$isModeled = modeled;
    }
}
