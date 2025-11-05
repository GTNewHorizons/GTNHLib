package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

/**
 * Defines a renderer responsible for drawing 3D geometry associated with a given object type.
 * <p>
 * This interface is typically used for post-processing effects (where the geometry is being rendered onto a secondary
 * framebuffer), as they require the depth buffer to already be fully populated before the geometry is being rendered
 * (or else, the post-processed result may not have the correct depth values)
 * </p>
 *
 */
public interface I3DGeometryRenderer {

    /**
     * Renders the geometry of the given renderer. <br>
     * "View space" refers to the space of the object in relation to the player's view. It is calculated as
     * (object.position - view.position) <br>
     *
     * @param x      The x-coordinate in view space
     * @param y      The y-coordinate in view space
     * @param z      The z-coordinate in view space
     * @param data The object that stores any additional data needed to render properly. (ex: an Entity or TileEntity)
     */
    void render(double x, double y, double z, Object data);
}
