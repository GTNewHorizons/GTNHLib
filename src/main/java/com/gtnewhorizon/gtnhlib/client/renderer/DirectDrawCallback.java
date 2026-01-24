package com.gtnewhorizon.gtnhlib.client.renderer;

import com.google.common.annotations.Beta;

@Beta // Not a stable API. May change in the future.
public interface DirectDrawCallback {

    boolean onDraw(CallbackTessellator tessellator);
}
