package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;

/// This class is mixed into {@link net.minecraft.block.Block}, so the model loader can store model information on them.
/// The data must *only* be mutated by the resource reload manager, but can be read by anyone. If your model can only
/// be found by a custom locator, you can implement this yourself and override [#nhlib$isModeled()] to always be true.
public interface BlockModelInfo {

    boolean nhlib$isModeled();

    void nhlib$setModeled(boolean modeled);

    static boolean isModeled(Block block) {
        return ((BlockModelInfo) block).nhlib$isModeled();
    }
}
