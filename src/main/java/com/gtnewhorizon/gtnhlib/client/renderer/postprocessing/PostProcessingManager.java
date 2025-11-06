package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders.PostProcessingRenderer;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class PostProcessingManager {

    private static PostProcessingManager instance;

    //
    private final List<GeometryRenderer> delayedRenderers = new ArrayList<>();
    private int renderersSize;
    private final List<PostProcessingRenderer> postProcessingRenderers = new ArrayList<>();

    // Ensures that the delayed geometry is rendered before the post-processing shaders.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void applyPostProcessingEffects(RenderWorldLastEvent event) {
        if (renderersSize != 0) {
            for (int i = 0; i < renderersSize; i++) {
                delayedRenderers.get(i).render();
            }
            renderersSize = 0;
        }

        final int length = postProcessingRenderers.size();
        for (int i = 0; i < length; i++) {
            final PostProcessingRenderer renderer = postProcessingRenderers.get(i);
            if (renderer.needsRendering) {
                renderer.render(event.partialTicks);
            }
        }
    }

    public static PostProcessingManager getInstance() {
        if (instance == null) {
            instance = new PostProcessingManager();
            MinecraftForge.EVENT_BUS.register(instance);
        }
        return instance;
    }

    public void registerPostProcessingRenderer(PostProcessingRenderer shader) {
        postProcessingRenderers.add(shader);
    }

    public void addDelayedRenderer(I3DGeometryRenderer renderer, double viewX, double viewY, double viewZ) {
        addDelayedRenderer(renderer, viewX, viewY, viewZ, null);
    }

    public void addDelayedRenderer(I3DGeometryRenderer renderer, double viewX, double viewY, double viewZ,
            Object data) {
        if (delayedRenderers.size() > renderersSize) {
            delayedRenderers.get(renderersSize).set(renderer, viewX, viewY, viewZ, data);
        } else {
            delayedRenderers.add(new GeometryRenderer(renderer, viewX, viewY, viewZ, data));
        }
        renderersSize++;
    }

    private static class GeometryRenderer {

        private I3DGeometryRenderer renderer;
        private double viewX;
        private double viewY;
        private double viewZ;
        private Object data;

        public GeometryRenderer(I3DGeometryRenderer renderer, double viewX, double viewY, double viewZ, Object data) {
            set(renderer, viewX, viewY, viewZ, data);
        }

        public void set(I3DGeometryRenderer renderer, double viewX, double viewY, double viewZ, Object data) {
            this.renderer = renderer;
            this.viewX = viewX;
            this.viewY = viewY;
            this.viewZ = viewZ;
            this.data = data;
        }

        public void render() {
            renderer.render(viewX, viewY, viewZ, data);
        }
    }
}
