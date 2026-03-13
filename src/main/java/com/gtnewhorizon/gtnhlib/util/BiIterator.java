package com.gtnewhorizon.gtnhlib.util;

import java.util.ListIterator;
import java.util.function.Consumer;

public interface BiIterator<E> extends ListIterator<E> {

    @Override
    default void add(E e) {}

    @Override
    default void set(E e) {}

    @Override
    default void remove() {}

    @Override
    default int nextIndex() {
        return null;
    }

    @Override
    default int previousIndex() {
        return null;
    }

    /**
     * Identical to Iterator.forEachNext but works backwards
     *
     * @param action the action that is run on all previous elements, in order
     */
    default void forEachPrevious(Consumer<? super E> action) {
        while (hasPrevious()) action.accept(previous());
    }

    default void forEachRemainingUntil(long count, Consumer<? super E> action) {
        while (hasNext() && count-- > 0) action.accept(next());
    }

    default void forEachPreviousUntil(long count, Consumer<? super E> action) {
        while (hasPrevious() && count-- > 0) action.accept(previous());
    }

}
