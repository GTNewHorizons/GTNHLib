/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import static com.gtnewhorizon.gtnhlib.bytebuf.Checks.DEBUG;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryStack.POINTER_SHIFT;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryStack.POINTER_SIZE;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.NULL;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memASCII;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memAddress;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memGetAddress;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memPointerBuffer;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memPutAddress;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memPutDouble;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memPutFloat;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.memPutLong;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.nmemFree;
import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.wrapBufferByte;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;

/**
 * Utility class useful to API bindings. [INTERNAL USE ONLY]
 *
 * <p>
 * Method names in this class are prefixed with {@code api} to avoid ambiguities when used with static imports.
 * </p>
 *
 */
public final class APIUtil {

    /**
     * The {@link PrintStream} used by LWJGL to print debug information and non-fatal errors. Defaults to
     * {@link System#err} which can be changed with
     */
    public static final PrintStream DEBUG_STREAM = getDebugStream();

    private static final Pattern API_VERSION_PATTERN;

    static {
        String PREFIX = "[^\\d\\n\\r]*";
        String VERSION = "(\\d+)[.](\\d+)(?:[.](\\S+))?";
        String IMPLEMENTATION = "(?:\\s+(.+?))?\\s*";

        API_VERSION_PATTERN = Pattern.compile("^" + PREFIX + VERSION + IMPLEMENTATION + "$", Pattern.DOTALL);
    }

    @SuppressWarnings({ "unchecked", "UseOfSystemOutOrSystemErr" })
    private static PrintStream getDebugStream() {
        PrintStream debugStream = System.err;

        Object state = System.err;
        if (state instanceof String) {
            try {
                Supplier<PrintStream> factory = (Supplier<PrintStream>) Class.forName((String) state).getConstructor()
                        .newInstance();
                debugStream = factory.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (state instanceof Supplier<?>) {
            debugStream = ((Supplier<PrintStream>) state).get();
        } else if (state instanceof PrintStream) {
            debugStream = (PrintStream) state;
        }

        return debugStream;
    }

    private APIUtil() {}

    /**
     * Prints the specified message to the {@link #DEBUG_STREAM} if {@link Checks#DEBUG} is true.
     *
     * @param msg the message to print
     */
    public static void apiLog(CharSequence msg) {
        if (DEBUG) {
            DEBUG_STREAM.print("[LWJGL] " + msg + "\n");
        }
    }

    /**
     * Same as {@link #apiLog}, but replaces the LWJGL prefix with a tab character.
     *
     * @param msg the message to print, in continuation of a previous message
     */
    public static void apiLogMore(CharSequence msg) {
        if (DEBUG) {
            DEBUG_STREAM.print("\t" + msg + "\n");
        }
    }

    public static void apiLogMissing(String api, ByteBuffer functionName) {
        if (DEBUG) {
            String function = memASCII(functionName, functionName.remaining() - 1);
            DEBUG_STREAM.print("[LWJGL] Failed to locate address for " + api + " function " + function + "\n");
        }
    }

    public static @Nullable ByteBuffer apiGetMappedBuffer(@Nullable ByteBuffer buffer, long mappedAddress,
            int capacity) {
        if (buffer != null && memAddress(buffer) == mappedAddress && buffer.capacity() == capacity) {
            return buffer;
        }
        return mappedAddress == NULL ? null : wrapBufferByte(mappedAddress, capacity);
    }

    public static long apiGetBytes(int elements, int elementShift) {
        return (elements & 0xFFFF_FFFFL) << elementShift;
    }

    public static long apiCheckAllocation(int elements, long bytes, long maxBytes) {
        if (DEBUG) {
            if (elements < 0) {
                throw new IllegalArgumentException("Invalid number of elements");
            }
            if ((maxBytes + Long.MIN_VALUE) < (bytes + Long.MIN_VALUE)) { // unsigned comparison
                throw new IllegalArgumentException("The request allocation is too large");
            }
        }
        return bytes;
    }

    /** A data class for API versioning information. */
    public static class APIVersion implements Comparable<APIVersion> {

        /** Returns the API major version. */
        public final int major;
        /** Returns the API minor version. */
        public final int minor;

        /** Returns the API revision. May be null. */
        public final @Nullable String revision;
        /** Returns the API implementation-specific versioning information. May be null. */
        public final @Nullable String implementation;

        public APIVersion(int major, int minor) {
            this(major, minor, null, null);
        }

        public APIVersion(int major, int minor, @Nullable String revision, @Nullable String implementation) {
            this.major = major;
            this.minor = minor;
            this.revision = revision;
            this.implementation = implementation;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(16);
            sb.append(major).append('.').append(minor);
            if (revision != null) {
                sb.append('.').append(revision);
            }
            if (implementation != null) {
                sb.append(" (").append(implementation).append(')');
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof APIVersion)) {
                return false;
            }

            APIVersion that = (APIVersion) o;

            return this.major == that.major && this.minor == that.major
                    && Objects.equals(this.revision, that.revision)
                    && Objects.equals(this.implementation, that.implementation);
        }

        @Override
        public int hashCode() {
            int result = major;
            result = 31 * result + minor;
            result = 31 * result + (revision != null ? revision.hashCode() : 0);
            result = 31 * result + (implementation != null ? implementation.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(APIVersion other) {
            if (this.major != other.major) {
                return Integer.compare(this.major, other.major);
            }

            if (this.minor != other.minor) {
                return Integer.compare(this.minor, other.minor);
            }

            return 0;
        }
    }

    /**
     * Returns the {@link APIVersion} value of the specified option.
     *
     * @param option the option to query
     */
    public static @Nullable APIVersion apiParseVersion(Object option) {
        APIVersion version;

        Object state = option;
        if (state instanceof String) {
            version = apiParseVersion((String) state);
        } else if (state instanceof APIVersion) {
            version = (APIVersion) state;
        } else {
            version = null;
        }

        return version;
    }

    /**
     * Parses a version string.
     *
     * <p>
     * The version string must have the format {@code PREFIX MAJOR.MINOR.REVISION IMPL}, where {@code PREFIX} is a
     * prefix without digits (string, optional), {@code MAJOR} is the major version (integer), {@code MINOR} is the
     * minor version (integer), {@code REVISION} is the revision version (string, optional) and {@code IMPL} is
     * implementation-specific information (string, optional).
     * </p>
     *
     * @param version the version string
     *
     * @return the parsed {@link APIVersion}
     */
    public static APIVersion apiParseVersion(String version) {
        Matcher matcher = API_VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Malformed API version string [%s]", version));
        }

        return new APIVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3),
                matcher.group(4));
    }

    public static String apiUnknownToken(int token) {
        return apiUnknownToken("Unknown", token);
    }

    public static String apiUnknownToken(String description, int token) {
        return String.format("%s [0x%X]", description, token);
    }

    /**
     * Returns a map of public static final integer fields in the specified classes, to their String representations. An
     * optional filter can be specified to only include specific fields. The target map may be null, in which case a new
     * map is allocated and returned.
     *
     * <p>
     * This method is useful when debugging to quickly identify values returned from an API.
     * </p>
     *
     * @param filter       the filter to use (optional)
     * @param target       the target map (optional)
     * @param tokenClasses the classes to get tokens from
     *
     * @return the token map
     */
    public static Map<Integer, String> apiClassTokens(@Nullable BiPredicate<Field, Integer> filter,
            @Nullable Map<Integer, String> target, Class<?>... tokenClasses) {
        if (target == null) {
            // noinspection AssignmentToMethodParameter
            target = new HashMap<>(64);
        }

        int TOKEN_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

        for (Class<?> tokenClass : tokenClasses) {
            for (Field field : tokenClass.getDeclaredFields()) {
                // Get only <public static final int> fields.
                if ((field.getModifiers() & TOKEN_MODIFIERS) == TOKEN_MODIFIERS && field.getType() == int.class) {
                    try {
                        Integer value = field.getInt(null);
                        if (filter != null && !filter.test(field, value)) {
                            continue;
                        }

                        String name = target.get(value);
                        target.put(value, name == null ? field.getName() : name + "|" + field.getName());
                    } catch (IllegalAccessException e) {
                        // Ignore
                    }
                }
            }
        }

        return target;
    }

    // ----------------------------------------

    /**
     * Stores the specified array of pointer addresses on the specified {@link MemoryStack}.
     *
     * @param stack     the stack to use
     * @param addresses the pointer addresses to store
     *
     * @return the pointer array address on the stack
     */
    public static long apiArray(MemoryStack stack, long... addresses) {
        PointerBuffer pointers = memPointerBuffer(
                stack.nmalloc(POINTER_SIZE, addresses.length << POINTER_SHIFT),
                addresses.length);

        for (long address : addresses) {
            pointers.put(address);
        }

        return memAddress(pointers);
    }

    /**
     * Stores the addresses of the specified array of buffers on the specified {@link MemoryStack}.
     *
     * @param stack   the stack to use
     * @param buffers the buffers to store
     *
     * @return the pointer array address on the stack
     */
    public static long apiArray(MemoryStack stack, ByteBuffer... buffers) {
        PointerBuffer pointers = memPointerBuffer(
                stack.nmalloc(POINTER_SIZE, buffers.length << POINTER_SHIFT),
                buffers.length);

        for (ByteBuffer buffer : buffers) {
            pointers.put(memAddress(buffer));
        }

        return memAddress(pointers);
    }

    /**
     * Stores the addresses of the specified array of buffers on the specified {@link MemoryStack}. A second array that
     * contains the buffer remaining bytes is stored immediately after the pointer array. Length values are
     * pointer-sized integers.
     *
     * @param stack   the stack to use
     * @param buffers the buffers to store
     *
     * @return the pointer array address on the stack
     */
    public static long apiArrayp(MemoryStack stack, ByteBuffer... buffers) {
        long pointers = apiArray(stack, buffers);

        PointerBuffer lengths = stack.mallocPointer(buffers.length);
        for (ByteBuffer buffer : buffers) {
            lengths.put(buffer.remaining());
        }

        return pointers;
    }

    // ----------------------------------------

    public interface Encoder {

        ByteBuffer encode(CharSequence text, boolean nullTerminated);
    }

    /**
     * Encodes the specified strings with the specified {@link Encoder} and stores an array of pointers to the encoded
     * data on the specified {@link MemoryStack}. The encoded strings include null-termination.
     *
     * @param stack   the stack to use
     * @param encoder the encoder to use
     * @param strings the strings to encode
     *
     * @return the pointer array address on the stack
     */
    public static long apiArray(MemoryStack stack, Encoder encoder, CharSequence... strings) {
        PointerBuffer pointers = stack.mallocPointer(strings.length);

        for (CharSequence s : strings) {
            pointers.put(memAddress(encoder.encode(s, true)));
        }

        return memAddress(pointers);
    }

    /**
     * Encodes the specified strings with the specified {@link Encoder} and stores an array of pointers to the encoded
     * data on the specified {@link MemoryStack}. A second array that contains the string lengths is stored immediately
     * after the pointer array. Length values are 4-byte integers.
     *
     * <p>
     * The encoded buffers must be freed with {@link #apiArrayFree}.
     * </p>
     *
     * @param stack   the stack to use
     * @param encoder the encoder to use
     * @param strings the strings to encode
     *
     * @return the pointer array address on the stack
     */
    public static long apiArrayi(MemoryStack stack, Encoder encoder, CharSequence... strings) {
        // Alignment rules guarantee these two will be contiguous
        PointerBuffer pointers = stack.mallocPointer(strings.length);
        IntBuffer lengths = stack.mallocInt(strings.length);

        for (CharSequence s : strings) {
            ByteBuffer buffer = encoder.encode(s, false);

            pointers.put(memAddress(buffer));
            lengths.put(buffer.capacity());
        }

        return memAddress(pointers);
    }

    /**
     * Encodes the specified strings with the specified {@link Encoder} and stores an array of pointers to the encoded
     * data on the specified {@link MemoryStack}. A second array that contains the string lengths is stored immediately
     * after the pointer array. Length values are pointer-sized integers.
     *
     * <p>
     * The encoded buffers must be freed with {@link #apiArrayFree}.
     * </p>
     *
     * @param stack   the stack to use
     * @param encoder the encoder to use
     * @param strings the strings to encode
     *
     * @return the pointer array address on the stack
     */
    public static long apiArrayp(MemoryStack stack, Encoder encoder, CharSequence... strings) {
        PointerBuffer pointers = stack.mallocPointer(strings.length);
        PointerBuffer lengths = stack.mallocPointer(strings.length);

        for (CharSequence s : strings) {
            ByteBuffer buffer = encoder.encode(s, false);

            pointers.put(memAddress(buffer));
            lengths.put(buffer.capacity());
        }

        return memAddress(pointers);
    }

    /**
     * Frees the specified array of pointers.
     *
     * @param pointers the pointer array to free
     * @param length   the pointer array length
     */
    public static void apiArrayFree(long pointers, int length) {
        for (int i = length; --i >= 0;) {
            nmemFree(memGetAddress(pointers + Integer.toUnsignedLong(i) * Pointer.POINTER_SIZE));
        }
    }

    public static void apiClosureRet(long ret, boolean __result) {
        memPutAddress(ret, __result ? 1L : 0L);
    }

    public static void apiClosureRet(long ret, byte __result) {
        memPutAddress(ret, __result & 0xFFL);
    }

    public static void apiClosureRet(long ret, short __result) {
        memPutAddress(ret, __result & 0xFFFFL);
    }

    public static void apiClosureRet(long ret, int __result) {
        memPutAddress(ret, __result & 0xFFFF_FFFFL);
    }

    public static void apiClosureRetL(long ret, long __result) {
        memPutLong(ret, __result);
    }

    public static void apiClosureRetP(long ret, long __result) {
        memPutAddress(ret, __result);
    }

    public static void apiClosureRet(long ret, float __result) {
        memPutFloat(ret, __result);
    }

    public static void apiClosureRet(long ret, double __result) {
        memPutDouble(ret, __result);
    }

}
