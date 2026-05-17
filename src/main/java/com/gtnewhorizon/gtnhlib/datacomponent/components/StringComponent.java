package com.gtnewhorizon.gtnhlib.datacomponent.components;

import java.lang.reflect.Type;

import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentType;

public interface StringComponent extends DataComponentType<String> {

    @Override
    default Type getType() {
        return String.class;
    }
}
