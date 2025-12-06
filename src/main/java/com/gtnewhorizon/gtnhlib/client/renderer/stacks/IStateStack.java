package com.gtnewhorizon.gtnhlib.client.renderer.stacks;

public interface IStateStack<T> {

    /**
     * Push the current state onto the stack.
     *
     * @return This state stack for chaining
     */
    T push();

    /**
     * Pop a state from the stack.
     *
     * @return This state stack for chaining
     */
    T pop();

    /**
     * Check if the stack is empty.
     *
     * @return true if the stack is empty
     */
    boolean isEmpty();

    /**
     * Clear the stack and reset to default state.
     *
     * @return This state stack for chaining
     */
    @SuppressWarnings("unchecked")
    default T clear() {
        while (!isEmpty()) {
            pop();
        }
        return (T) this;
    }

    // ==================== Lazy Copy-on-Write API ====================

    /**
     * Increment depth counter without copying state (lazy push).
     * <p>
     * This is the entry point for lazy copy-on-write optimization. Unlike {@link #push()}, this method does not
     * immediately save the current state. Instead, the state will only be saved when {@link #beforeModify()} is called,
     * indicating that a modification is about to occur.
     * <p>
     * Usage pattern:
     *
     * <pre>
     * stack.pushDepth();
     * // ... later, before modifying state:
     * stack.beforeModify();
     * stack.setValue(newValue);
     * // ... when done:
     * stack.popDepth();
     * </pre>
     *
     * @return The new depth after pushing
     */
    default int pushDepth() {
        push();
        return getDepth();
    }

    /**
     * Decrement depth and restore state only if it was modified at this depth.
     * <p>
     * For stacks that track modifications, this will only restore the saved state if {@link #beforeModify()} was called
     * at the current depth. For stacks that don't track modifications (default implementation), this behaves the same
     * as {@link #pop()}.
     *
     * @return This state stack for chaining
     */
    @SuppressWarnings("unchecked")
    default T popDepth() {
        pop();
        return (T) this;
    }

    /**
     * Called before modifying state to trigger lazy save if needed.
     * <p>
     * When using the lazy copy-on-write pattern with {@link #pushDepth()}, call this method before any state
     * modification. If the state hasn't been saved at the current depth yet, this will save it.
     * <p>
     * For implementations that don't support lazy copy-on-write, this is a no-op.
     */
    default void beforeModify() {
        // Default: no-op for stacks that don't support lazy copy-on-write
    }

    /**
     * Get the current stack depth.
     *
     * @return The current depth (0 when empty)
     */
    default int getDepth() {
        return 0; // Default for stacks that don't track depth separately
    }
}
