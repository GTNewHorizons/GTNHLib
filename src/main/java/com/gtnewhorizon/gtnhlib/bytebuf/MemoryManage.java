/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.MemoryAllocator;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.getUnsafeInstance;

import net.minecraft.launchwrapper.Launch;

/** Provides {@link MemoryAllocator} implementations for {@link MemoryUtilities} to use. */
final class MemoryManage {

    private MemoryManage() {}

    static MemoryAllocator getInstance() {
        final boolean hasLwjgl3ify = Launch.blackboard.get("lwjgl3ify:rfb-booted") == Boolean.TRUE;
        return hasLwjgl3ify ? CheckIntrinsics.getLwjgl3ifyAllocator() : new StdlibAllocator();
    }

    /** stdlib memory allocator. */
    private static class StdlibAllocator implements MemoryAllocator {

        static final sun.misc.Unsafe UNSAFE = getUnsafeInstance();

        @Override
        public long malloc(long size) {
            return UNSAFE.allocateMemory(size);
        }

        @Override
        public long calloc(long num, long size) {
            final long totalSize = Math.multiplyExact(num, size);
            final long addr = UNSAFE.allocateMemory(totalSize);
            UNSAFE.setMemory(addr, totalSize, (byte) 0);
            return addr;
        }

        @Override
        public long realloc(long ptr, long size) {
            return UNSAFE.reallocateMemory(ptr, size);
        }

        @Override
        public void free(long ptr) {
            UNSAFE.freeMemory(ptr);
        }
    }

}
