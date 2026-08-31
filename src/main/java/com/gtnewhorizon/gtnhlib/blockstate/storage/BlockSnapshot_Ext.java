package com.gtnewhorizon.gtnhlib.blockstate.storage;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;

public interface BlockSnapshot_Ext {

    @Nullable BlockState gtnhlib$getCapturedState();
    void gtnhlib$setCapturedState(@Nullable BlockState state);

}
