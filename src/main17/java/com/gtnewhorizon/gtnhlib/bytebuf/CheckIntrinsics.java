/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.jetbrains.annotations.NotNullByDefault;

import me.eigenraven.lwjgl3ify.api.Lwjgl3Aware;

/** Java 9 version of {@code CheckIntrinsics}. */
@NotNullByDefault
@Lwjgl3Aware
public final class CheckIntrinsics {

    private CheckIntrinsics() {}

    public static int classVersion() {
        return 17;
    }

    public static int checkIndex(int index, int length) {
        return Objects.checkIndex(index, length);
    }

    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        return Objects.checkFromToIndex(fromIndex, toIndex, length);
    }

    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        return Objects.checkFromIndexSize(fromIndex, size, length);
    }

    public static ByteBuffer NewDirectByteBuffer(long address, int capacity) {
        return org.lwjgl.system.jni.JNINativeInterface.NewDirectByteBuffer(address, capacity);
    }

    public static MemoryUtilities.MemoryAllocator getLwjgl3ifyAllocator() {
        return new Lwjgl3ifyAllocator();
    }

    @Lwjgl3Aware
    private static final class Lwjgl3ifyAllocator implements MemoryUtilities.MemoryAllocator {

        @Override
        public long malloc(long size) {
            return org.lwjgl.system.MemoryUtil.nmemAlloc(size);
        }

        @Override
        public long calloc(long num, long size) {
            return org.lwjgl.system.MemoryUtil.nmemCalloc(num, size);
        }

        @Override
        public long realloc(long ptr, long size) {
            return org.lwjgl.system.MemoryUtil.nmemRealloc(ptr, size);
        }

        @Override
        public void free(long ptr) {
            org.lwjgl.system.MemoryUtil.nmemFree(ptr);
        }
    }

}
