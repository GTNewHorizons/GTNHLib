package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingManager;

import static com.gtnewhorizon.gtnhlib.ClientProxy.mc;

public abstract class PostProcessingRenderer {

    protected boolean needsRendering;

    public PostProcessingRenderer() {
        PostProcessingManager.getInstance().registerPostProcessingRenderer(this);
    }

    public final void checkRender(float partialTicks) {
        if (needsRendering) {
            needsRendering = false;
            render(partialTicks);
        }
    }

    public final void setNeedsRendering() {
        this.needsRendering = true;
    }

    public static void unbind() {
        mc.getFramebuffer().bindFramebuffer(false);
    }

    protected abstract void render(float partialTicks);

    public abstract void bindFramebuffer();
}
