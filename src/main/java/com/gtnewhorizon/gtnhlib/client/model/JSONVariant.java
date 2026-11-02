package com.gtnewhorizon.gtnhlib.client.model;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;

/// Mirrors Minecraft's format for specifying models.
@Internal
public final class JSONVariant implements BakeData {

    /// @param weight Used when selecting models. Irrelevant when only one variant is allowed, but must always be
    /// greater than zero.
    public static Weighted<JSONVariant> weightedVariant(ResourceLoc.ModelLoc model, int x, int y, boolean uvLock,
            int weight) {
        final var v = new JSONVariant(model, x, y, uvLock);
        return new Weighted<>(v, weight);
    }

    public static final float DEG2RAD = (float) (Math.PI / 180);
    private final ResourceLoc.ModelLoc model;
    private final int x;
    private final int y;
    private final boolean uvLock;

    /// @param model The model file this variant refers to
    /// @param x X rotation
    /// @param y Y rotation
    /// @param uvLock If true, textures aren't rotated with the block
    public JSONVariant(ResourceLoc.ModelLoc model, int x, int y, boolean uvLock) {
        this.model = model;
        this.x = x;
        this.y = y;
        this.uvLock = uvLock;
    }

    // Modern doesn't use block centered rotation for the matrices.
    // You might ask why do we take the - sign? Modern MC is describing how the model is rotated
    // around its own coordinate/rendering convention rather than mathematical consistency is all I can tell.
    @Override
    public Matrix4fc getAffineMatrix() {
        return new Matrix4f().rotateLocalX(-x * DEG2RAD).rotateLocalY(-y * DEG2RAD);
    }

    @Override
    public boolean uvLock() {
        return uvLock;
    }

    public ResourceLoc.ModelLoc model() {
        return model;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }
}
