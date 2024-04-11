package com.gtnewhorizon.gtnhlib.concurrent.cas;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * A Compare-and-Swap adapter for any reference type. Extend it for your reference type, and implement the missing
 * methods to use.
 * <p>
 * When you have a object that is read from very frequently, but mutated rarely, this class allows conveniently using it
 * with reads as fast as an unsynchronized object, at the cost of writes that are much slower when they try to happen
 * concurrently.
 * <p>
 * This works by wrapping an object into a {@link java.util.concurrent.atomic.AtomicReference}, and updating the
 * reference with a new immutable copy of data every time it's updated. The data must not be null to avoid the ABA
 * problem.
 *
 * @param <ImmutableView> The type of the stored reference, should be immutable (or at least not mutated by users)
 * @param <MutableView>   The type that's easy to construct from the stored reference, used for safe mutations.
 */
@SuppressWarnings("unused") // API type
public abstract class CasAdapter<ImmutableView, MutableView> {

    private final AtomicReference<@NotNull ImmutableView> reference;

    /** Initializes the inner reference to the given value. */
    public CasAdapter(@NotNull ImmutableView initial) {
        Objects.requireNonNull(initial);
        this.reference = new AtomicReference<>(initial);
    }

    /** @return An immutable view of the stored object, do not mutate. */
    public final @NotNull ImmutableView read() {
        return reference.get();
    }

    /**
     * Forcefully overwrites the contents with a new value, without checking the previous value. Only call if you know
     * that any concurrent mutations are fine to occur on your new object instead of the old one.
     */
    public final void overwrite(@NotNull ImmutableView newValue) {
        Objects.requireNonNull(newValue);
        // synchronize to avoid racing with this.mutate
        synchronized (this) {
            reference.set(newValue);
        }
    }

    /**
     * Creates a mutable copy of the inner data, runs the user-provided mutation function, and stores the new immutable
     * data. Ensures the modification happens by using an atomic compare-and-set operation, and retrying the mutation on
     * failure.
     *
     * @param mutator The function to modify the contents of this reference. Must be safe to invoke multiple times.
     * @return The newly updated value.
     */
    public final <R> R mutate(@NotNull Function<@NotNull MutableView, R> mutator) {
        do {
            // synchronize to avoid multiple racy CAS loops whenever possible
            synchronized (this) {
                final ImmutableView oldVal = reference.get();
                final MutableView mutVal = mutableCopyOf(oldVal);
                Objects.requireNonNull(mutVal);
                final R retVal = mutator.apply(mutVal);
                final ImmutableView newVal = immutableCopyOf(mutVal);
                Objects.requireNonNull(newVal);
                final boolean success = reference.compareAndSet(oldVal, newVal);
                // Because of the synchronized block, this should always succeed - but future-proof with a CAS loop to
                // be on the safe side
                if (success) {
                    return retVal;
                }
            }
        } while (true);
    }

    /**
     * This function must create a mutable copy of the data stored in an immutable view. If the data is not a copy, this
     * container is no longer thread-safe.
     *
     * @param data The immutable data
     * @return The mutable data copy
     */
    protected abstract @NotNull MutableView mutableCopyOf(@NotNull ImmutableView data);

    /**
     * This function must create an immutable copy of the data stored in a mutable view. If the data is not a copy, this
     * container is no longer thread-safe if a mutable reference escapes from the mutate method.
     *
     * @param data The mutable data
     * @return The immutable data copy
     */
    protected abstract @NotNull ImmutableView immutableCopyOf(@NotNull MutableView data);
}
