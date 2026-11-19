package com.gtnewhorizon.gtnhlib.blockstate.core;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface BlockPropertyValueConsumer {

    /// Called for each block property value in a block state.
    /// @param name The property name
    /// @param state The property value's current state
    /// @param property The property, or null if this value is deferred
    /// @param value The value. The exact value [Class] depends on the property value's state - may be a [String] if
    /// deferred.
    @SuppressWarnings("rawtypes")
    void accept(String name, BlockPropertyState state, @Nullable BlockProperty property, Object value);

}
