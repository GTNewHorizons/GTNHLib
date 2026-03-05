package com.gtnewhorizon.gtnhlib.concurrent.cas;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /** Write lock shared by {@link #mutate}, {@link #overwrite}, and {@link #lockAndGet}/{@link #replaceAndUnlock}. */
    protected final ReentrantLock writeLock = new ReentrantLock();

    /**
     * When non-null, the mutable copy checked out by {@link #lock()} or {@link #lockAndGet()} on the thread holding
     * {@link #writeLock}.
     */
    protected @Nullable MutableView lockedMutable;

    /** Monotonically increasing counter, bumped inside the write lock on every publish. */
    private volatile long version;

    /** Re-entrancy depth for {@link #lock()}/{@link #unlock()}. Only accessed by the lock-holding thread. */
    private int lockDepth;

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
     * @return The current version stamp. Incremented on every publish (mutate, overwrite, unlock, replaceAndUnlock).
     */
    public final long version() {
        return version;
    }

    /** @return The current snapshot paired with its version stamp. */
    public final @NotNull Versioned<ImmutableView> readVersioned() {
        // Read version before the reference — if a concurrent write lands between the two reads,
        // the caller gets a stale version with a newer snapshot, which is conservative (they'll
        // re-validate and see a mismatch).
        final long stamp = version;
        return new Versioned<>(reference.get(), stamp);
    }

    /** @return {@code true} if no write has occurred since the given stamp was obtained. */
    public final boolean validateVersion(long stamp) {
        return version == stamp;
    }

    /**
     * Forcefully overwrites the contents with a new value, without checking the previous value. Only call if you know
     * that any concurrent mutations are fine to occur on your new object instead of the old one.
     */
    public final void overwrite(@NotNull ImmutableView newValue) {
        Objects.requireNonNull(newValue);
        writeLock.lock();
        try {
            reference.set(newValue);
            version++;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Creates a mutable copy of the inner data, runs the user-provided mutation function, and stores the new immutable
     * data.
     * <p>
     * If the current thread holds the bulk lock via {@link #lock()} or {@link #lockAndGet()}, the mutator is applied
     * directly to the locked mutable copy (no clone, no publish) — changes accumulate until {@link #unlock()} or
     * {@link #replaceAndUnlock(Object)}.
     *
     * @param mutator The function to modify the contents of this reference.
     * @return The return value of the mutator.
     */
    public final <R> R mutate(@NotNull Function<@NotNull MutableView, R> mutator) {
        final MutableView locked = lockedMutable;
        if (locked != null && writeLock.isHeldByCurrentThread()) {
            return mutator.apply(locked);
        }
        writeLock.lock();
        try {
            final ImmutableView oldVal = reference.get();
            final MutableView mutVal = mutableCopyOf(oldVal);
            Objects.requireNonNull(mutVal);
            lockedMutable = mutVal;
            final R retVal;
            try {
                retVal = mutator.apply(mutVal);
            } finally {
                lockedMutable = null;
            }
            final ImmutableView newVal = immutableCopyOf(mutVal);
            Objects.requireNonNull(newVal);
            reference.set(newVal);
            version++;
            return retVal;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Acquire the write lock and create a mutable working copy. Cross-thread readers continue seeing the old snapshot
     * via {@link #read()}. While the lock is held, {@link #mutate} on the same thread applies directly to the working
     * copy (no clone, no publish per call). The working copy is published when the lock is released.
     * <p>
     * Use this when wrapping code that calls the normal mutation API — changes accumulate transparently until
     * {@link #unlock()}. For direct raw-map access use {@link #lockAndGet()}/{@link #replaceAndUnlock} instead.
     */
    public void lock() {
        if (lockedMutable != null && writeLock.isHeldByCurrentThread()) {
            if (lockDepth == 0) {
                throw new IllegalStateException("Cannot nest lock() inside lockAndGet() scope");
            }
            // Re-entrant: already locked by this thread, just bump depth.
            lockDepth++;
            return;
        }
        writeLock.lock();
        lockedMutable = mutableCopyOf(reference.get());
        lockDepth = 1;
    }

    /**
     * Publish the accumulated changes and release the write lock acquired by {@link #lock()}. If re-entrant, decrements
     * the depth counter; only the outermost {@code unlock()} publishes and releases.
     */
    public void unlock() {
        if (--lockDepth > 0) {
            return;
        }
        try {
            final MutableView locked = lockedMutable;
            lockedMutable = null;
            if (locked != null) {
                reference.set(immutableCopyOf(locked));
                version++;
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Lock for exclusive write access and return a mutable copy for direct manipulation. Cross-thread readers continue
     * seeing the old snapshot via {@link #read()}.
     * <p>
     * Use this when you want to operate directly on the returned map, then call {@link #replaceAndUnlock(Object)}.
     *
     * @return A mutable copy for in-place modification.
     */
    public MutableView lockAndGet() {
        if (lockedMutable != null && writeLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Already locked — cannot nest lock() and lockAndGet() calls");
        }
        writeLock.lock();
        final MutableView mutable = mutableCopyOf(reference.get());
        lockedMutable = mutable;
        return mutable;
    }

    /**
     * Publish the modified data and release the write lock acquired by {@link #lockAndGet()}.
     *
     * @param modified The mutable copy returned by (or derived from) {@link #lockAndGet()}.
     */
    public void replaceAndUnlock(MutableView modified) {
        try {
            lockedMutable = null;
            reference.set(immutableCopyOf(modified));
            version++;
        } finally {
            writeLock.unlock();
        }
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
