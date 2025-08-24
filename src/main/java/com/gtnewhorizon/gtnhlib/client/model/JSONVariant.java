package com.gtnewhorizon.gtnhlib.client.model;

import static java.lang.Math.toRadians;

import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;

/**
 * Largely mirrors Minecraft's format for specifying models, except we use radians instead of degrees.
 * @param model The model file this variant refers to
 * @param x X rotation, in radians
 * @param y Y rotation, in radians
 * @param z Z rotation, in radians
 * @param uvLock If true, textures aren't rotated with the block
 * @param weight Used when selecting models. Irrelevant when only one variant is allowed, but must always be greater
 *               than zero.
 */
@Internal
public record JSONVariant(ResourceLocation model, float x, float y, float z, boolean uvLock, int weight)
    implements BakeData {

    public JSONVariant(ResourceLocation model, int x, int y, boolean uvLock) {
        this(model, x, y, 0, uvLock, 1);
    }

    public JSONVariant(ResourceLocation model, int x, int y, int z, boolean uvLock) {
        this(model, x, y, z, uvLock, 1);
    }

    public JSONVariant(ResourceLocation model, int x, int y, int z, boolean uvLock, int weight) {
        this(model, (float) toRadians(x), (float) toRadians(y), (float) toRadians(z), uvLock, weight);
    }

    @Override
    public Matrix4f getAffineMatrix() {

        return new Matrix4f().translation(-.5f, -.5f, -.5f).rotateLocalX(x).rotateLocalY(y).rotateLocalZ(z)
                .translateLocal(.5f, .5f, .5f);
    }

    @Override
    public boolean lockUV() {
        return uvLock;
    }

    /**
     * Convenience wrapper to combine a weight with a model. Does not uniquely identify a model, unlike JSONVariant
     * itself.
     */
    public record Weighted(JSONVariant v, int weight) {}
}
