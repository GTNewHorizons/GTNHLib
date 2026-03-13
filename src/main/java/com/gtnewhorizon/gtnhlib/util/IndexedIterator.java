package com.gtnewhorizon.gtnhlib.util;

public abstract class IndexedIterator<E> extends BiIterator<E> {

    long getStartIndex();

    long index();

    boolean isLowerBoundKnown();

    boolean isUpperBoundKnown();

    long getUpperBound();

    long getLowerBound();

    default boolean isLengthKnown() {
        return isLowerBoundKnown() && isUpperBoundKnown();
    }

    default long getLength() {
        return getUpperBound() - getLowerBound();
    }

    default boolean hasNext(long count) {
        if (isUpperBoundKnown()) return getUpperBound() - count > index();
        // The code below is only for compatability's sake. Avoid calling this w/o known upper bound.
        long i = 0;
        for(;hasNext() && i < count;++i) next();
        boolean ret = i == count;
	    for(;i > 0;i--) previous();
        return ret;
    }

    default boolean hasPrevious(long count) {
        if (isLowerBoundKnown()) return getLowerBound() + count < index();
        // The code below is only for compatability's sake. Avoid calling this w/o known lower bound.
        long i = 0;
        for(;hasPrevious() && i < count;++i) previous();
        boolean ret = i == count;
	    for(;i > 0;i--) next();
        return ret;
    }

    default long nextIndex() {
        return index();
    }

    default long previousIndex() {
        return index() - 1;
    }

    @Override
    default int nextIndex() {
        long index = index();
        if (index == (int) index) return (int) index;
        throw new ClassCastException("Iterator uses full long bounds while the inherited int nextIndex() was called.");
    }

    @Override
    default int previousIndex() {
        long index = index() - 1;
        if (index == (int) index) return (int) index;
        throw new ClassCastException("Iterator uses full long bounds while the inherited int previousIndex() was called.");
    }

    //default E next(long count)

    //E[] next

    //E nextIfHasNext

    //etc.

}
