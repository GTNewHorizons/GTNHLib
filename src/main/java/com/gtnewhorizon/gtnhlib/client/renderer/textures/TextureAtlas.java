package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

/**
 * An implementation of a texture atlas that does not change its image. <br>
 * Unlike Minecraft's texture atlas, it does not upload the image data every animation update, but instead changes the
 * UV coordinates, resulting in better performance.
 */
public class TextureAtlas {

    private final int texture;
    private final SpriteAnimationMetadata[] animationMetadata;
    private final float heightUnit;

    private int lastTextureUpdate;

    public static TextureAtlas createTextureAtlas(String domain, String location, int amount) {
        ResourceLocation[] resources = new ResourceLocation[amount];
        for (int i = 0; i < amount; i++) {
            resources[i] = new ResourceLocation(domain, location + i + ".png");
        }
        return new TextureAtlas(resources);
    }

    public TextureAtlas(ResourceLocation... resources) {
        final int amount = resources.length;
        this.animationMetadata = new SpriteAnimationMetadata[amount];
        final IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        final BufferedImage[] images = new BufferedImage[amount];
        int maxHeight = 0;
        for (int i = 0; i < amount; i++) {
            ResourceLocation resourceLocation = resources[i];
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
                images[i] = image;
                animationMetadata[i] = new SpriteAnimationMetadata(metadata);

                if (image.getHeight() > maxHeight) {
                    maxHeight = image.getHeight();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        heightUnit = 1f / (maxHeight / 16f);

        final int atlasWidth = amount * 16;
        final int atlasHeight = maxHeight;

        texture = TextureLoader.createBindTextureAtlas(GL11.GL_RGBA, GL12.GL_BGRA, atlasWidth, atlasHeight, images);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public int getTexture() {
        return texture;
    }

    public void bindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getTexture());
    }

    public void uploadUVBuffer(int location) {
        if (Minecraft.getMinecraft().thePlayer.ticksExisted != lastTextureUpdate) {
            lastTextureUpdate = Minecraft.getMinecraft().thePlayer.ticksExisted;

            FloatBuffer uvs = BufferUtils.createFloatBuffer(20);

            for (SpriteAnimationMetadata metadata : animationMetadata) {
                metadata.updateAnimation();
                final float v = metadata.index * heightUnit;
                uvs.put(v);
                uvs.put(v + heightUnit);
            }
            uvs.flip();
            GL20.glUniform2(location, uvs);
        }
    }

}
