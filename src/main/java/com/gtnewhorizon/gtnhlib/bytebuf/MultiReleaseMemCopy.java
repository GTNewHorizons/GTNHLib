/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.UNSAFE;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memCopyAligned64;
import static com.gtnewhorizon.gtnhlib.bytebuf.Pointer.BITS64;

final class MultiReleaseMemCopy {

    private MultiReleaseMemCopy() {}

    public static int classVersion() {
        return 8;
    }

    static void copy(long src, long dst, long bytes) {
        // A custom Java loop is fastest at small sizes, approximately up to 64 bytes.
        if (bytes < 64 && BITS64 && ((src | dst) & 7) == 0) {
            // both src and dst are aligned to 8 bytes
            memCopyAligned64(src, dst, (int) bytes & 0x3F);
        } else {
            // Unaligned fallback. Poor performance until Java 16.
            UNSAFE.copyMemory(null, src, null, dst, bytes);
        }
    }

}
