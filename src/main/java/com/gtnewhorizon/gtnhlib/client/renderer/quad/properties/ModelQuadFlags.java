package com.gtnewhorizon.gtnhlib.client.renderer.quad.properties;

public class ModelQuadFlags {

    /**
     * @return True if the bit-flag of {@link ModelQuadFlags} contains the given flag
     */
    public static boolean contains(int flags, int mask) {
        return (flags & mask) != 0;
    }

}
