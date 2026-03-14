package com.gtnewhorizon.gtnhlib.util;

import java.lang.reflect.Method;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
* An Iterator that works two ways, supports reversing, and need not have a {@link java.util.Collection} attached.
* 
*/
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
    default BiIterator<E> reversed() {
        reverse();
        return this;
    }

    E __next();

    E __previous();

    boolean __hasNext();

    boolean __hasPrevious();

    default E next() {
        return isReversed() ? __previous() : __next();
    }

    default E previous() {
        return isReversed() ? __next() : __previous();
    }

    default boolean hasNext() {
        return isReversed() ? __hasPrevious() : __hasNext();
    }

    default boolean hasPrevious() {
        return isReversed() ? __hasNext() : __hasPrevious();
    }

    @Nullable("If hasNext() returns false")
    default E nextIfHasNext() {
        return isReversed() ? __hasPrevious() ? __previous() : null : __hasNext() ? __next() : null;
    }

    @Nullable("If hasPrevious() returns false")
    default E previousIfHasPrevious() {
        return isReversed() ? __hasNext() ? __next() : null : __hasPrevious() ? __previous() : null;
    }

    default E nextIfHasNextOrLast() {
        return isReversed() ? __hasPrevious() ? __previous() : __peekNext() : __hasNext() ? __next() : __peekPrevious();
    }

    default E previousIfHasPreviousOrLast() {
        return isReversed() ? __hasNext() ? __next() : __peekPrevious() : __hasPrevious() ? __previous() : __peekNext();
    }

    @Nullable("If both hasNext() and hasPrevious() return false")
    default E nextIfHasNextOrLastIfHasLast() {
        return isReversed() ? __hasPrevious() ? __previous() : __hasNext() ? __peekNext() : null : __hasNext() ? __next() : __hasPrevious() ? __peekPrevious() : null;
    }

    @Nullable("If both hasNext() and hasPrevious() return false")
    default E previousIfHasPreviousOrLastIfHasLast() {
        return isReversed() ? __hasNext() ? __next() : __hasPrevious() ? __peekPrevious() : null : __hasPrevious() ? __previous() : __hasNext() ? __peekNext() : null;
    }

    default E __peekPrevious() {
        E ret = __previous();
        __next();
        return ret;
    }

    default E peekPrevious() {
        return isReversed() ? __peekNext() : __peekPrevious();
    }

    default E __peekNext() {
        E ret = __next();
        __previous();
        return ret;
    }

    /**
    * Does nothing unless defined by implementing class.
    * This is here to not force classes that just want {@link #previous()} and {@link #next()} to use it.
    * @see java.util.ListIterator#add(E)
    *
    * @param e the object to add to an underlying collection at the cursor position, if implemented
    */
    @Override
    default void add(E e) {}

    /**
    * Does nothing unless defined by implementing class.
    * This is here to not force classes that just want {@link #previous()} and {@link #next()} to use it.
    * @see java.util.ListIterator#set(E)
    *
    * @param e the object to set in an underlying collection at the cursor position, if implemented
    */
    @Override
    default void set(E e) {}

    /**
    * Does nothing unless defined by implementing class.
    * This is here to not force classes that just want {@link #previous()} and {@link #next()} to use it.
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
    * Overridden to support reversing.&nbsp;Use {@link #forEachNext(java.util.function.Consumer)} instead.
    * This is just here for compatability's sake; "Next" is used instead of "Remaining" in other methods.
    * @see #forEachNext(java.util.function.consumer)
    *
    * @param action the consumer that is run on all future elements, in order
    */
    @Override
    default void forEachRemaining(Consumer<? super E> action) {
        forEachNext(action);
    }

    /**
     * Same functionality as {@link java.util.Iterator#forEachRemaining(java.util.function.Consumer)}, but supports reversing and chaining.
     *
     * @param action the consumer that is run on all future elements, in order
     * @return the instantiating class, for chaining
     */
    @Override
    default BiIterator<E> forEachNext(Consumer<? super E> action) {
        if (isReversed()) while (__hasPrevious()) action.accept(__previous());
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
        if (isReversed()) while (__hasNext()) action.accept(__next());
        else while (__hasPrevious()) action.accept(__previous());
        return this;
    } // Probably COULD have implemented isReversed() with reflection jank. Didn't.
    // in Python, functions are variables so it's a lot easier.

    // goddamn java doesn't support private internal default methods
    /**
    * Internal method call for the reversing implementation.&nbsp;Use only in implementing class.
    * Ignores {@link #isReversed()}
    * @see #forEachNextUntil(long, java.util.function.Consumer)
    *
    * <!-- @param flag whether {@link #isReversed()} and `count`'s sign should be ignored -->
    */
    default void __forEachNextUntil(long count, Consumer<? super E> action) {
        if (count < 0) __forEachPreviousUntil(-count, action);
        else while (__hasNext() && count-- > 0) action.accept(__next());
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
        __forEachNextUntil(isReversed() ? -count : count, action);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Use only in implementing class.
    * Ignores {@link #isReversed()}
    * @see #forEachPreviousUntil(long, java.util.function.Consumer)
    *
    * <!-- @param flag whether {@link #isReversed()} and `count`'s sign should be ignored -->
    */
    default void __forEachPreviousUntil(long count, Consumer<? super E> action) {
        if (count < 0) __forEachNextUntil(-count, action);
        else while (__hasPrevious() && count-- > 0) action.accept(__previous());
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
        __forEachPreviousUntil(isReversed() ? -count : count, action);
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
        if (isReversed()) while (__hasPrevious() && action.apply(__previous()));
        else while (__hasNext() && action.accept(__next()));
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
        if (isReversed()) while (__hasNext() && action.apply(__next()));
        else while (__hasPrevious() && action.accept(__previous()));
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Use only in implementing class.
    * Ignores {@link #isReversed()}
    * @see #forEachNextUntil(long, java.util.function.Function)
    *
    * <!-- @param flag whether {@link #isReversed()} and `count`'s sign should be ignored -->
    */
    default void __forEachNextUntil(long count, Function<? super E, Boolean> action) {
        if (count < 0) __forEachPreviousUntil(-count, action);
        else while (__hasNext() && count-- > 0 && action.accept(__next()));
    }

    default BiIterator<E> forEachNextUntil(long count, Function<? super E, Boolean> action) {
        __forEachNextUntil(isReversed() ? -count : count, action);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Use only in implementing class.
    * Ignores {@link #isReversed()}
    * @see #forEachPreviousUntil(long, java.util.function.Function)
    *
    * <!-- @param flag whether {@link #isReversed()} and `count`'s sign should be ignored -->
    */

    default void __forEachPreviousUntil(long count, Function<? super E, Boolean> action) {
        if (count < 0) __forEachNextUntil(-count, action);
        else while (__hasPrevious() && count-- > 0 && action.accept(__previous()));
    }

    default BiIterator<E> forEachPreviousUntil(long count, Function<? super E, Boolean> action) {
        __forEachPreviousUntil(isReversed() ? -count : count, action);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Use only in implementing class.
    * Ignores {@link #isReversed()}
    * @see #forEachNextUntil_checkless(long, java.util.function.Consumer)
    *
    * <!-- @param flag whether {@link #isReversed()} and `count`'s sign should be ignored -->
    */

    default void __forEachNextUntil_checkless(long count, Consumer<? super E> action) {
        if (count < 0) __forEachPreviousUntil_checkless(-count, action);
        else while (count-- > 0) action.accept(__next());
    }

    default BiIterator<E> forEachNextUntil_checkless(long count, Consumer<? super E> action) {
        __forEachNextUntil_checkless(isReversed() ? -count : count, action);
        return this;
    }

    /**
    * Internal method call for the reversing implementation.&nbsp;Use only in implementing class.
    * Ignores {@link #isReversed()}
    * @see #forEachPreviousUntil_checkless(long, java.util.function.Consumer)
    *
    * <!-- @param flag whether {@link #isReversed()} and `count`'s sign should be ignored -->
    */

    default void __forEachPreviousUntil_checkless(long count, Consumer<? super E> action, boolean flag) {
        if (flag) {
            if (isReversed()) count = -count;
            if (count < 0) __forEachNextUntil_checkless(-count, action, false);
        }
        else while (count-- > 0) action.accept(__previous());
    }

    default BiIterator<E> forEachPreviousUntil_checkless(long count, Consumer<? super E> action) {
        __forEachPreviousUntil_checkless(isReversed() ? -count : count, action);
        return this;
    }

    default BiIterator<E> forEachNextUntil_checkless(Function<? super E, Boolean> action) {
        if (isReversed()) while (action.accept(__previous()));
        else while (action.accept(__next()));
        return this;
    }

    default BiIterator<E> forEachPreviousUntil_checkless(Function<? super E, Boolean> action) {
        if (isReversed()) while (action.accept(__next()));
        else while (action.accept(__previous()));
        return this;
    }

    default void __forEachNextUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) __forEachPreviousUntil_checkless(-count, action);
        else while (count-- > 0 && action.accept(__next()));
    }

    default BiIterator<E> forEachNextUntil_checkless(long count, Function<? super E, Boolean> action) {
        __forEachNextUntil_checkless(isReversed() ? -count : count, action);
        return this;
    }

    default void __forEachPreviousUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) __forEachNextUntil_checkless(-count, action);
        else while (count-- > 0 && action.accept(__previous()));
    }

    default BiIterator<E> forEachPreviousUntil_checkless(long count, Function<? super E, Boolean> action) {
        __forEachPreviousUntil_checkless(isReversed() ? -count : count, action);
        return this;
    }

    @Nullable("If 0 is passed")
    default E next(long count) {
        if (isReversed()) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0) return cur;
            cur = __previous();
        } else for (;;) {
            if (count-- <= 0) return cur;
            cur = __next();
        }
    }

    @Nullable("If 0 is passed")
    default E previous(long count) {
        if (isReversed()) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0) return cur;
            cur = __next();
        } else for (;;) {
            if (count-- <= 0) return cur;
            cur = __previous();
        }
    }

    default E nextRemaining(long count) {
        if (count < 0) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0 || !__hasNext()) return cur;
            cur = __previous();
        } else for (;;) {
            if (count-- <= 0 || !__hasPrevious()) return cur;
            cur = __next();
        }
    }

    default E previousRemaining(long count) {
        if (count < 0) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0 || !__hasNext()) return cur;
            cur = __next();
        } else for (;;) {
            if (count-- <= 0 || !__hasPrevious()) return cur;
            cur = __previous();
        }
    }

    default E[] __allNext() {
        //int len = 32;
        int leng = 32;//len;
        E[] arr = new E[leng];
        E[] arr2;
        int i = 0;
        hackyGoto: {
            for (byte step = 1;;++step) {
                while (i<leng) {
                    if (!__hasNext()) break hackyGoto;
                    arr[++i] = __next();
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

    default E[] allNext() {
        return isReversed() ? __allPrevious() : __allNext();
    }

    default E[] allPrevious() {
        return isReversed() ? __allNext() : __allPrevious();
    }

    default E[] __allPrevious() {
        //int len = 32;
        int leng = 32;//len;
        E[] arr = new E[leng];
        E[] arr2;
        int i = 0;
        hackyGoto: {
            for (byte step = 1;;++step) {
                while (i<leng) {
                    if (!__hasPrevious()) break hackyGoto;
                    arr[++i] = __previous();
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

    default void checkArrayValid(long length) {
        if (length > Integer.MAX_VALUE - 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",length));

    default E[] allNext(long count) {
        if (isReversed()) count = -count;
        if (count < 0) {
            checkArrayValid(-count);
            E[] arr = new E[-count];
            for (int i = 0;;) {
                E[-i--] = __previous();
                if (i == count) return arr;
            }
        } else {
            checkArrayValid(count);
            E[] arr = new E[count];
            for (int i = 0;;) {
                if (i == count) return arr;
                E[i++] = __next();
            }
        }
    }

    default E[] allPrevious(long count) {
        if (isReversed()) count = -count;
        if (count < 0) {
            checkArrayValid(-count);//if (count <= Integer.MIN_VALUE + 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",-count));
            E[] arr = new E[-count];
            for (int i = 0;;) {
                E[-i--] = __next();
                if (i == count) return arr;
            }
        } else {
            checkArrayValid(count);//if (count > Integer.MAX_VALUE - 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",count));
            E[] arr = new E[count];
            for (int i = 0;;) {
                if (i == count) return arr;
                E[i++] = __previous();
            }
        }
    }

    default E[] allNextRemaining(long count) {
        if (isReversed()) count = -count;
        E[] arr;
        int i = 0;
        if (count < 0) {
            checkArrayValid
            arr = E[-count];
            while (__hasPrevious()) {
                arr[-i--] = __previous();
                if (i == count) return arr;
            }} else {
            arr = E[count];
            while (__hasNext()) {
                if (i == count) return arr;
                arr[i++] = __next();
            }
        }
        i = Math.abs(i);
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default E[] allPreviousRemaining(long count) {
        if (isReversed()) count = -count;
        E[] arr = E[Math.abs(count)];
        int i = 0;
        if (count < 0) while (__hasNext()) {
            arr[i++] = __next();
            if (-i == count) return arr;
        } else while (__hasPrevious()) {
            if (i == count) return arr;
            arr[i++] = __previous();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default <T super E> T[] allNext(T[] arr) {
        int len = arr.length;
        for (int i = 0;;) {
            if (i == len) return arr;
            arr[i++] = __next();
        }
    }

    default <T super E> T[] allPrevious(T[] arr) {
        int len = arr.length;
        for (int i = 0;;) {
            if (i == len) return arr;
            arr[i++] = __previous();
        }
    }

    default E[] allNextRemaining(long count) {
        if (count < 0) return allPreviousRemaining(-count);
        E[] arr = new E[count];
        int i = 0;
        while (__hasNext()) {
            if (i == count) return arr;
            arr[i++] = __next();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default E[] allPreviousRemaining(long count) {
        if (count < 0) return allNextRemaining(-count);
        E[] arr = new E[count];
        int i = 0;
        while (__hasPrevious()) {
            if (i == count) return arr;
            arr[i++] = __next();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

}
