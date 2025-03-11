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
}
