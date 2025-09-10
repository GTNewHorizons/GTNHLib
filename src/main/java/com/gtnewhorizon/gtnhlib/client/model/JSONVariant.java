package com.gtnewhorizon.gtnhlib.client.model;

import static java.lang.Math.toRadians;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;

/**
 * Largely mirrors Minecraft's format for specifying models, except we use radians instead of degrees.
 *
 * @param model  The model file this variant refers to
 * @param x      X rotation, in radians
 * @param y      Y rotation, in radians
 * @param z      Z rotation, in radians
 * @param uvLock If true, textures aren't rotated with the block
 */
@Desugar
@Internal
public record JSONVariant(ResourceLoc.ModelLoc model, float x, float y, float z, boolean uvLock) implements BakeData {

    public JSONVariant(ResourceLoc.ModelLoc model, int x, int y, boolean uvLock) {
        this(model, x, y, 0, uvLock);
    }

    public JSONVariant(ResourceLoc.ModelLoc model, int x, int y, int z, boolean uvLock) {
        this(model, (float) toRadians(x), (float) toRadians(y), (float) toRadians(z), uvLock);
    }

    /// @param weight Used when selecting models. Irrelevant when only one variant is allowed, but must always be
    /// greater than zero.
    public static Weighted<JSONVariant> weightedVariant(ResourceLoc.ModelLoc model, int x, int y, int z, boolean uvLock,
                                                        int weight) {
        final var v = new JSONVariant(model, x, y, z, uvLock);
        return new Weighted<>(v, weight);
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
}
