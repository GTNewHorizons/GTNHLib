package com.gtnewhorizon.gtnhlib.geometry;

import org.joml.Vector3f;

/// A version of {@link DirectionTransform} that can transform arbitrary vectors.
public interface VectorTransform extends TransformLike {

    /// Transforms the vector. Does not copy the parameter.
    Vector3f transform(Vector3f v);

    /// Performs the inverse transform on the vector. Does not copy the parameter.
    Vector3f inverse(Vector3f v);
}
