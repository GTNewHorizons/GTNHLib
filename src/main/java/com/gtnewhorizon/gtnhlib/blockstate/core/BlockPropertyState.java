package com.gtnewhorizon.gtnhlib.blockstate.core;

/// Describes the resolution state of a single property value within a [BlockState].
/// See the [BlockState] header comment for the full lifecycle description.
public enum BlockPropertyState {
    /// The property reference is fully resolved and the value has been validated against a real block in the world.
    /// This is the normal state for properties obtained via
    /// [com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry#getBlockState].
    NORMAL,
    /// The property reference is known, but the value has not been validated against a block in the world.
    /// This occurs when a property is injected externally via [BlockState#setPropertyValue(BlockProperty, Object)].
    UNVALIDATED,
    /// Only a name-value string pair is held; the property reference has not yet been resolved.
    /// This occurs when a property is set via [BlockState#setPropertyValue(String, Object)] and no matching
    /// property is currently loaded in the state.
    DEFERRED;
}
