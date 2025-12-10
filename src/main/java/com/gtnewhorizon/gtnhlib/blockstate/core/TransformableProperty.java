package com.gtnewhorizon.gtnhlib.blockstate.core;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;

/// This indicates that a property can be transformed in the world according to an arbitrary transform.
public interface TransformableProperty<TValue> extends BlockProperty<TValue> {

    /// The transform function may contain any transform, but there is no guarantee that this property has the degrees
    /// of freedom to properly respect it.
    /// See the header comment {@link BlockProperty} for this method's mutability semantics.
    @NotNull
    TValue transform(TValue value, DirectionTransform transform);
}
