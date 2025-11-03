package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;

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
     * This relies on bindFullscreenVAO being called prior to this method call. Else it won't do anything
     */
    public static void drawFullscreenQuad() {
        fullscreenQuadVAO.draw();
    }

    public static void unbindVAO() {
        fullscreenQuadVAO.unbind();
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
