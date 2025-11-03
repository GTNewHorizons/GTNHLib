package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CustomFramebuffer {
    public int framebufferWidth;
    public int framebufferHeight;
    public int framebufferObject;
    public int framebufferTexture;
    public int depthBuffer;

    private final int settings;

    public static final int FRAMEBUFFER_DEPTH_ENABLED = 0x1;
    public static final int FRAMEBUFFER_DEPTH_RENDERBUFFER = 0x1;
    public static final int FRAMEBUFFER_DEPTH_TEXTURE = 0x2 | 0x1;
    public static final int FRAMEBUFFER_STENCIL_BUFFER = 0x4;
    public static final int FRAMEBUFFER_TEXTURE_LINEAR = 0x8;
    public static final int FRAMEBUFFER_ALPHA_CHANNEL = 0x10;
    public static final int FRAMEBUFFER_HDR_COLORS = 0x20;

    public CustomFramebuffer() {
        this(0);
    }

    public CustomFramebuffer(int settings) {
        this.settings = settings;
    }

    public CustomFramebuffer(int width, int height) {
        this(width, height, 0);
    }

    public CustomFramebuffer(int width, int height, int settings) {
        createFramebuffer(width, height);
        unbindFramebuffer();
        this.settings = settings;
    }


    public void createBindFramebuffer(int width, int height) {
        this.createFramebuffer(width, height);
        OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, 0);
    }

    protected void createFramebuffer(int width, int height) {
        checkDeleteFramebuffer();

        this.framebufferWidth = width;
        this.framebufferHeight = height;

        this.framebufferObject = OpenGlHelper.func_153165_e();


        this.depthBuffer = OpenGlHelper.func_153185_f();

        bindFramebuffer();

        this.framebufferTexture = createFramebufferAttachment();



        OpenGlHelper.func_153176_h(OpenGlHelper.field_153199_f, this.depthBuffer);
        OpenGlHelper.func_153186_a(
            OpenGlHelper.field_153199_f,
            EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
            this.framebufferWidth, this.framebufferHeight
        );
        OpenGlHelper.func_153190_b(
            GL30.GL_FRAMEBUFFER,
            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
            OpenGlHelper.field_153199_f,
            this.depthBuffer
        );
        OpenGlHelper.func_153190_b(
            GL30.GL_FRAMEBUFFER,
            EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
            OpenGlHelper.field_153199_f,
            this.depthBuffer
        );

        clearCurrentFramebuffer();
        this.unbindFramebufferTexture();
    }

    public int createFramebufferAttachment() {
        return createFramebufferAttachment(0);
    }

    public int createFramebufferAttachment(int slot) {
        int texture = GL11.glGenTextures();
        // GL30.GL_RGB32F,

        // TODO add params for custom format + interpolation
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            this.framebufferWidth, this.framebufferHeight,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null
        );
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        // field_153198_e = GL30.GL_FRAMEBUFFER
        // field_153200_g = GL30.GL_COLOR_ATTACHMENT0

        // glFramebufferTexture2D
        OpenGlHelper.func_153188_a(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0 | slot,
            GL11.GL_TEXTURE_2D,
            texture,
            0
        );
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

        if (this.depthBuffer > -1) {
            OpenGlHelper.func_153184_g(this.depthBuffer);
            this.depthBuffer = -1;
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
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
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
        GL11.glViewport(
            0,
            0,
            framebufferWidth,
            framebufferHeight
        );
    }

    public void setupOrtho() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(
            0.0D, framebufferWidth,
            framebufferHeight, 0.0D,
            1000.0D,
            3000.0D
        );
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
            0, 0,
            framebufferWidth, framebufferHeight,
            GL12.GL_BGRA,
            GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
            pixelBuffer
        );
        pixelBuffer.get(pixels);
    }

    public void readPixels(IntBuffer pixelBuffer, int[] pixels, int component) {
        pixelBuffer.clear();
        GL11.glReadPixels(
            0, 0, framebufferWidth, framebufferHeight,
            component,
            GL11.GL_FLOAT,
            pixelBuffer
        );
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
        Minecraft.getMinecraft()
            .getFramebuffer()
            .bindFramebufferTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        bindFramebufferTexture();
        GL11.glDisable(GL11.GL_BLEND);
        PostProcessingHelper.drawFullscreenQuad();
        // this.copyTextureToFile("bloomshader", "framebuffer_final.png");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }



    // DEBUG TOOLS
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
        GL11.glGetTexImage(
            GL11.GL_TEXTURE_2D,
            0,
            GL12.GL_BGRA,
            GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
            pixelBuffer
        );
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


    //TODO unify and faster flipping
    public void copyDepthToFile(File file) {
        copyComponentToFile(GL11.GL_DEPTH_COMPONENT, file);
    }

    public void copyStencilToFile(File file) {

        final int width = this.framebufferWidth;
        final int height = this.framebufferHeight;

        int[] pixelValues = new int[width * height];
        IntBuffer stencilBuffer = BufferUtils.createIntBuffer(pixelValues.length);

        this.bindFramebuffer();
        GL11.glReadPixels(
            0, 0, width, height,
            GL11.GL_STENCIL_INDEX,
            GL11.GL_FLOAT,
            stencilBuffer
        );

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
        GL11.glReadPixels(
            0, 0, width, height,
            component,
            GL11.GL_FLOAT,
            depthBuffer
        );

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
