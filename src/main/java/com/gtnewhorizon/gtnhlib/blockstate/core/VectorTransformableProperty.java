package com.gtnewhorizon.gtnhlib.blockstate.core;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.geometry.VectorTransform;

/// A version of {@link TransformableProperty} that supports arbitrary vector transformation, instead of explicitly
/// locking the vectors to a ForgeDirection.
public interface VectorTransformableProperty<TValue> extends BlockProperty<TValue> {

    /// The transform function may contain any transform, but there is no guarantee that this property has the degrees
    /// of freedom to properly respect it.
    /// See the header comment {@link BlockProperty} for this method's mutability semantics.
    @NotNull
    TValue transform(TValue value, VectorTransform transform);
}
