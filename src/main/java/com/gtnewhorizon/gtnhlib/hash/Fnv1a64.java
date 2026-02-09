package com.gtnewhorizon.gtnhlib.hash;

/**
 * The 64-bit version of <a href="http://www.isthe.com/chongo/tech/comp/fnv/index.html">Fowler/Noll/Vo 1-alternate</a>
 * hash. To use this class, you need to keep track of a single {@code int} worth of state yourself, for example:
 *
 * <pre>
 * long hash = Fnv1a64.initialState();
 * hash = Fnv1a64.hashStep(hash, value1);
 * hash = Fnv1a64.hashStep(hash, value2);
 * hash = Fnv1a64.hashStep(hash, value3);
 * </pre>
 *
 * The final hash is just the state after performing all the hashing steps, there is no separate finalization step
 * needed.
 * 
 * The values produced by this class will not be the same as a true Fnv1a 64-bit implementation, because of a bug where
 * Java's signed byte type causes any octet with the highest (sign) bit set to flip the upper 56 bytes of the state in
 * the XOR step. As it's just flipping all the bits, it shouldn't affect dispersion that much in theory, so it is left
 * in to not change the behaviour of existing worldgen code.
 */
@SuppressWarnings("unused") // Public API
public final class Fnv1a64 {

    /** The FNV_prime constant for this hash function */
    public static final long PRIME64 = 0x100_0000_01B3L;
    /** The offset_basis constant for this hash function */
    public static final long OFFSET_BASIS64 = 0xCBF2_9CE4_8422_2325L;

    /**
     * @return The value to assign to the state variable to begin constructing a new hash.
     */
    public static long initialState() {
        return OFFSET_BASIS64;
    }

    /**
     * Hashes a single byte into the given state, and returns the updated state.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(final long prevState, final byte value) {
        // bug: see class javadoc, if this is to be fixed (ie on a major/breaking version) then:
        // return (prevState ^ (value & 0xFFL)) * PRIME64;
        return (prevState ^ value) * PRIME64;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Booleans are converted to
     * a 0/1 byte first.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(final long prevState, final boolean value) {
        return hashStep(prevState, value ? (byte) 1 : (byte) 0);
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Chars are hashed like
     * their underlying short representation.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(final long prevState, final char value) {
        return hashStep(prevState, (short) value);
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Integers are hashed in
     * order from most to least significant byte.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(long prevState, final short value) {
        prevState = hashStep(prevState, (byte) ((value >> 8) & 0xFF));
        prevState = hashStep(prevState, (byte) (value & 0xFF));
        return prevState;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Integers are hashed in
     * order from most to least significant byte.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(long prevState, final int value) {
        prevState = hashStep(prevState, (byte) ((value >> 24) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 16) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 8) & 0xFF));
        prevState = hashStep(prevState, (byte) (value & 0xFF));
        return prevState;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Integers are hashed in
     * order from most to least significant byte.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(long prevState, final long value) {
        prevState = hashStep(prevState, (byte) ((value >> 56) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 48) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 40) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 32) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 24) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 16) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 8) & 0xFF));
        prevState = hashStep(prevState, (byte) (value & 0xFF));
        return prevState;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Floats are hashed in
     * order from most to least significant byte of their integer representation.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(final long prevState, final float value) {
        return hashStep(prevState, Float.floatToRawIntBits(value));
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Floats are hashed in
     * order from most to least significant byte of their integer representation.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static long hashStep(final long prevState, final double value) {
        return hashStep(prevState, Double.doubleToLongBits(value));
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final byte[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final boolean[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final char[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final short[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final int[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final long[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final float[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final double[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes any {@link CharSequence} (including {@link String}) by hashing each individual code unit in it.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static long hashStep(long prevState, final CharSequence array) {
        final int length = array.length();
        for (int i = 0; i < length; i++) {
            prevState = hashStep(prevState, array.charAt(i));
        }
        return prevState;
    }
}
