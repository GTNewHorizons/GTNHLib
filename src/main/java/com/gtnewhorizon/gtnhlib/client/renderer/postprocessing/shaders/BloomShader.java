package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders;

import static com.gtnewhorizon.gtnhlib.ClientProxy.mc;
import static com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer.HDR_COLORS;
import static com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer.NO_ALPHA_CHANNEL;
import static com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer.TEXTURE_LINEAR;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingHelper;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.SharedDepthFramebuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;

public class BloomShader extends PostProcessingRenderer {

    private static BloomShader instance;

    private CustomFramebuffer[] framebuffers;

    private final ShaderProgram downscaleProgram;
    private final int uTexelSize_downscale;

    private final ShaderProgram upscaleProgram;
    private final int uTexelSize_upscale;

    private float multiplier;

    public BloomShader() {
        super();
        downscaleProgram = new ShaderProgram(
                GTNHLib.RESOURCE_DOMAIN,
                "shaders/bloom/downscale.vert.glsl",
                "shaders/bloom/downscale.frag.glsl");
        uTexelSize_downscale = downscaleProgram.getUniformLocation("texelSize");

        upscaleProgram = new ShaderProgram(
                GTNHLib.RESOURCE_DOMAIN,
                "shaders/bloom/upscale.vert.glsl",
                "shaders/bloom/upscale.frag.glsl");
        uTexelSize_upscale = upscaleProgram.getUniformLocation("texelSize");

        createFramebuffers();
    }

    private void createFramebuffers() {
        float width = mc.displayWidth;
        float height = mc.displayHeight;
        List<CustomFramebuffer> framebufferList = new ArrayList<>();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();

        int screenWidth = dm.getWidth();
        int screenHeight = dm.getHeight();

        multiplier = 0.5f;
        if (width < screenWidth || height < screenHeight) {
            float widthMultiplier = width / screenWidth;
            float heightMultiplier = height / screenHeight;

            float avg = (float) Math.sqrt((widthMultiplier + heightMultiplier) / 2);
            multiplier *= avg;
        }

        while (framebufferList.size() < 8 && width + height > 5) {
            final CustomFramebuffer framebuffer;
            if (framebufferList.isEmpty()) {
                framebuffer = new SharedDepthFramebuffer(
                        Math.round(width),
                        Math.round(height),
                        TEXTURE_LINEAR | HDR_COLORS | NO_ALPHA_CHANNEL);
            } else {
                framebuffer = new CustomFramebuffer(
                        Math.round(width),
                        Math.round(height),
                        TEXTURE_LINEAR | HDR_COLORS | NO_ALPHA_CHANNEL);
            }
            framebufferList.add(framebuffer);

            width /= 2;
            height /= 2;
        }
        framebuffers = framebufferList.toArray(new CustomFramebuffer[0]);
    }

    public static BloomShader getInstance() {
        if (instance == null) {
            instance = new BloomShader();
        }
        return instance;
    }

    @Override
    public void bindFramebuffer() {
        CustomFramebuffer mainFramebuffer = framebuffers[0];
        if (mc.displayWidth != mainFramebuffer.framebufferWidth
                || mc.displayHeight != mainFramebuffer.framebufferHeight) {
            for (CustomFramebuffer framebuffer : framebuffers) {
                framebuffer.checkDeleteFramebuffer();
            }
            createFramebuffers();
            mainFramebuffer = framebuffers[0];
        }

        needsRendering = true;
        mainFramebuffer.bindFramebuffer();
    }

    @Override
    public void render(float partialTicks) {
        PostProcessingHelper.bindFullscreenVAO();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        final CustomFramebuffer mainFramebuffer = framebuffers[0];

        mainFramebuffer.bindFramebuffer();
        mainFramebuffer.bindFramebufferTexture();

        downscaleProgram.use();

        for (int i = 1; i < framebuffers.length; i++) {
            CustomFramebuffer framebuffer = framebuffers[i];
            framebuffer.clearBindFramebuffer(true);
            GL20.glUniform2f(
                    uTexelSize_downscale,
                    1f / framebuffer.framebufferWidth,
                    1f / framebuffer.framebufferHeight);

            PostProcessingHelper.drawFullscreenQuad();

            framebuffer.bindFramebufferTexture();
        }

        upscaleProgram.use();

        for (int i = framebuffers.length - 1; i >= 1; i--) {
            CustomFramebuffer framebuffer = framebuffers[i];
            CustomFramebuffer upscaledFramebuffer = framebuffers[i - 1];
            framebuffer.bindFramebufferTexture();
            upscaledFramebuffer.bindFramebuffer(true);
            GL20.glUniform2f(uTexelSize_upscale, 1f / framebuffer.framebufferWidth, 1f / framebuffer.framebufferHeight);

            PostProcessingHelper.drawFullscreenQuad();
        }

        mc.getFramebuffer().bindFramebuffer(false);
        mainFramebuffer.applyTonemapping(this.multiplier);
        mainFramebuffer.clearBindFramebuffer();
        mc.getFramebuffer().bindFramebuffer(false);

        ShaderProgram.clear();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        PostProcessingHelper.unbindVAO();
    }
}
