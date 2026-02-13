package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.compat.Mods;

public class PostProcessingHelper {

    private static IVertexArrayObject fullscreenQuadVAO;

    /**
     * Ideally, you'd only call bindFullscreenVAO/unbind only once per post-processing pass. <br>
     * If you ever want to render any other geometry inside of the post-processing pass for whatever reason, you'll have
     * to call unbindVAO() before rendering the other geometry.
     */
    public static void bindFullscreenVAO() {
        if (fullscreenQuadVAO == null) {
            fullscreenQuadVAO = genFullscreenQuadVAO();
        }
        fullscreenQuadVAO.bind();
    }

    /**
     * This relies on bindFullscreenVAO being called prior to this method call. Else it won't do anything. <br>
     * Keep in mind that the vertices are a fullscreen in opengl space (-1..1), meaning your shader shouldn't multiply
     * the vertex position by the ModelViewProjection matrix. This also means that you don't need to mess with the
     * translations in order to render the quad.
     */
    public static void drawFullscreenQuad() {
        fullscreenQuadVAO.draw();
    }

    public static void unbindVAO() {
        fullscreenQuadVAO.unbind();
    }

    public static void setupPostProcessingGL() {
        bindFullscreenVAO();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    public static void clearPostProcessingGL() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        unbindVAO();
    }

    public static int getDepthTexture() {
        return getDepthTexture(Minecraft.getMinecraft().getFramebuffer());
    }

    // TODO Sisyphus: move IRenderTargetExt to GTNHLib in a non-invasive way
    public static int getDepthTexture(Framebuffer framebuffer) {
        if (!Mods.ANGELICA)
            throw new UnsupportedOperationException("The depth texture requires Angelica to be loaded.");
        return ((IRenderTargetExt) framebuffer).iris$getDepthTextureId();
    }

    private static IVertexArrayObject genFullscreenQuadVAO() {
        final DirectTessellator tessellator = TessellatorManager.startCapturingDirect();
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-1, -1, 0, 0, 0);
        tessellator.addVertexWithUV(1, -1, 0, 1, 0);
        tessellator.addVertexWithUV(1, 1, 0, 1, 1);
        tessellator.addVertexWithUV(-1, 1, 0, 0, 1);
        tessellator.draw();

        return TessellatorManager.stopCapturingDirectToVBO(VertexBufferType.IMMUTABLE);
    }
}
