package com.gtnewhorizon.gtnhlib.bytebuf;

import java.nio.ByteBuffer;

import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;

/**
 * String decoding utilities.
 *
 * <p>
 * On Java 9 different implementations are used that work better with compact strings (JEP 254).
 * </p>
 */
@Lwjgl3Aware
final class MultiReleaseTextDecoding {

    private MultiReleaseTextDecoding() {}

    public static int classVersion() {
        return 17;
    }

    /** @see MemoryUtilities#memUTF8(ByteBuffer, int, int) */
    static String decodeUTF8(long source, int length) {
        return org.lwjgl.system.MemoryUtil.memUTF8(source, length);
    }

}
