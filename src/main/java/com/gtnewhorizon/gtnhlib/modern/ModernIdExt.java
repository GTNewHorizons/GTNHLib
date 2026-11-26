package com.gtnewhorizon.gtnhlib.modern;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public interface ModernIdExt {

    /// The id this object gets when nothing has been explicitly registered for it. Pure: does not register anything.
    @NotNull
    ResourceLocation gtnhlib$getDefaultModernId();

}
