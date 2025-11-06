package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders.PostProcessingRenderer;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class PostProcessingManager {

    private static PostProcessingManager instance;

    // This list stores all reuseable GeometryRenderer objects. The actual length of the list is renderersIndex.
    private final List<GeometryRenderer> delayedRenderers = new ArrayList<>();
    private int renderersIndex;
    private final List<PostProcessingRenderer> postProcessingRenderers = new ArrayList<>();

    // Ensures that the delayed geometry is rendered before the post-processing shaders.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void applyPostProcessingEffects(RenderWorldLastEvent event) {

        if (renderersIndex != 0) {
            for (int i = 0; i < renderersIndex; i++) {
                final GeometryRenderer renderer = delayedRenderers.get(i);

                GL11.glTranslatef(renderer.viewX, renderer.viewY, renderer.viewZ);
                renderer.renderer.render(renderer.data);
                GL11.glTranslatef(-renderer.viewX, -renderer.viewY, -renderer.viewZ);

                renderer.data = null; // Prevent potential memory leaks
            }
            renderersIndex = 0;
        }

        final int length = postProcessingRenderers.size();
        // noinspection ForLoopReplaceableByForEach
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

    // Reduce object allocations
    public void addDelayedRenderer(I3DGeometryRenderer renderer, double viewX, double viewY, double viewZ, Object data) {
        if (delayedRenderers.size() > renderersIndex) {
            delayedRenderers.get(renderersIndex).set(renderer, (float) viewX, (float) viewY, (float) viewZ, data);
        } else {
            delayedRenderers.add(new GeometryRenderer(renderer, (float) viewX, (float) viewY, (float) viewZ, data));
        }
        renderersIndex++;
    }

    //
    private static final class GeometryRenderer {

        private I3DGeometryRenderer renderer;
        private float viewX;
        private float viewY;
        private float viewZ;
        private Object data;

        public GeometryRenderer(I3DGeometryRenderer renderer, float viewX, float viewY, float viewZ, Object data) {
            set(renderer, viewX, viewY, viewZ, data);
        }

        public void set(I3DGeometryRenderer renderer, float viewX, float viewY, float viewZ, Object data) {
            this.renderer = renderer;
            this.viewX = viewX;
            this.viewY = viewY;
            this.viewZ = viewZ;
            this.data = data;
        }
    }
}
