package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import net.minecraft.client.resources.data.AnimationMetadataSection;

public final class SpriteAnimationMetadata {

    public final AnimationMetadataSection metadata;
    public int tickCounter;
    public int frameCounter;
    public int index;

    public SpriteAnimationMetadata(AnimationMetadataSection metadata) {
        this.metadata = metadata;
    }

    public void updateAnimation() {
        if (++this.tickCounter >= this.metadata.getFrameTimeSingle(this.frameCounter)) {
            this.tickCounter = 0;
            int maxCount = this.metadata.getFrameCount();
            this.frameCounter = (this.frameCounter + 1) % maxCount;
            index = this.metadata.getFrameIndex(this.frameCounter);
        }
    }
}
