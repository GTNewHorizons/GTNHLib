package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;

public interface FloatBlockProperty extends BlockProperty<Float> {

    @Override
    default Type getType() {
        return float.class;
    }
}
