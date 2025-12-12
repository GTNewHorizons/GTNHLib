package com.gtnewhorizon.gtnhlib.blockstate.core;

public enum BlockPropertyTrait {
    /// If this property can interact with blocks in the world
    SupportsWorld,
    /// If this property can interact with blocks in ItemStacks. The Item must extend {@link ItemBlock}.
    SupportsStacks,
    /// If this property is exclusively derived from metadata (block and item) and implements {@link MetaBlockProperty}.
    OnlyNeedsMeta,
    /// If this property can be changed while the block exists in the world, regardless of whether this would spawn
    /// items/etc into existence.
    WorldMutable,
    /// If this property can be changed while the block is in stack form, regardless of whether this would spawn
    /// items/etc into existence.
    StackMutable,
    /// If this property can be treated like a 'setting' and will not give anything to the player when changed.
    Config,
    /// If this property can be transformed by a direction transform and implements {@link TransformableProperty}.
    Transformable,
    /// If this property can be transformed by a vector transform and implements {@link VectorTransformableProperty}.
    VectorTransformable,
}
