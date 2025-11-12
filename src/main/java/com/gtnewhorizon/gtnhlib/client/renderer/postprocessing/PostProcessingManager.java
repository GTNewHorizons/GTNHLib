package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders.PostProcessingRenderer;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class PostProcessingManager {

    private static PostProcessingManager instance;

    // This list stores all reuseable GeometryRenderer objects. The actual length of the list is renderersIndex.
    private final List<GeometryRenderer> delayedRenderers = new ArrayList<>();
    private int renderersIndex;
    private final List<PostProcessingRenderer> postProcessingRenderers = new ArrayList<>();

    // Ensures that the delayed geometry is rendered before the post-processing shaders.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void applyPostProcessingEffects(RenderWorldLastEvent event) {

        final int cachedRenderers = delayedRenderers.size();
        if (cachedRenderers >= 16) {
            final int upperLimit = cachedRenderers / 4;
            if (upperLimit > renderersIndex) {
                delayedRenderers.subList(upperLimit, cachedRenderers).clear();
            }
        }

        if (renderersIndex != 0) {
            for (int i = 0; i < renderersIndex; i++) {
                final GeometryRenderer renderer = delayedRenderers.get(i);

                GL11.glTranslated(renderer.viewX, renderer.viewY, renderer.viewZ);
                renderer.renderer.render(renderer.data);
                GL11.glTranslated(-renderer.viewX, -renderer.viewY, -renderer.viewZ);

                renderer.data = null; // Prevent potential memory leaks
            }
            renderersIndex = 0;
        }

        final int length = postProcessingRenderers.size();
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < length; i++) {
            final PostProcessingRenderer renderer = postProcessingRenderers.get(i);
            if (renderer.needsRendering) {
                renderer.needsRendering = false;
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

    /**
     * Adds a delayed renderer. The given renderer will be called during {@link RenderWorldLastEvent}. <br>
     *
     * These need to be registered every render tick <strong>before</strong> the {@link RenderWorldLastEvent} gets
     * called.<br>
     *
     * "View space" refers to the space of the object in relation to the player's view. It is calculated as
     * (object.position - view.position) <br>
     *
     * @param renderer The implementation of the geometry renderer
     * @param viewX    The x-coordinate of the object in view space
     * @param viewY    The x-coordinate of the object in view space
     * @param viewZ    The x-coordinate of the object in view space
     * @param data     Any additional data that's necessary for the rendering of the object (ex: an Entity or
     *                 TileEntity)
     */
    public void addDelayedRenderer(I3DGeometryRenderer renderer, double viewX, double viewY, double viewZ,
            Object data) {
        if (delayedRenderers.size() > renderersIndex) {
            // Reduce object allocations
            delayedRenderers.get(renderersIndex).set(renderer, (float) viewX, (float) viewY, (float) viewZ, data);
        } else {
            delayedRenderers.add(new GeometryRenderer(renderer, (float) viewX, (float) viewY, (float) viewZ, data));
        }
        renderersIndex++;
    }

    /**
     * Helper methods to convert the coordinates of a block to view space.
     */
    public static double viewX(double x) {
        return x - RenderManager.renderPosX;
    }

    public static double viewY(double y) {
        return y - RenderManager.renderPosY;
    }

    public static double viewZ(double z) {
        return z - RenderManager.renderPosZ;
    }

    private static final class GeometryRenderer {

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
    }
}
