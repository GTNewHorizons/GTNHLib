package com.gtnewhorizon.gtnhlib.blockstate.core;

import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty.BooleanMetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.FloatBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.FloatBlockProperty.FloatMetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty.IntegerMetaBlockProperty;

public enum BlockPropertyTrait {
    /// If this property can interact with blocks in the world
    SupportsWorld,
    /// If this property can interact with blocks in ItemStacks. The Item must extend [ItemBlock].
    SupportsStacks,
    /// If this property is exclusively derived from metadata (block and item) and implements [MetaBlockProperty].
    OnlyNeedsMeta,
    /// If this property can be changed while the block exists in the world, regardless of whether this would spawn
    /// items/etc into existence.
    WorldMutable,
    /// If this property can be changed while the block is in stack form, regardless of whether this would spawn
    /// items/etc into existence.
    StackMutable,
    /// If this property can be treated like a 'setting' and will not give anything to the player when changed.
    Config,
    /// If this property can be transformed by a direction transform and implements [TransformableProperty].
    Transformable,
    /// If this property can be transformed by a vector transform and implements [VectorTransformableProperty].
    VectorTransformable,
    /// If this property can be safely downcast to a primitive specialization property.
    /// @see BooleanBlockProperty
    /// @see IntegerBlockProperty
    /// @see FloatBlockProperty
    Primitive,
    /// If this property can be safely downcast to a combined primitive + meta property.
    /// @see BooleanMetaBlockProperty
    /// @see IntegerMetaBlockProperty
    /// @see FloatMetaBlockProperty
    MetaPrimitive,
}
