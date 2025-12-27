package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import java.awt.image.BufferedImage;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
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
            this.metadata = new SpriteAnimationMetadata(resource, image);
            this.texture = TextureLoader.createBindTexture(GL11.GL_RGBA, GL12.GL_BGRA, image);
            heightUnit = 1f / (image.getHeight() / image.getWidth());

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
        return 0;
    }

    public final float getMaxU() {
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
