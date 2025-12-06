package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.core.GTNHLibCore;

/**
 * A utility class that helps with loading textures. <br>
 * This class is subject to change.
 */
@Beta
public class TextureLoader {

    public static final IntBuffer dataBuffer;

    public static BufferedImage getBufferedImage(IResource resource) throws IOException {
        final InputStream inputStream = resource.getInputStream();
        final BufferedImage image = ImageIO.read(inputStream);
        inputStream.close();
        return image;
    }

    public static int[] mergeImages(BufferedImage[] images, int totalWidth, int maxHeight) {
        final int amount = images.length;
        int[] atlasRGB = new int[totalWidth * maxHeight];

        // Calculate max width to reduce allocations
        int maxWidth = 0;
        for (BufferedImage image : images) {
            if (image.getWidth() > maxWidth) maxWidth = image.getWidth();
        }

        int[] rgb = new int[maxWidth * maxHeight];

        for (int i = 0; i < amount; i++) {
            BufferedImage texture = images[i];
            final int width = texture.getWidth();
            final int height = texture.getHeight();

            texture.getRGB(0, 0, width, height, rgb, 0, width);

            for (int y = 0; y < height; y++) {
                final int src = y * width;
                final int dest = (y * totalWidth) + (i * width);

                System.arraycopy(rgb, src, atlasRGB, dest, width);
            }
        }
        return atlasRGB;
    }

    public static int[] getImagePixels(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        return pixels;
    }

    public static int createBindTexture(BufferedImage image) {
        return createBindTexture(GL11.GL_RGBA, GL12.GL_BGRA, image);
    }

    public static int createBindTexture(int format, BufferedImage image) {
        return createBindTexture(format, GL12.GL_BGRA, image);
    }

    public static int createBindTexture(int format, int pixelFormat, BufferedImage image) {
        return createBindTexture(format, pixelFormat, image.getWidth(), image.getHeight(), getImagePixels(image));
    }

    public static int createBindTextureAtlas(int atlasWidth, int atlasHeight, BufferedImage[] images) {
        return createBindTextureAtlas(GL11.GL_RGBA, GL12.GL_BGRA, atlasWidth, atlasHeight, images);
    }

    public static int createBindTextureAtlas(int format, int pixelFormat, int atlasWidth, int atlasHeight,
            BufferedImage[] images) {
        return createBindTexture(
                format,
                pixelFormat,
                atlasWidth,
                atlasHeight,
                mergeImages(images, atlasWidth, atlasHeight));
    }

    public static int createBindTexture(int format, int pixelFormat, int width, int height, int[] pixels) {
        return createBindTexture(format, pixelFormat, width, height, pixels, GL11.GL_NEAREST);
    }

    public static int createBindTexture(int format, int pixelFormat, int width, int height, int[] pixels, int filter) {
        dataBuffer.clear();
        dataBuffer.put(pixels).flip();
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                format,
                width,
                height,
                0,
                pixelFormat,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                dataBuffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        return texture;
    }

    static {
        IntBuffer buffer;
        try {
            Field f = TextureUtil.class.getDeclaredField(GTNHLibCore.isObf() ? "field_111000_c" : "dataBuffer");
            f.setAccessible(true);
            buffer = (IntBuffer) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            buffer = GLAllocation.createDirectIntBuffer(4194304);
        }
        dataBuffer = buffer;
    }
}
