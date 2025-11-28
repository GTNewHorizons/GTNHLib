package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class AnimatedTexture {

    private final SpriteAnimationMetadata metadata;
    public final int texture;
    private final float heightUnit;

    private int lastTextureUpdate;

    public AnimatedTexture(ResourceLocation resourceLocation) {
        final IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        try {
            IResource resource = resourceManager.getResource(resourceLocation);
            BufferedImage image = TextureLoader.getBufferedImage(resource);
            AnimationMetadataSection metadata = (AnimationMetadataSection) resource.getMetadata("animation");
            if (metadata.getFrameCount() == 0) {
                final int columns = image.getHeight() / image.getWidth();
                List<AnimationFrame> arraylist = new ArrayList<>();
                for (int i1 = 0; i1 < columns; i1++) {
                    arraylist.add(new AnimationFrame(i1, -1));
                }

                metadata = new AnimationMetadataSection(arraylist, 16, columns * 16, metadata.getFrameTime());
            }
            this.metadata = new SpriteAnimationMetadata(metadata);
            this.texture = TextureLoader.createBindTexture(GL11.GL_RGBA, GL12.GL_BGRA, image);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            heightUnit = 1f / (image.getHeight() / 16f);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void checkUpdateTexture() {
        if (lastTextureUpdate != Minecraft.getMinecraft().thePlayer.ticksExisted) {
            lastTextureUpdate = Minecraft.getMinecraft().thePlayer.ticksExisted;
            metadata.updateAnimation();
        }
    }

    public final void bindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    public final float getMinU() {
        checkUpdateTexture();
        return 0;
    }

    public final float getMaxU() {
        checkUpdateTexture();
        return 1;
    }

    public final float getMinV() {
        checkUpdateTexture();
        return metadata.index * heightUnit;
    }

    public final float getMaxV() {
        checkUpdateTexture();
        return (metadata.index + 1) * heightUnit;
    }
}
