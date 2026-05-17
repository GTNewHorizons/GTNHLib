package com.gtnewhorizon.gtnhlib.datacomponent.components;

import java.lang.reflect.Type;

import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentType;

public interface IntegerComponent extends DataComponentType<Integer> {

    @Override
    default Type getType() {
        return int.class;
    }
}
