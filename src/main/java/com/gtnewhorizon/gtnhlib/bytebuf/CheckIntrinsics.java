/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

/**
 * Simple index checks.
 *
 * <p>
 * On Java 9 these checks are replaced with the corresponding {@link java.util.Objects} methods, which perform better.
 * </p>
 */
public final class CheckIntrinsics {

    private CheckIntrinsics() {}

    public static int classVersion() {
        return 8;
    }

    public static int checkIndex(int index, int length) {
        if (index < 0 || length <= index) {
            throw new IndexOutOfBoundsException();
        }
        return index;
    }

    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || toIndex < fromIndex || length < toIndex) {
            throw new IndexOutOfBoundsException();
        }
        return fromIndex;
    }

    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if ((length | fromIndex | size) < 0 || length - fromIndex < size) {
            throw new IndexOutOfBoundsException();
        }
        return fromIndex;
    }

    public static ByteBuffer NewDirectByteBuffer(long address, int capacity) {
        try {
            // Should work on OpenJDK 8 and OpenJ9 8
            @SuppressWarnings("unchecked")
            final Class<? extends ByteBuffer> dbb = (Class<? extends ByteBuffer>) Class
                    .forName("java.nio.DirectByteBuffer");
            final Constructor<? extends ByteBuffer> newDbb = dbb.getDeclaredConstructor(long.class, int.class);
            newDbb.setAccessible(true);
            return newDbb.newInstance(address, capacity);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MemoryUtilities.MemoryAllocator getLwjgl3ifyAllocator() {
        return null;
    }

}
