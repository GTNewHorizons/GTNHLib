package com.gtnewhorizon.gtnhlib.datacomponent.components;

import java.lang.reflect.Type;

import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentType;

public interface FloatComponent extends DataComponentType<Float> {

    @Override
    default Type getType() {
        return float.class;
    }
}
