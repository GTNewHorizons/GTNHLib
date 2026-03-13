package com.gtnewhorizon.gtnhlib.util;

import java.lang.reflect.Method;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public interface BiIterator<E> extends ListIterator<E> {

    /**
    * Reverses the view of the iterator.
    */
    void reverse();

    /**
    * Outputs whether the iterator is reversed.
    *
    * @return true if it is reversed, false otherwise
    */
    boolean isReversed();

    /**
    * Reverses the view on the iterator.&nbsp;[Read below for impl details][#reversed()]
    * You should @Override this with the **type of your class** as the return type.
    * @see java.util.SequencedCollection#reversed()
    *
    * @return returns this (the class) for chaining.
    */
    BiIterator<E> reversed();

    /**
    * Does nothing unless defined by implementing class.
    * This is here to not force classes that just want {@link java.util.Iterator#previous()} and {@link java.util.Iterator#next()} to use it.
    * @see java.util.ListIterator#add(E)
    *
    * @param e the object to add to an underlying collection at the cursor position, if implemented
    */
    @Override
    default void add(E e) {}

    /**
    * Does nothing unless defined by implementing class.
    * This is here to not force classes that just want {@link java.util.Iterator#previous()} and {@link java.util.Iterator#next()} to use it.
    * @see java.util.ListIterator#set(E)
    *
    * @param e the object to set in an underlying collection at the cursor position, if implemented
    */
    @Override
    default void set(E e) {}

    /**
    * Does nothing unless defined by implementing class.
    * This is here to not force classes that just want {@link java.util.Iterator#previous()} and {@link java.util.Iterator#next()} to use it.
    * @see java.util.ListIterator#remove()
    */
    @Override
    default void remove() {}

    /**
    * Returns 0 unless defined by implementing class.
    * This is here to not force classes that just want {@link java.util.Iterator#previous()} and {@link java.util.Iterator#next()} to use it.
    * It is still a good idea to keep track of iterator index, and {@link IndexedIterator} uses it.
    * @see java.util.ListIterator#nextIndex()
    *
    * @return The index after the cursor if implemented, else 0
    */
    @Override
    default int nextIndex() {
        return 0;
    }

    /**
    * Returns 0 unless defined by implementing class.
    * This is here to not force classes that just want {@link java.util.Iterator#previous()} and {@link java.util.Iterator#next()} to use it.
    * It is still a good idea to keep track of iterator index, and {@link IndexedIterator} uses it.
    * @see java.util.ListIterator#nextIndex()
    *
    * @return The index before the cursor if implemented, else 0
    */
    @Override
    default int previousIndex() {
        return 0;
    }

    /**
     * Same functionality as {@link java.util.Iterator#forEachRemaining(java.util.function.Consumer)}, but supports reversing and chaining.
     *
     * @param action the consumer that is run on all future elements, in order
     * @return the instantiating class, for chaining
     */
    @Override
    default BiIterator<E> forEachNext(Consumer<? super E> action) {
        if (isReversed()) while (hasPrevious()) action.accept(previous());
        else ListIterator.super.forEachRemaining(action);
        return this;
    }

    /**
     * Same functionality as {@link #forEachNext(java.util.function.Consumer)} but works backwards.
     *
     * @param action the consumer that is run on all previous elements, in order
     * @return the instantiating class, for chaining
     */
    default BiIterator<E> forEachPrevious(Consumer<? super E> action) {
        if (isReversed()) while (hasNext()) action.accept(next());
        else while (hasPrevious()) action.accept(previous());
        return this;
    } // Probably COULD have implemented isReversed() with reflection jank. Didn't.
    // in Python, functions are variables so it's a lot easier.

    // goddamn java doesn't support private internal default methods
    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachNextUntil(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether {@link #isReversed()} should be ignored
    */
    default void __forEachNextUntil(long count, Consumer<? super E> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachPreviousUntil(-count, action, false);
        }
        else while (hasNext() && count-- > 0) action.accept(next());
    }

    /**
     * Same functionality as {@link #forEachNext(java.util.function.Consumer)}, but stops early after a certian amount of iterations.
     * Still checks {@link java.util.Iterator#hasNext()}; use {@link #forEachNextUntil_checkless(long, java.util.function.Consumer)} to not check that.
     *
     * @param count the maximum amount of iterations
     * @param action the consumer that is run on all previous elements, in order
     * @return the instantiating class, for chaining
     */
    default BiIterator<E> forEachNextUntil(long count, Consumer<? super E> action) {
        __forEachNextUntil(count, action, true);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachPreviousUntil(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether {@link #isReversed()} should be ignored
    */
    default void __forEachPreviousUntil(long count, Consumer<? super E> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachNextUntil(-count, action, false);
        }
        else while (hasPrevious() && count-- > 0) action.accept(previous());
    }

    /**
     * Same functionality as {@link #forEachPrevious(java.util.function.Consumer)}, but stops early after a certian amount of iterations.
     * Still checks {@link java.util.Iterator#hasPrevious()}; use {@link #forEachPreviousUntil_checkless(long, java.util.function.Consumer)} to not check that.
     *
     * @param count the maximum amount of iterations
     * @param action the consumer that is run on all previous elements, in order
     * @return the instantiating class, for chaining
     */
    default BiIterator<E> forEachPreviousUntil(long count, Consumer<? super E> action) {
        __forEachPreviousUntil(count, action, true);
        return this;
    }

    /**
    * Same functionality as {@link #forEachNext(java.util.function.Consumer)}, but allows the function to control when it stops.
    * Still checks {@link java.util.Iterator#hasNext()}; use {@link #forEachNextUntil_checkless(java.util.function.Function)} to not check that.
    *
    * @param action the function that accepts the remaining elements and returns `true` to keep going or `false` to stop
    * @return the instantiating class, for chaining
    */
    default BiIterator<E> forEachNextUntil(Function<? super E, Boolean> action) {
        if (isReversed()) while (hasPrevious() && action.apply(previous()));
        else while (hasNext() && action.accept(next()));
        return this;
    }

    /**
    * Same functionality as {@link #forEachPrevious(java.util.function.Consumer)}, but allows the function to control when it stops.
    * Still checks {@link java.util.Iterator#hasPrevious()}; use {@link #forEachPreviousUntil_checkless(java.util.function.Function)} to not check that.
    *
    * @param action the function that accepts the previous elements and returns `true` to keep going or `false` to stop
    * @return the instantiating class, for chaining
    */
    default BiIterator<E> forEachPreviousUntil(Function<? super E, Boolean> action) {
        if (isReversed()) while (hasNext() && action.apply(next()));
        else while (hasPrevious() && action.accept(previous()));
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachNextUntil(long, java.util.function.Function)
    * @hidden
    *
    * @param flag whether {@link #isReversed()} should be ignored
    */
    default void __forEachNextUntil(long count, Function<? super E, Boolean> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachPreviousUntil(-count, action, false);
        }
        else while (hasNext() && count-- > 0 && action.accept(next()));
    }

    default BiIterator<E> forEachNextUntil(long count, Function<? super E, Boolean> action) {
        __forEachNextUntil(count, action, true);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachPreviousUntil(long, java.util.function.Function)
    * @hidden
    *
    * @param flag whether {@link #isReversed()} should be ignored
    */

    default void __forEachPreviousUntil(long count, Function<? super E, Boolean> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachNextUntil(-count, action);
        }
        else while (hasPrevious() && count-- > 0 && action.accept(previous()));
    }

    default BiIterator<E> forEachPreviousUntil(long count, Function<? super E, Boolean> action) {
        __forEachPreviousUntil(count, action, true);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachNextUntil_checkless(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether {@link #isReversed()} should be ignored
    */

    default void __forEachNextUntil_checkless(long count, Consumer<? super E> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachPreviousUntil_checkless(-count, action, false);
        }
        else while (count-- > 0) action.accept(next());
    }

    default BiIterator<E> forEachNextUntil_checkless(long count, Consumer<? super E> action) {
        __forEachNextUntil_checkless(count, action, true);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Do not use.
    * @see #forEachPreviousUntil_checkless(long, java.util.function.Consumer)
    * @hidden
    *
    * @param flag whether {@link #isReversed()} should be ignored
    */

    default void __forEachPreviousUntil_checkless(long count, Consumer<? super E> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachNextUntil_checkless(-count, action, false);
        }
        else while (count-- > 0) action.accept(previous());
    }

    default BiIterator<E> forEachPreviousUntil_checkless(long count, Consumer<? super E> action) {
        __forEachPreviousUntil_checkless(count, action, true);
        return this;
    }

    default void forEachNextUntil_checkless(Function<? super E, Boolean> action) {
        while (action.accept(next()));
    }

    default void forEachPreviousUntil_checkless(Function<? super E, Boolean> action) {
        while (action.accept(previous()));
    }

    default void forEachNextUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachPreviousUntil(-count, action);
        else while (count-- > 0 && action.accept(next()));
    }

    default void forEachPreviousUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) forEachNextUntil(-count, action);
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
        int len = arr.length;
        for (int i = 0;;) {
            if (i == len) return arr;
            arr[i++] = next();
        }
    }

    default <T super E> T[] allPrevious(T[] arr) {
        int len = arr.length;
        for (int i = 0;;) {
            if (i == len) return arr;
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
