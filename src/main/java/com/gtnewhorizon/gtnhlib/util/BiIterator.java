package com.gtnewhorizon.gtnhlib.util;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public interface BiIterator<E> extends ListIterator<E> {

    void reverse();

    boolean isReversed();

    BiIterator<E> reversed();

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
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (hasNext() && count-- > 0) action.accept(next());
    }

    default void forEachPreviousUntil(long count, Consumer<? super E> action) {
        if (count < 0) forEachRemainingUntil(-count, action);
        else while (hasPrevious() && count-- > 0) action.accept(previous());
    }

    default void forEachRemainingUntil(Function<? super E, Boolean> action) {
        while (hasNext() && action.accept(next()));
    }

    default void forEachPreviousUntil(Function<? super E, Boolean> action) {
        while (hasPrevious() && action.accept(previous()));
    }

    default void forEachRemainingUntil(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (hasNext() && count-- > 0 && action.accept(next()));
    }

    default void forEachPreviousUntil(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachRemainingUntil(-count, action);
        else while (hasPrevious() && count-- > 0 && action.accept(previous()));
    }

    default void forEachRemainingUntil_checkless(long count, Consumer<? super E> action) {
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (count-- > 0) action.accept(next());
    }

    default void forEachPreviousUntil_checkless(long count, Consumer<? super E> action) {
        if (count < 0) forEachRemainingUntil(-count, action);
        else while (count-- > 0) action.accept(previous());
    }

    default void forEachRemainingUntil_checkless(Function<? super E, Boolean> action) {
        while (action.accept(next()));
    }

    default void forEachPreviousUntil_checkless(Function<? super E, Boolean> action) {
        while (action.accept(previous()));
    }

    default void forEachRemainingUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (count-- > 0 && action.accept(next()));
    }

    default void forEachPreviousUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachRemainingUntil(-count, action);
        else while (count-- > 0 && action.accept(previous()));
    }

    default E next(long count) {
        if (count < 0) return previous(-count);
        E cur = null;
        for (;;) {
            if (count-- <= 0) return cur;
            cur = next();
        }
    }

    default E previous(long count) {
        if (count < 0) return next(-count);
        E cur = null;
        for (;;) {
            if (count-- <= 0) return cur;
            cur = previous();
        }
    }

    default E nextRemaining(long count) {
        if (count < 0) return previousRemaining(-count);
        E cur = null;
        for (;;) {
            if (count-- <= 0 || !hasNext()) return cur;
            cur = next();
        }
    }

    default E previousRemaining(long count) {
        if (count < 0) return nextRemaining(-count);
        E cur = null;
        for (;;) {
            if (count-- <= 0 || !hasPrevious()) return cur;
            cur = previous();
        }
    }

    default E[] allNext() {
        int len = 32;
        E[] arr;
        E[] arr2;
        int i = 0;
    }

    default E[] allPrevious() {}

    default E[] allNext(long count) {
        if (count < 0) return allPrevious(-count);
        if (count > Integer.MAX_VALUE - 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",count));
        E[] arr = new E[count];
        for (int i = 0;;) {
            if (i == count) return arr;
            E[i++] = next();
        }
    }

    default E[] allPrevious(long count) {
        if (count < 0) return allNext(-count);
        if (count > Integer.MAX_VALUE - 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",count));
        E[] arr = new E[count];
        for (int i = 0;;) {
            if (i == count) return arr;
            E[i++] = previous();
        }
    }

    default E[] allNextRemaining(long count) {
        if (count < 0) return allPreviousRemaining(-count);
        E[] arr = new E[count];
        int i = 0;
        while (hasNext()) {
            if (i == count) return arr;
            arr[i++] = next();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default E[] allPreviousRemaining(long count) {
        if (count < 0) return allNextRemaining(-count);
        E[] arr = new E[count];
        int i = 0;
        while (hasPrevious()) {
            if (i == count) return arr;
            arr[i++] = next();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default <T super E> T[] allNext(T[] arr) {
        for (int i = 0;;) {
            if (i == arr.length) return arr;
            arr[i++] = next();
        }
    }

    default <T super E> T[] allPrevious(T[] arr) {
        for (int i = 0;;) {
            if (i == arr.length) return arr;
            arr[i++] = previous();
        }
    }

    default E[] allNextRemaining(long count) {
        if (count < 0) return allPreviousRemaining(-count);
        E[] arr = new E[count];
        int i = 0;
        while (hasNext()) {
            if (i == count) return arr;
            arr[i++] = next();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default E[] allPreviousRemaining(long count) {
        if (count < 0) return allNextRemaining(-count);
        E[] arr = new E[count];
        int i = 0;
        while (hasPrevious()) {
            if (i == count) return arr;
            arr[i++] = next();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

}
