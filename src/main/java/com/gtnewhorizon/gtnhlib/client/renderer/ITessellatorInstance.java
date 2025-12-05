package com.gtnewhorizon.gtnhlib.client.renderer;

/**
 * Interface for Tessellator instances to support GTNHLib's capturing/compiling features.
 */
public interface ITessellatorInstance {

    /**
     * Discard the current drawing state without rendering. Standard implementation should set isDrawing = false and
     * call reset().
     */
    void discard();

    /**
     * Check if this Tessellator instance is in compiling mode (display list compilation).
     *
     * @return true if currently compiling, false otherwise
     */
    boolean gtnhlib$isCompiling();

    /**
     * Set the compiling mode flag for this Tessellator instance.
     *
     * @param compiling true to enable compiling mode, false to disable
     */
    void gtnhlib$setCompiling(boolean compiling);
}
