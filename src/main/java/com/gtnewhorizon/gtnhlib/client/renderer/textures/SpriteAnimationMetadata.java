package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;

public final class SpriteAnimationMetadata {

    public final AnimationMetadataSection metadata;
    public int tickCounter;
    public int frameCounter;
    public int index;

    public SpriteAnimationMetadata(IResource mcmeta, BufferedImage image) {
        this((AnimationMetadataSection) mcmeta.getMetadata("animation"), image);
    }

    public SpriteAnimationMetadata(AnimationMetadataSection metadata, BufferedImage image) {
        if (metadata.getFrameCount() == 0) {
            final int width = image.getWidth();
            final int height = image.getHeight();
            final int columns = height / width;
            List<AnimationFrame> list = new ArrayList<>(columns);
            for (int i1 = 0; i1 < columns; i1++) {
                list.add(new AnimationFrame(i1, -1));
            }

            metadata = new AnimationMetadataSection(list, width, height, metadata.getFrameTime());
        }

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
