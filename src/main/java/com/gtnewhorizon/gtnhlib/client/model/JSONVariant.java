package com.gtnewhorizon.gtnhlib.client.model;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;

/// Largely mirrors Minecraft's format for specifying models.
///
/// @param model  The model file this variant refers to
/// @param x      X rotation
/// @param y      Y rotation
/// @param z      Z rotation (not present in modern)
/// @param uvLock If true, textures aren't rotated with the block
@Desugar
@Internal
public record JSONVariant(ResourceLoc.ModelLoc model, float x, float y, float z, boolean uvLock) implements BakeData {

    /// @param weight Used when selecting models. Irrelevant when only one variant is allowed, but must always be
    /// greater than zero.
    public static Weighted<JSONVariant> weightedVariant(ResourceLoc.ModelLoc model, int x, int y, int z, boolean uvLock,
            int weight) {
        final var v = new JSONVariant(model, x, y, z, uvLock);
        return new Weighted<>(v, weight);
    }

    private static final float DEG2RAD = (float) (Math.PI / 180);

    @Override
    public Matrix4f getAffineMatrix() {
        return new Matrix4f()
            .translation(-.5f, -.5f, -.5f)
            .rotateLocalX(x * DEG2RAD)
            .rotateLocalY(y * DEG2RAD)
            .rotateLocalZ(z * DEG2RAD)
            .translateLocal(.5f, .5f, .5f);
    }

    @Override
    public boolean lockUV() {
        return uvLock;
    }
}
