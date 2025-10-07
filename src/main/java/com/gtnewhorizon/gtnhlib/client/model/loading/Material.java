package com.gtnewhorizon.gtnhlib.client.model.loading;

public class Material {

    // If true, uses diffuse shading/shadows
    private static final int DIFFUSE_MASK = 1 << 2;
    private static final int AO_MASK = 1 << 3;

    // By default, quads use diffuse shading and AO
    private static final int DEFAULTS = DIFFUSE_MASK | AO_MASK;

    private int flags = DEFAULTS;

    private boolean getMask(int mask) {
        return (this.flags & mask) == mask;
    }

    public boolean getDiffuse() {
        return getMask(DIFFUSE_MASK);
    }
}
