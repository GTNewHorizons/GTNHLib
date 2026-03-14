package com.gtnewhorizon.gtnhlib.util;

//import java.lang.reflect.Method;
import java.util.Collection;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
* An Iterator that works two ways, supports reversing, and needs not have a {@link java.util.Collection} attached.
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

    void setProgressionSeq(@Nullable BiIterator<Long> iter);

    BiIterator<Long> getProgressionSeq();

    default BiIterator<E> withProgressionSeq(@Nullable BiIterator<Long> iter) {
        setProgressionSeq(iter);
        return this;
    }

    E __next();

    E __previous();

    E __hasNext():

    E __hasPrevious();

    default E _next() {
        long i;
        BiIterator<long> seq = getProgressionSeq();
        if (seq == null || (i = seq.next()) == 1) return __next();
        if (i == 0)  {
            E ret = __previous();
            __next();
            return ret;
        }

    default E _previous();

    default boolean _hasNext();

    default boolean _hasPrevious();

    default E next() {
        return isReversed() ? _previous() : _next();
    }

    default E previous() {
        return isReversed() ? _next() : _previous();
    }

    default boolean hasNext() {
        return isReversed() ? _hasPrevious() : _hasNext();
    }

    default boolean hasPrevious() {
        return isReversed() ? _hasNext() : _hasPrevious();
    }

    @Nullable("If hasNext() returns false")
    default E nextIfHasNext() {
        return isReversed() ? _hasPrevious() ? _previous() : null : _hasNext() ? _next() : null;
    }

    @Nullable("If hasPrevious() returns false")
    default E previousIfHasPrevious() {
        return isReversed() ? _hasNext() ? _next() : null : _hasPrevious() ? _previous() : null;
    }

    default E nextIfHasNextOrLast() {
        return isReversed() ? _hasPrevious() ? _previous() : __peekNext() : _hasNext() ? _next() : __peekPrevious();
    }

    default E previousIfHasPreviousOrLast() {
        return isReversed() ? _hasNext() ? _next() : __peekPrevious() : _hasPrevious() ? _previous() : __peekNext();
    }

    @Nullable("If both hasNext() and hasPrevious() return false")
    default E nextIfHasNextOrLastIfHasLast() {
        return isReversed() ? _hasPrevious() ? _previous() : _hasNext() ? __peekNext() : null : _hasNext() ? _next() : _hasPrevious() ? __peekPrevious() : null;
    }

    @Nullable("If both hasNext() and hasPrevious() return false")
    default E previousIfHasPreviousOrLastIfHasLast() {
        return isReversed() ? _hasNext() ? _next() : _hasPrevious() ? __peekPrevious() : null : _hasPrevious() ? _previous() : _hasNext() ? __peekNext() : null;
    }

    default E __peekPrevious() {
        E ret = _previous();
        _next();
        return ret;
    }

    default E peekPrevious() {
        return isReversed() ? __peekNext() : __peekPrevious();
    }

    default E __peekNext() {
        E ret = _next();
        _previous();
        return ret;
    }

    default E peekNext() {
        return isReversed() ? __peekPrevious() : __peekNext();
    }

    default boolean __hasNext(long count) {
        long iters = 0;
        boolean ret = true;
        for (;iters++<count;) {
            if (!_hasNext()) ret = false; break;
            _next();
        } while (iters-- > 0)  _previous();
        return ret;
    }

    default boolean __hasPrevious(long count) {
        long iters = 0;
        boolean ret = true;
        for (;iters++<count;) {
            if (!_hasPrevious()) ret = false; break;
            _previous();
        } while (iters-- > 0)  _next();
        return ret;
    }

    default boolean hasNext(long count) {
        if (isReversed()) count = -count;
        return count < 0 ? __hasPrevious() : __hasNext();
    }

    default boolean hasPrevious(long count) {
        if (isReversed()) count = -count;
        return count < 0 ? __hasNext() : __hasPrevious();
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
        if (isReversed()) while (_hasPrevious()) action.accept(_previous());
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
        if (isReversed()) while (_hasNext()) action.accept(_next());
        else while (_hasPrevious()) action.accept(_previous());
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
        else while (_hasNext() && count-- > 0) action.accept(_next());
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
        else while (_hasPrevious() && count-- > 0) action.accept(_previous());
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
        if (isReversed()) while (_hasPrevious() && action.apply(_previous()));
        else while (_hasNext() && action.accept(_next()));
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
        if (isReversed()) while (_hasNext() && action.apply(_next()));
        else while (_hasPrevious() && action.accept(_previous()));
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
        else while (_hasNext() && count-- > 0 && action.accept(_next()));
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
        else while (_hasPrevious() && count-- > 0 && action.accept(_previous()));
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
        else while (count-- > 0) action.accept(_next());
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
        else while (count-- > 0) action.accept(_previous());
    }

    default BiIterator<E> forEachPreviousUntil_checkless(long count, Consumer<? super E> action) {
        __forEachPreviousUntil_checkless(isReversed() ? -count : count, action);
        return this;
    }

    default BiIterator<E> forEachNextUntil_checkless(Function<? super E, Boolean> action) {
        if (isReversed()) while (action.accept(_previous()));
        else while (action.accept(_next()));
        return this;
    }

    default BiIterator<E> forEachPreviousUntil_checkless(Function<? super E, Boolean> action) {
        if (isReversed()) while (action.accept(_next()));
        else while (action.accept(_previous()));
        return this;
    }

    default void __forEachNextUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) __forEachPreviousUntil_checkless(-count, action);
        else while (count-- > 0 && action.accept(_next()));
    }

    default BiIterator<E> forEachNextUntil_checkless(long count, Function<? super E, Boolean> action) {
        __forEachNextUntil_checkless(isReversed() ? -count : count, action);
        return this;
    }

    default void __forEachPreviousUntil_checkless(long count, Function<? super E, Boolean> action) {
        if (count < 0) __forEachNextUntil_checkless(-count, action);
        else while (count-- > 0 && action.accept(_previous()));
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
            cur = _previous();
        } else for (;;) {
            if (count-- <= 0) return cur;
            cur = _next();
        }
    }

    @Nullable("If 0 is passed")
    default E previous(long count) {
        if (isReversed()) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0) return cur;
            cur = _next();
        } else for (;;) {
            if (count-- <= 0) return cur;
            cur = _previous();
        }
    }

    default E nextRemaining(long count) {
        if (count < 0) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0 || !_hasNext()) return cur;
            cur = _previous();
        } else for (;;) {
            if (count-- <= 0 || !_hasPrevious()) return cur;
            cur = _next();
        }
    }

    default E previousRemaining(long count) {
        if (count < 0) count = -count;
        E cur = null;
        if (count < 0) for (;;) {
            if (count++ >= 0 || !_hasNext()) return cur;
            cur = _next();
        } else for (;;) {
            if (count-- <= 0 || !_hasPrevious()) return cur;
            cur = _previous();
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
                    if (!_hasNext()) break hackyGoto;
                    arr[++i] = _next();
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
                    if (!_hasPrevious()) break hackyGoto;
                    arr[++i] = _previous();
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

    default void __checkArrayValid(long length) {
        if (length > Integer.MAX_VALUE - 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",length));

    default E[] allNext(long count) {
        if (isReversed()) count = -count;
        if (count < 0) {
            __checkArrayValid(-count);
            E[] arr = new E[-count];
            for (int i = 0;;) {
                E[-i--] = _previous();
                if (i == count) return arr;
            }
        } else {
            __checkArrayValid(count);
            E[] arr = new E[count];
            for (int i = 0;;) {
                if (i == count) return arr;
                E[i++] = _next();
            }
        }
    }

    default E[] allPrevious(long count) {
        if (isReversed()) count = -count;
        if (count < 0) {
            __checkArrayValid(-count);//if (count <= Integer.MIN_VALUE + 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",-count));
            E[] arr = new E[-count];
            for (int i = 0;;) {
                E[-i--] = _next();
                if (i == count) return arr;
            }
        } else {
            __checkArrayValid(count);//if (count > Integer.MAX_VALUE - 8) throw new ArrayIndexOutOfBoundsException(String.format("Standard java array size maximum is Integer.MAX_VALUE - 8 (Given %d)",count));
            E[] arr = new E[count];
            for (int i = 0;;) {
                if (i == count) return arr;
                E[i++] = _previous();
            }
        }
    }

    default E[] allNextRemaining(long count) {
        if (isReversed()) count = -count;
        E[] arr;
        int i = 0;
        if (count < 0) {
            __checkArrayValid(-count);
            arr = E[-count];
            while (_hasPrevious()) {
                arr[-i--] = _previous();
                if (i == count) return arr;
            }} else {
            __checkArrayValid(count);
            arr = E[count];
            while (_hasNext()) {
                if (i == count) return arr;
                arr[i++] = _next();
            }
        }
        i = Math.abs(i);
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default E[] allPreviousRemaining(long count) {
        if (isReversed()) count = -count;
        __checkArrayValid(Math.abs(count));
        E[] arr = E[Math.abs(count)];
        int i = 0;
        if (count < 0) while (_hasNext()) {
            arr[i++] = _next();
            if (-i == count) return arr;
        } else while (_hasPrevious()) {
            if (i == count) return arr;
            arr[i++] = _previous();
        }
        E[] arr2 = new E[i];
        System.arrayCopy(arr, 0, arr2, 0, i);
        return arr2;
    }

    default void __checkArrBounds(int start, int length, long count) {
        checkArrayValid(length);
        if (start <= length) throw new ArrayIndexOutOfBoundsException(String.format("Array start index %d out of bounds fo length %d", start, length));
        if (start + count > length) throw new ArrayIndexOutOfBoundsException(String.format("Array end index %d out of bounds fo length %d", start + count - 1, length));
        checkArrayValid(start + count);
    }

    default <T> T[] allNext(int start, T[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default <T> T[] allPrevious(int start, T[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default <T> T[] allNext(int start, T[] arr) {
        return allNext(start, arr, arr.length - start);
    } default <T> T[] allPrevious(int start, T[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default <T> T[] allNext(T[] arr, long count) {
        return allNext(0, arr, count);
    } default <T> T[] allPrevious(T[] arr, long count) {
        return allPrevious(0, arr, count);
    } default <T> T[] allNext(T[] arr) {
        return allNext(0, arr, arr.length);
    } default <T> T[] allPrevious(T[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default int[] allNext(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default int[] allPrevious(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default int[] allNext(int start, int[] arr) {
        return allNext(start, arr, arr.length - start);
    } default int[] allPrevious(int start, int[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default int[] allNext(int[] arr, long count) {
        return allNext(0, arr, count);
    } default int[] allPrevious(int[] arr, long count) {
        return allPrevious(0, arr, count);
    } default int[] allNext(int[] arr) {
        return allNext(0, arr, arr.length);
    } default int[] allPrevious(int[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default long[] allNext(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default long[] allPrevious(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default long[] allNext(int start, long[] arr) {
        return allNext(start, arr, arr.length - start);
    } default long[] allPrevious(int start, long[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default long[] allNext(long[] arr, long count) {
        return allNext(0, arr, count);
    } default long[] allPrevious(long[] arr, long count) {
        return allPrevious(0, arr, count);
    } default long[] allNext(long[] arr) {
        return allNext(0, arr, arr.length);
    } default long[] allPrevious(long[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default short[] allNext(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default short[] allPrevious(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default short[] allNext(int start, short[] arr) {
        return allNext(start, arr, arr.length - start);
    } default short[] allPrevious(int start, short[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default short[] allNext(short[] arr, long count) {
        return allNext(0, arr, count);
    } default short[] allPrevious(short[] arr, long count) {
        return allPrevious(0, arr, count);
    } default short[] allNext(short[] arr) {
        return allNext(0, arr, arr.length);
    } default short[] allPrevious(short[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default char[] allNext(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default char[] allPrevious(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default char[] allNext(int start, char[] arr) {
        return allNext(start, arr, arr.length - start);
    } default char[] allPrevious(int start, char[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default char[] allNext(char[] arr, long count) {
        return allNext(0, arr, count);
    } default char[] allPrevious(char[] arr, long count) {
        return allPrevious(0, arr, count);
    } default char[] allNext(char[] arr) {
        return allNext(0, arr, arr.length);
    } default char[] allPrevious(char[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default byte[] allNext(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default byte[] allPrevious(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default byte[] allNext(int start, byte[] arr) {
        return allNext(start, arr, arr.length - start);
    } default byte[] allPrevious(int start, byte[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default byte[] allNext(byte[] arr, long count) {
        return allNext(0, arr, count);
    } default byte[] allPrevious(byte[] arr, long count) {
        return allPrevious(0, arr, count);
    } default byte[] allNext(byte[] arr) {
        return allNext(0, arr, arr.length);
    } default byte[] allPrevious(byte[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default boolean[] allNext(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default boolean[] allPrevious(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default boolean[] allNext(int start, boolean[] arr) {
        return allNext(start, arr, arr.length - start);
    } default boolean[] allPrevious(int start, boolean[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default boolean[] allNext(boolean[] arr, long count) {
        return allNext(0, arr, count);
    } default boolean[] allPrevious(boolean[] arr, long count) {
        return allPrevious(0, arr, count);
    } default boolean[] allNext(boolean[] arr) {
        return allNext(0, arr, arr.length);
    } default boolean[] allPrevious(boolean[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default float[] allNext(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default float[] allPrevious(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default float[] allNext(int start, float[] arr) {
        return allNext(start, arr, arr.length - start);
    } default float[] allPrevious(int start, float[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default float[] allNext(float[] arr, long count) {
        return allNext(0, arr, count);
    } default float[] allPrevious(float[] arr, long count) {
        return allPrevious(0, arr, count);
    } default float[] allNext(float[] arr) {
        return allNext(0, arr, arr.length);
    } default float[] allPrevious(float[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default double[] allNext(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default double[] allPrevious(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default double[] allNext(int start, double[] arr) {
        return allNext(start, arr, arr.length - start);
    } default double[] allPrevious(int start, double[] arr) {
        return allPrevious(start, arr, arr.length - start);
    } default double[] allNext(double[] arr, long count) {
        return allNext(0, arr, count);
    } default double[] allPrevious(double[] arr, long count) {
        return allPrevious(0, arr, count);
    } default double[] allNext(double[] arr) {
        return allNext(0, arr, arr.length);
    } default double[] allPrevious(double[] arr) {
        return allPrevious(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, Object[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, Object[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, Object[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, Object[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(Object[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(Object[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(Object[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(Object[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, int[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, int[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(int[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(int[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(int[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(int[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, long[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, long[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(long[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(long[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(long[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(long[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, short[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, short[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(short[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(short[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(short[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(short[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, char[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, char[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(char[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(char[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(char[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(char[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, byte[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, byte[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(byte[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(byte[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(byte[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(byte[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, boolean[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, boolean[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(boolean[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(boolean[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(boolean[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(boolean[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, float[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, float[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(float[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(float[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(float[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(float[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default BiIterator<E> allNextC(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousC(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextC(int start, double[] arr) {
        return allNextC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousC(int start, double[] arr) {
        return allPreviousC(start, arr, arr.length - start);
    } default BiIterator<E> allNextC(double[] arr, long count) {
        return allNextC(0, arr, count);
    } default BiIterator<E> allPreviousC(double[] arr, long count) {
        return allPreviousC(0, arr, count);
    } default BiIterator<E> allNextC(double[] arr) {
        return allNextC(0, arr, arr.length);
    } default BiIterator<E> allPreviousC(double[] arr) {
        return allPreviousC(0, arr, arr.length);
    }

    default <T> T[] allNextRemaining(int start, T[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default <T> T[] allPreviousRemaining(int start, T[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default <T> T[] allNextRemaining(int start, T[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default <T> T[] allPreviousRemaining(int start, T[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default <T> T[] allNextRemaining(T[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default <T> T[] allPreviousRemaining(T[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default <T> T[] allNextRemaining(T[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default <T> T[] allPreviousRemaining(T[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default int[] allNextRemaining(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default int[] allPreviousRemaining(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default int[] allNextRemaining(int start, int[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default int[] allPreviousRemaining(int start, int[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default int[] allNextRemaining(int[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default int[] allPreviousRemaining(int[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default int[] allNextRemaining(int[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default int[] allPreviousRemaining(int[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default long[] allNextRemaining(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default long[] allPreviousRemaining(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default long[] allNextRemaining(int start, long[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default long[] allPreviousRemaining(int start, long[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default long[] allNextRemaining(long[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default long[] allPreviousRemaining(long[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default long[] allNextRemaining(long[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default long[] allPreviousRemaining(long[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default short[] allNextRemaining(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default short[] allPreviousRemaining(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default short[] allNextRemaining(int start, short[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default short[] allPreviousRemaining(int start, short[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default short[] allNextRemaining(short[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default short[] allPreviousRemaining(short[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default short[] allNextRemaining(short[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default short[] allPreviousRemaining(short[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default char[] allNextRemaining(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default char[] allPreviousRemaining(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default char[] allNextRemaining(int start, char[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default char[] allPreviousRemaining(int start, char[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default char[] allNextRemaining(char[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default char[] allPreviousRemaining(char[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default char[] allNextRemaining(char[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default char[] allPreviousRemaining(char[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default byte[] allNextRemaining(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default byte[] allPreviousRemaining(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default byte[] allNextRemaining(int start, byte[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default byte[] allPreviousRemaining(int start, byte[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default byte[] allNextRemaining(byte[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default byte[] allPreviousRemaining(byte[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default byte[] allNextRemaining(byte[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default byte[] allPreviousRemaining(byte[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default boolean[] allNextRemaining(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default boolean[] allPreviousRemaining(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default boolean[] allNextRemaining(int start, boolean[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default boolean[] allPreviousRemaining(int start, boolean[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default boolean[] allNextRemaining(boolean[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default boolean[] allPreviousRemaining(boolean[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default boolean[] allNextRemaining(boolean[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default boolean[] allPreviousRemaining(boolean[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default float[] allNextRemaining(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default float[] allPreviousRemaining(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default float[] allNextRemaining(int start, float[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default float[] allPreviousRemaining(int start, float[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default float[] allNextRemaining(float[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default float[] allPreviousRemaining(float[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default float[] allNextRemaining(float[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default float[] allPreviousRemaining(float[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default double[] allNextRemaining(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        }
    }

    default double[] allPreviousRemaining(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count) return arr;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count) return arr;
            arr[start++] = _previous();
        }
    }

    default double[] allNextRemaining(int start, double[] arr) {
        return allNextRemaining(start, arr, arr.length - start);
    } default double[] allPreviousRemaining(int start, double[] arr) {
        return allPreviousRemaining(start, arr, arr.length - start);
    } default double[] allNextRemaining(double[] arr, long count) {
        return allNextRemaining(0, arr, count);
    } default double[] allPreviousRemaining(double[] arr, long count) {
        return allPreviousRemaining(0, arr, count);
    } default double[] allNextRemaining(double[] arr) {
        return allNextRemaining(0, arr, arr.length);
    } default double[] allPreviousRemaining(double[] arr) {
        return allPreviousRemaining(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, Object[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, Object[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, Object[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, Object[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(Object[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(Object[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(Object[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(Object[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, int[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, int[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, int[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(int[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(int[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(int[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(int[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, long[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, long[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, long[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(long[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(long[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(long[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(long[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, short[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, short[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, short[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(short[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(short[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(short[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(short[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, char[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, char[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, char[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(char[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(char[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(char[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(char[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, byte[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, byte[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, byte[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(byte[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(byte[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(byte[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(byte[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, boolean[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, boolean[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, boolean[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(boolean[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(boolean[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(boolean[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(boolean[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, float[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, float[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, float[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(float[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(float[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(float[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(float[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

    default BiIterator<E> allNextRemainingC(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        } else for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        }
    }

    default BiIterator<E> allPreviousRemainingC(int start, double[] arr, long count) {
        __checkArrBounds(start, arr.length, Math.abs(count));
        if (isReversed()) count = -count;
        if (count < 0) count = -count; for (;;) {
            if (start == count || !_hasNext()) return this;
            arr[start++] = _next();
        } else for (;;) {
            if (start == count || !_hasPrevious()) return this;
            arr[start++] = _previous();
        }
    }

    default BiIterator<E> allNextRemainingC(int start, double[] arr) {
        return allNextRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allPreviousRemainingC(int start, double[] arr) {
        return allPreviousRemainingC(start, arr, arr.length - start);
    } default BiIterator<E> allNextRemainingC(double[] arr, long count) {
        return allNextRemainingC(0, arr, count);
    } default BiIterator<E> allPreviousRemainingC(double[] arr, long count) {
        return allPreviousRemainingC(0, arr, count);
    } default BiIterator<E> allNextRemainingC(double[] arr) {
        return allNextRemainingC(0, arr, arr.length);
    } default BiIterator<E> allPreviousRemainingC(double[] arr) {
        return allPreviousRemainingC(0, arr, arr.length);
    }

}
