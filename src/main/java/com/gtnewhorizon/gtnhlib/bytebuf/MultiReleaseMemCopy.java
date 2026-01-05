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
        // A custom Java loop is fastest at small sizes, approximately up to 160 bytes.
        if (BITS64 && bytes < 160L && ((src | dst) & 7L) == 0L) {
            // both src and dst are aligned to 8 bytes
            memCopyAligned64(src, dst, (int) bytes);
        } else {
            // Unaligned fallback. Poor performance until Java 16.
            UNSAFE.copyMemory(null, src, null, dst, bytes);
        }
    }

}
