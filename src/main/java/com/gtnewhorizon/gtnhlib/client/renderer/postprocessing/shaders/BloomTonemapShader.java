package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingHelper;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;

/**
 * A class that helps with converting HDR colors to non-HDR colors. This class is mainly used for bloom, and some
 * calculations may be off because of it.
 */
@Beta
public class BloomTonemapShader {

    // Magical constant, also used in the tonemap shader.
    private static final float TONEMAP_VALUE = 1.9f;

    private static ShaderProgram shader;
    private static int uMultiplier;
    private static float tonemapMultiplier;

    private static BloomTonemapShader instance;

    public BloomTonemapShader() {
        shader = new ShaderProgram(
                GTNHLib.RESOURCE_DOMAIN,
                "shaders/hdr/tonemap.vert.glsl",
                "shaders/hdr/tonemap.frag.glsl");
        shader.use();
        uMultiplier = shader.getUniformLocation("multiplier");
        shader.bindTextureSlot("uScene", 0);
        shader.bindTextureSlot("uOverlay", 1);
        ShaderProgram.clear();
    }

    public static BloomTonemapShader getInstance() {
        if (instance == null) {
            instance = new BloomTonemapShader();
        }
        return instance;
    }

    public void applyTonemapping(float multiplier, int overlayTexture) {
        shader.use();
        if (tonemapMultiplier != multiplier) {
            GL20.glUniform1f(uMultiplier, multiplier);
            tonemapMultiplier = multiplier;
        }
        Minecraft.getMinecraft().getFramebuffer().bindFramebufferTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, overlayTexture);
        GL11.glDisable(GL11.GL_BLEND);
        PostProcessingHelper.drawFullscreenQuad();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /**
     * Converts a HDR color to a non-HDR color.
     */
    public static float tonemap(float x) {
        final float a = 2.51f;
        final float b = 0.03f;
        final float c = 2.43f;
        final float d = 0.59f;
        final float e = 0.14f;
        return MathHelper.clamp_float((x * (a * x + b)) / (x * (c * x + d) + e), 0.0f, 1.0f);
    }
}
