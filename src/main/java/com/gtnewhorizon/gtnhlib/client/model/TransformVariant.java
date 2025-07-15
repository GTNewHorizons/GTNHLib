package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraft.util.ResourceLocation;

import org.joml.Matrix4f;

import com.google.common.annotations.Beta;

/**
 * A subclass of {@link Variant} which takes an arbitrary matrix to transform the model rather than a set of axis
 * rotations. Not forward compatible with the Vanilla block state format.
 */
@Beta
public class TransformVariant extends Variant {

    private final Matrix4f transform;

    public TransformVariant(ResourceLocation model, Matrix4f transform, boolean uvLock) {
        super(model, 0, 0, 0, uvLock);
        this.transform = transform;
    }

    @Override
    public Matrix4f getAffineMatrix() {
        return new Matrix4f(transform);
    }
}
