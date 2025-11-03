package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;

public class CustomFramebuffer {

    public int framebufferWidth;
    public int framebufferHeight;
    public int framebufferObject;
    public int framebufferTexture;
    public int depthAttachment;

    private final int settings;
    private final int bufferBits;

    public static final int DEPTH_ENABLED = 0x1;
    public static final int DEPTH_TEXTURE = 0x2 | DEPTH_ENABLED;
    public static final int STENCIL_BUFFER = 0x4 | DEPTH_ENABLED;
    public static final int TEXTURE_LINEAR = 0x8;
    public static final int NO_ALPHA_CHANNEL = 0x10;
    public static final int HDR_COLORS = 0x20;
    public static final int SHARED_DEPTH_ATTACHMENT = 0x40;

    // field_153198_e = GL30.GL_FRAMEBUFFER
    // field_153200_g = GL30.GL_COLOR_ATTACHMENT0
    // field_153199_f = GL30.GL_RENDERBUFFER
    // field_153201_h = GL30.GL_DEPTH_ATTACHMENT

    public CustomFramebuffer(int settings) {
        this.settings = settings;
        this.bufferBits = createBufferBits();
    }

    public CustomFramebuffer(int width, int height) {
        this(width, height, 0);
    }

    public CustomFramebuffer(int width, int height, int settings) {
        this(settings);
        createFramebuffer(width, height);
        unbindFramebuffer();
    }

    protected int createBufferBits() {
        int bits = GL11.GL_COLOR_BUFFER_BIT;
        if (isEnabled(DEPTH_ENABLED)) {
            bits |= GL11.GL_DEPTH_BUFFER_BIT;
        }
        if (isEnabled(STENCIL_BUFFER)) {
            bits |= GL11.GL_STENCIL_BUFFER_BIT;
        }
        return bits;
    }

    private int getTextureFormat() {
        if (isEnabled(HDR_COLORS)) {
            if (isEnabled(NO_ALPHA_CHANNEL)) {
                return GL30.GL_RGB16F;
            } else {
                return GL11.GL_RGBA16;
            }
        } else {
            if (isEnabled(NO_ALPHA_CHANNEL)) {
                return GL11.GL_RGB8;
            } else {
                return GL11.GL_RGBA8;
            }
        }
    }

    private int getTextureFilter() {
        return isEnabled(TEXTURE_LINEAR) ? GL11.GL_LINEAR : GL11.GL_NEAREST;
    }

    private boolean isEnabled(int bit) {
        return (settings & bit) != 0;
    }

    public void createBindFramebuffer(int width, int height) {
        this.createFramebuffer(width, height);
    }

    protected void createFramebuffer(int width, int height) {
        checkDeleteFramebuffer();

        this.framebufferWidth = width;
        this.framebufferHeight = height;

        this.framebufferObject = OpenGlHelper.func_153165_e();

        bindFramebuffer();

        this.framebufferTexture = createFramebufferAttachment();

        if (isEnabled(DEPTH_ENABLED)) {
            final boolean stencil = isEnabled(STENCIL_BUFFER);
            if (isEnabled(DEPTH_TEXTURE)) {
                final int filter = getTextureFilter();
                this.depthAttachment = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.depthAttachment);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
                if (stencil) {
                    GL11.glTexImage2D(
                            GL11.GL_TEXTURE_2D,
                            0,
                            GL30.GL_DEPTH24_STENCIL8,
                            width,
                            height,
                            0,
                            GL30.GL_DEPTH_STENCIL,
                            GL30.GL_UNSIGNED_INT_24_8,
                            (IntBuffer) null);
                } else {
                    GL11.glTexImage2D(
                            GL11.GL_TEXTURE_2D,
                            0,
                            GL11.GL_DEPTH_COMPONENT,
                            width,
                            height,
                            0,
                            GL11.GL_DEPTH_COMPONENT,
                            GL11.GL_FLOAT,
                            (IntBuffer) null);
                }
                OpenGlHelper.func_153188_a(
                        GL30.GL_FRAMEBUFFER,
                        GL30.GL_DEPTH_ATTACHMENT,
                        GL11.GL_TEXTURE_2D,
                        this.depthAttachment,
                        0);
                if (stencil) {
                    OpenGlHelper.func_153188_a(
                            GL30.GL_FRAMEBUFFER,
                            GL30.GL_STENCIL_ATTACHMENT,
                            GL11.GL_TEXTURE_2D,
                            this.depthAttachment,
                            0);
                }
            } else {
                this.depthAttachment = OpenGlHelper.func_153185_f();
                OpenGlHelper.func_153176_h(GL30.GL_RENDERBUFFER, this.depthAttachment);
                if (stencil) {
                    OpenGlHelper.func_153186_a(
                            GL30.GL_RENDERBUFFER,
                            EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
                            framebufferWidth,
                            framebufferHeight);
                    OpenGlHelper.func_153190_b(
                            GL30.GL_FRAMEBUFFER,
                            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                            GL30.GL_RENDERBUFFER,
                            this.depthAttachment);
                    OpenGlHelper.func_153190_b(
                            GL30.GL_FRAMEBUFFER,
                            EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
                            GL30.GL_RENDERBUFFER,
                            this.depthAttachment);
                } else {
                    OpenGlHelper.func_153186_a(
                            GL30.GL_FRAMEBUFFER,
                            GL14.GL_DEPTH_COMPONENT24,
                            framebufferWidth,
                            framebufferHeight);
                    OpenGlHelper.func_153190_b(
                            GL30.GL_FRAMEBUFFER,
                            GL30.GL_DEPTH_ATTACHMENT,
                            GL30.GL_RENDERBUFFER,
                            this.depthAttachment);
                }
            }
        }

        clearCurrentFramebuffer();
    }

    public int createFramebufferAttachment() {
        return createFramebufferAttachment(0);
    }

    public int createFramebufferAttachment(int slot) {
        int texture = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                getTextureFormat(),
                this.framebufferWidth,
                this.framebufferHeight,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        final int filter = getTextureFilter();
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        // glFramebufferTexture2D
        OpenGlHelper
                .func_153188_a(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 | slot, GL11.GL_TEXTURE_2D, texture, 0);
        return texture;
    }

    public final void checkDeleteFramebuffer() {
        if (this.framebufferObject > 0) {
            this.deleteFramebuffer();
        }
    }

    protected void deleteFramebuffer() {
        this.unbindFramebufferTexture();
        this.unbindFramebuffer();

        if (this.depthAttachment > -1) {
            if (isEnabled(DEPTH_TEXTURE)) {
                GL11.glDeleteTextures(this.depthAttachment);
            } else {
                OpenGlHelper.func_153184_g(this.depthAttachment);
            }
            this.depthAttachment = -1;
        }

        if (this.framebufferTexture > -1) {
            GL11.glDeleteTextures(this.framebufferTexture);
            this.framebufferTexture = -1;
        }

        if (this.framebufferObject > -1) {
            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, 0);
            OpenGlHelper.func_153174_h(this.framebufferObject);
            this.framebufferObject = -1;
        }
    }

    public void framebufferClear() {
        this.bindFramebuffer();
        clearCurrentFramebuffer();
        this.unbindFramebuffer();
    }

    public void clearCurrentFramebuffer() {
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClearDepth(1.0D);
        GL11.glClear(bufferBits);
    }

    public void copyDepthFromFramebuffer(Framebuffer other) {
        OpenGlHelper.func_153171_g(GL30.GL_READ_FRAMEBUFFER, other.framebufferObject);
        OpenGlHelper.func_153171_g(GL30.GL_DRAW_FRAMEBUFFER, framebufferObject);
        GL30.glBlitFramebuffer(
                0,
                0,
                other.framebufferWidth,
                other.framebufferHeight,
                0,
                0,
                framebufferWidth,
                framebufferHeight,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST);

    }

    public void bindFramebuffer(boolean viewport) {
        bindFramebuffer();

        if (viewport) {
            setupViewport();
        }
    }

    public void bindFramebuffer() {
        OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, this.framebufferObject);
    }

    public void clearBindFramebuffer() {
        bindFramebuffer();
        clearCurrentFramebuffer();
    }

    public void clearBindFramebuffer(boolean viewport) {
        bindFramebuffer(viewport);
        clearCurrentFramebuffer();
    }

    public void setupViewport() {
        GL11.glViewport(0, 0, framebufferWidth, framebufferHeight);
    }

    public void setupOrtho() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, framebufferWidth, framebufferHeight, 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
    }

    public void restoreOrtho() {
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public void bindFramebufferTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.framebufferTexture);
    }

    public void unbindFramebufferTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void unbindFramebuffer() {
        unbindFramebuffer(false);
    }

    public void unbindFramebuffer(boolean restoreViewport) {
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(restoreViewport);
    }

    /**
     * Note: glReadPixels is a very slow and stalling process.
     */
    public void readTexturePixels(IntBuffer pixelBuffer, int[] pixels) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferObject);
        pixelBuffer.clear();
        GL11.glReadPixels(
                0,
                0,
                framebufferWidth,
                framebufferHeight,
                GL12.GL_BGRA,
                GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                pixelBuffer);
        pixelBuffer.get(pixels);
    }

    public void readPixels(IntBuffer pixelBuffer, int[] pixels, int component) {
        pixelBuffer.clear();
        GL11.glReadPixels(0, 0, framebufferWidth, framebufferHeight, component, GL11.GL_FLOAT, pixelBuffer);
        pixelBuffer.get(pixels);
    }

    // TONEMAP SHADER

    private static ShaderProgram tonemapShader;
    private static int uMultiplier;
    private static float tonemapMultiplier;

    public void applyTonemapping(float multiplier) {
        if (tonemapShader == null) {
            tonemapShader = new ShaderProgram(
                    GTNHLib.RESOURCE_DOMAIN,
                    "shaders/hdr/tonemap.vert.glsl",
                    "shaders/hdr/tonemap.frag.glsl");
            tonemapShader.use();
            uMultiplier = tonemapShader.getUniformLocation("multiplier");
            tonemapShader.bindTextureSlot("uScene", 0);
            tonemapShader.bindTextureSlot("uOverlay", 1);
            ShaderProgram.clear();
        }

        tonemapShader.use();
        if (tonemapMultiplier != multiplier) {
            GL20.glUniform1f(uMultiplier, multiplier);
            tonemapMultiplier = multiplier;
        }
        Minecraft.getMinecraft().getFramebuffer().bindFramebufferTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        bindFramebufferTexture();
        GL11.glDisable(GL11.GL_BLEND);
        PostProcessingHelper.drawFullscreenQuad();
        // this.copyTextureToFile("bloomshader", "framebuffer_final.png");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    // DEBUG TOOLS

    public void checkFramebufferComplete() {
        int i = OpenGlHelper.func_153167_i(OpenGlHelper.field_153198_e);

        if (i != OpenGlHelper.field_153202_i) {
            if (i == OpenGlHelper.field_153203_j) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == OpenGlHelper.field_153204_k) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == OpenGlHelper.field_153205_l) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == OpenGlHelper.field_153206_m) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }

    public void copyTextureToFile(String category, String filename) {
        File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "debug"), category);
        dir.mkdirs();
        copyTextureToFile(new File(dir, filename));
    }

    public void copyTextureToFile(String category, String filename, int texture) {
        File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "debug"), category);
        dir.mkdirs();
        copyTextureToFile(new File(dir, filename), texture);
    }

    public void copyTextureToFile(String category, String filename, int[] pixels) {
        File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "debug"), category);
        dir.mkdirs();
        copyTextureToFile(new File(dir, filename), pixels);
    }

    public void copyDepthToFile(String category, String filename) {
        File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "debug"), category);
        dir.mkdirs();
        copyDepthToFile(new File(dir, filename));
    }

    public void copyStencilToFile(String category, String filename) {
        File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "debug"), category);
        dir.mkdirs();
        copyStencilToFile(new File(dir, filename));
    }

    public void copyTextureToFile(File file) {
        copyTextureToFile(file, framebufferTexture);
    }

    public void copyTextureToFile(File file, int texture) {
        final int width = this.framebufferWidth;
        final int height = this.framebufferHeight;
        BufferedImage bufferedimage = new BufferedImage(width, height, 1);
        final int[] pixelValues = ((DataBufferInt) bufferedimage.getRaster().getDataBuffer()).getData();
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(pixelValues.length);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
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

        copyBufferedImageToFile(bufferedimage, file);
        bufferedimage.flush();
    }

    public void copyTextureToFile(File file, int[] pixelValues) {
        final int width = this.framebufferWidth;
        final int height = this.framebufferHeight;
        BufferedImage bufferedimage = new BufferedImage(width, height, 1);

        // Flip textures
        final int halfHeight = height / 2;
        final int[] rowBuffer = new int[width];
        for (int y = 0; y < halfHeight; y++) {
            final int top = y * width;
            final int bottom = (height - 1 - y) * width;

            System.arraycopy(pixelValues, top, rowBuffer, 0, width);
            System.arraycopy(pixelValues, bottom, pixelValues, top, width);
            System.arraycopy(rowBuffer, 0, pixelValues, bottom, width);
        }
        bufferedimage.setRGB(0, 0, width, height, pixelValues, 0, width);

        copyBufferedImageToFile(bufferedimage, file);
        bufferedimage.flush();
    }

    // TODO unify and faster flipping
    public void copyDepthToFile(File file) {
        copyComponentToFile(GL11.GL_DEPTH_COMPONENT, file);
    }

    public void copyStencilToFile(File file) {

        final int width = this.framebufferWidth;
        final int height = this.framebufferHeight;

        int[] pixelValues = new int[width * height];
        IntBuffer stencilBuffer = BufferUtils.createIntBuffer(pixelValues.length);

        this.bindFramebuffer();
        GL11.glReadPixels(0, 0, width, height, GL11.GL_STENCIL_INDEX, GL11.GL_FLOAT, stencilBuffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = stencilBuffer.get(y * width + x);
                gray = Math.max(0, Math.min(255, gray));

                int rgb = (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, height - 1 - y, rgb);
            }
        }
        copyBufferedImageToFile(image, file);
        image.flush();
    }

    public void copyComponentToFile(int component, File file) {

        final int width = this.framebufferWidth;
        final int height = this.framebufferHeight;

        int[] pixelValues = new int[width * height];
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(pixelValues.length);

        this.bindFramebuffer();
        GL11.glReadPixels(0, 0, width, height, component, GL11.GL_FLOAT, depthBuffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float depth = depthBuffer.get(y * width + x);

                int gray = (int) ((1.0f - depth) * 255.0f);
                gray = Math.max(0, Math.min(255, gray));

                int rgb = (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, height - 1 - y, rgb);
            }
        }
        copyBufferedImageToFile(image, file);
        image.flush();
    }

    private static void copyBufferedImageToFile(BufferedImage image, File file) {
        try {
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
