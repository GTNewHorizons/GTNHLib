package com.gtnewhorizon.gtnhlib.util;

import java.lang.reflect.Method;
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
     * Identical to Iterator.forEachRemaining, but supports reversing.
     *
     * @param action the action that is run on all future elements, in order
     */

    @Override
    default void forEachRemaining(Consumer<? super E> action) {
        if (isReversed()) while (hasPrevious()) action.accept(previous());
        else ListIterator.super.forEachRemaining(action);
    }

    /**
     * Identical to Iterator.forEachRemaining but works backwards.
     *
     * @param action the action that is run on all previous elements, in order
     */
    default void forEachPrevious(Consumer<? super E> action) {
        if (isReversed()) while (hasNext()) action.accept(next());
        else while (hasPrevious()) action.accept(previous());
    } // Probably COULD have implemented isReversed() with reflection jank. Didn't.
    // in Python, functions are variables so it's a lot easier.

    // goddamn java doesn't support private internal default methods
    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachRemainingUntil(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether isReversed() should be ignored
    */
    default void forEachRemainingUntil(long count, Consumer<? super E> action, boolean flag) {
        if (isReversed()) count = -count;
        if (flag && count < 0) forEachPreviousUntil(-count, action, false);
        else while (hasNext() && count-- > 0) action.accept(next());
    }

    /**
     * Same functionality as forEachRemaining, but stops early after a certian amount of iterations.
     * Still checks hasNext().
     *
     * @param count the maximum amount of iterations
     * @param action the 
     */
    default void forEachRemainingUntil(long count, Consumer<? super E> action) {
        forEachRemainingUntil(count, action, true);
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachPreviousUntil(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether isReversed() should be ignored
    */

    default void forEachPreviousUntil(long count, Consumer<? super E> action, boolean flag) {
        if (isReversed()) count = -count;
        if (flag && count < 0) forEachRemainingUntil(-count, action, false);
        else while (hasPrevious() && count-- > 0) action.accept(previous());
    }

    default void forEachPreviousUntil(long count, Consumer<? super E> action) {
        forEachPreviousUntil(count, action, true);
    }

    default void forEachRemainingUntil(Function<? super E, Boolean> action) {
        if (isReversed()) while (hasPrevious() && action.accept(previous()));
        else while (hasNext() && action.accept(next()));
    }

    default void forEachPreviousUntil(Function<? super E, Boolean> action) {
        while (hasPrevious() && action.accept(previous()));
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachRemainingUntil(long, java.util.function.Function)
    * @hidden
    *
    * @param flag whether isReversed() should be ignored
    */

    default void forEachRemainingUntil(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (hasNext() && count-- > 0 && action.accept(next()));
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachPreviousUntil(long, java.util.function.Function)
    * @hidden
    *
    * @param flag whether isReversed() should be ignored
    */

    default void forEachPreviousUntil(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachRemainingUntil(-count, action);
        else while (hasPrevious() && count-- > 0 && action.accept(previous()));
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachRemainingUntil_checkless(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether isReversed() should be ignored
    */

    default void forEachRemainingUntil_checkless(long count, Consumer<? super E> action) {
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (count-- > 0) action.accept(next());
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachPreviousUntil_checkless(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether isReversed() should be ignored
    */

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
        //int len = 32;
        int leng = 32;//len;
        E[] arr = new E[leng];
        E[] arr2;
        int i = 0;
        hackyGoto: {
            for (byte step = 1;;++step) {
                while (i<leng) {
                    if (!hasNext()) break hackyGoto;
                    arr[++i] = next();
                }
                if (step>108) break;
                //if (step%4==0) len *= 2;
                arr2 = arr;
                leng = step != 108 ? (32<<step/4)+step%4*(8<<step/4) /*len+step%4*len/4*/ : Integer.MAX_VALUE - 8;
                arr = new E[leng];
                System.arrayCopy(arr2,0,arr,0,leng);
            }
            return arr;
        }
        arr2 = new E[i];
        System.arrayCopy(arr,0,arr2,0,i);
        return arr2;
    }

    default E[] allPrevious() {
        //int len = 32;
        int leng = 32;//len;
        E[] arr = new E[leng];
        E[] arr2;
        int i = 0;
        hackyGoto: {
            for (byte step = 1;;++step) {
                while (i<leng) {
                    if (!hasPrevious()) break hackyGoto;
                    arr[++i] = previous();
                }
                if (step>108) break;
                //if (step%4==0) len *= 2;
                arr2 = arr;
                leng = step != 108 ? (32<<step/4)+step%4*(8<<step/4) /*len+step%4*len/4*/ : Integer.MAX_VALUE - 8;
                arr = new E[leng];
                System.arrayCopy(arr2,0,arr,0,leng);
            }
            return arr;
        }
        arr2 = new E[i];
        System.arrayCopy(arr,0,arr2,0,i);
        return arr2;
    }

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
