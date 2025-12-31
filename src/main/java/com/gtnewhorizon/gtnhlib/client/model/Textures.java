package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class Textures {

    public static final ResourceLocation[] BLOCK_CRACK_TEXS = new ResourceLocation[10];
    static {
        for (int i = 0; i < 10; ++i) {
            BLOCK_CRACK_TEXS[i] = new ResourceLocation("textures/blocks/destroy_stage_" + i + ".png");
        }
    }
}
