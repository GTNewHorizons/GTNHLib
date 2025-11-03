package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.compat.Mods;

public class PostProcessingHelper {

    private static VertexBuffer fullscreenQuadVAO;

    /**
     * Ideally, you'd only call bindFullscreenVAO/unbind only once per post-processing pass. <br>
     * If you ever want to render any other geometry inside of the post-processing pass for whatever reason, you'll have
     * to unbind() before rendering the other geometry.
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

    public static int getDepthTexture() {
        return getDepthTexture(Minecraft.getMinecraft().getFramebuffer());
    }

    public static int getDepthTexture(Framebuffer framebuffer) {
        if (!Mods.ANGELICA)
            throw new UnsupportedOperationException("The depth texture requires Angelica to be loaded.");
        return ((IRenderTargetExt) framebuffer).iris$getDepthTextureId();
    }

    private static VertexBuffer genFullscreenQuadVAO() {
        final CapturingTessellator tessellator = TessellatorManager.startCapturingAndGet();
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-1, -1, 0, 0, 0);
        tessellator.addVertexWithUV(1, -1, 0, 1, 0);
        tessellator.addVertexWithUV(1, 1, 0, 1, 1);
        tessellator.addVertexWithUV(-1, 1, 0, 0, 1);
        tessellator.draw();

        return TessellatorManager.stopCapturingToVAO(DefaultVertexFormat.POSITION_TEXTURE);
    }
}
