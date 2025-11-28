package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingHelper;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;

public class TonemapShader {

    private static ShaderProgram shader;
    private static int uMultiplier;
    private static float tonemapMultiplier;

    private static TonemapShader instance;

    public TonemapShader() {
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

    public static TonemapShader getInstance() {
        if (instance == null) {
            instance = new TonemapShader();
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
        // this.copyTextureToFile("bloomshader", "framebuffer_final.png");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public static float aces(float x) {
        final float a = 2.51f;
        final float b = 0.03f;
        final float c = 2.43f;
        final float d = 0.59f;
        final float e = 0.14f;
        return MathHelper.clamp_float((x * (a * x + b)) / (x * (c * x + d) + e), 0.0f, 1.0f);
    }

    public static float inverseAces(float y) {
        final float a = 2.51f;
        final float b = 0.03f;
        final float c = 2.43f;
        final float d = 0.59f;
        final float e = 0.14f;

        float numerator = -(b - y * d);
        float discriminant = (b - y * d) * (b - y * d) - 4.0f * (a - y * c) * (-y * e);
        float sqrtDisc = (float) Math.sqrt(Math.max(discriminant, 0));
        float denom = 2.0f * (a - y * c);

        return (numerator + sqrtDisc) / denom;
    }

    public static float gammaCorrect(float color) {
        return (float) Math.pow(color, 1.9f);
    }

    public static float inverseGammaCorrect(float color) {
        return (float) Math.pow(color, 1 / 1.9f);
    }
}
