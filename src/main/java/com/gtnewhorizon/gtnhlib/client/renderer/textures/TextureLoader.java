package com.gtnewhorizon.gtnhlib.client.renderer.textures;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.core.GTNHLibCore;
import com.gtnewhorizons.angelica.glsm.GLStateManager;

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

    public static int[] mergeImages(BufferedImage[] images, int maxHeight) {
        final int amount = images.length;
        int[] atlasRGB = new int[amount * 16 * maxHeight];
        int[] rgb = new int[16 * maxHeight];

        for (int i = 0; i < amount; i++) {
            BufferedImage texture = images[i];

            texture.getRGB(0, 0, 16, texture.getHeight(), rgb, 0, 16);

            for (int y = 0; y < texture.getHeight(); y++) {
                int dest = y * amount * 16 + i * 16;
                int src = y * 16;
                System.arraycopy(rgb, src, atlasRGB, dest, 16);
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
        return createBindTexture(format, pixelFormat, atlasWidth, atlasHeight, mergeImages(images, atlasHeight));
    }

    public static int createBindTexture(int format, int pixelFormat, int width, int height, int[] pixels) {
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
        return texture;
    }

    public static void copyTextureToFile(int textureID, File output) {
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        BufferedImage bufferedimage = new BufferedImage(width, height, 1);
        final int[] pixelValues = ((DataBufferInt) bufferedimage.getRaster().getDataBuffer()).getData();
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(pixelValues.length);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        pixelBuffer.get(pixelValues);

        // Flip texture
        final int halfHeight = height / 2;
        final int[] rowBuffer = new int[width];
        for (int y = 0; y < halfHeight; y++) {
            final int top = y * width;
            final int bottom = (height - 1 - y) * width;

            System.arraycopy(pixelValues, top, rowBuffer, 0, width);
            System.arraycopy(pixelValues, bottom, pixelValues, top, width);
            System.arraycopy(rowBuffer, 0, pixelValues, bottom, width);
        }

        File dir = output.getParentFile();
        dir.mkdirs();
        copyBufferedImageToFile(bufferedimage, output);
        bufferedimage.flush();
    }

    private static void copyBufferedImageToFile(BufferedImage image, File file) {
        try {
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
