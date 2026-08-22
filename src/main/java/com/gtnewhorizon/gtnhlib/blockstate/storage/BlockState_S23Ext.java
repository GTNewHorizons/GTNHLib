package com.gtnewhorizon.gtnhlib.blockstate.storage;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.google.gson.JsonObject;

@Internal
public interface BlockState_S23Ext {

    JsonObject gtnhlib$getBlockState();
    void gtnhlib$setBlockState(JsonObject state);

}
