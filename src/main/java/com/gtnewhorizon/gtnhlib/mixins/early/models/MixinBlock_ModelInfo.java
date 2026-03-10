package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.gtnhlib.client.model.loading.BlockModelInfo;

@Mixin(Block.class)
public class MixinBlock_ModelInfo implements BlockModelInfo {

    @Unique
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
