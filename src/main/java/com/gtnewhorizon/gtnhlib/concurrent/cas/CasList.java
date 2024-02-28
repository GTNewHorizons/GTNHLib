package com.gtnewhorizon.gtnhlib.concurrent.cas;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * A fast-to-read, slow-to-modify concurrent ArrayList adapter, see {@link CasAdapter} for details on the
 * implementation. Use {@link CasList#read()} and {@link CasList#mutate} if you need to perform any sequence of
 * operations, otherwise each method call will operate on a different snapshot of the state of the list.
 *
 * @param <T> List element type
 */
@SuppressWarnings("unused") // API type
public class CasList<T> extends CasAdapter<ObjectImmutableList<T>, ObjectArrayList<T>> implements List<T> {

    /** Constructs an empty list */
    public CasList() {
        super(ObjectImmutableList.of());
    }

    /** Constructs a list from the given elements, or an empty list if null */
    @SafeVarargs
    public CasList(final @Nullable T... initialElements) {
        super(initialElements == null ? ObjectImmutableList.of() : new ObjectImmutableList<>(initialElements));
    }

    /** Constructs a list from the given elements, or an empty list if null */
    public CasList(final @Nullable Collection<T> initialElements) {
        super(initialElements == null ? ObjectImmutableList.of() : new ObjectImmutableList<>(initialElements));
    }

    /** Constructs a list from the given elements, or an empty list if null */
    public CasList(final @Nullable ObjectIterator<T> initialElements) {
        super(initialElements == null ? ObjectImmutableList.of() : new ObjectImmutableList<>(initialElements));
    }

    /** Constructs a list populated in the given lambda */
    public CasList(final @NotNull Consumer<@NotNull ObjectArrayList<T>> constructor) {
        this();
        final ObjectArrayList<T> elems = new ObjectArrayList<>();
        constructor.accept(elems);
        overwrite(immutableCopyOf(elems));
    }

    @Override
    public final @NotNull ObjectArrayList<T> mutableCopyOf(@NotNull ObjectImmutableList<T> data) {
        return new ObjectArrayList<>(data);
    }

    @Override
    public final @NotNull ObjectImmutableList<T> immutableCopyOf(@NotNull ObjectArrayList<T> data) {
        return new ObjectImmutableList<>(data);
    }

    /**
     * @deprecated Basically useless as it could be modified after being checked, iterate over a stored reference from
     *             {@link CasList#read()} instead.
     */
    @Override
    @Deprecated
    public int size() {
        return read().size();
    }

    @Override
    public boolean isEmpty() {
        return read().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return read().contains(o);
    }

    /**
     * Note: the returned iterator will not see new/removed elements from after this method is called. The iterator is
     * immutable.
     */
    @NotNull
    @Override
    public Iterator<T> iterator() {
        return read().iterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return read().toArray();
    }

    @NotNull
    @Override
    public <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
        return read().toArray(a);
    }

    @Override
    public boolean add(T t) {
        mutate(al -> al.add(t));
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return mutate(al -> al.remove(o));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return read().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return mutate(al -> al.addAll(c));
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        return mutate(al -> al.addAll(index, c));
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return mutate(al -> al.removeAll(c));
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return mutate(al -> al.retainAll(c));
    }

    @Override
    public void clear() {
        mutate(al -> {
            al.clear();
            return null;
        });
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public T get(int index) {
        return read().get(index);
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public T set(int index, T element) {
        return mutate(al -> al.set(index, element));
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public void add(int index, T element) {
        mutate(al -> {
            al.add(index, element);
            return null;
        });
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public T remove(int index) {
        return mutate(al -> al.remove(index));
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public int indexOf(Object o) {
        return read().indexOf(o);
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @Override
    @Deprecated
    public int lastIndexOf(Object o) {
        return read().lastIndexOf(o);
    }

    /**
     * Note: the returned iterator will not see new/removed elements from after this method is called. The iterator is
     * immutable.
     */
    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return read().listIterator();
    }

    /**
     * Note: the returned iterator will not see new/removed elements from after this method is called. The iterator is
     * immutable.
     */
    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return read().listIterator(index);
    }

    /**
     * @deprecated Indices are not safe to use across different method calls, use {@link CasList#mutate} or a cached
     *             {@link CasList#read} view instead.
     */
    @NotNull
    @Override
    @Deprecated
    public ObjectList<T> subList(int fromIndex, int toIndex) {
        return read().subList(fromIndex, toIndex);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        mutate(al -> {
            al.replaceAll(operator);
            return null;
        });
    }

    @Override
    public void sort(Comparator<? super T> c) {
        mutate(al -> {
            al.sort(c);
            return null;
        });
    }

    /**
     * Note: the returned iterator will not see new/removed elements from after this method is called. The iterator is
     * immutable.
     */
    @Override
    public Spliterator<T> spliterator() {
        return read().spliterator();
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return mutate(al -> al.removeIf(filter));
    }

    /**
     * Note: the returned iterator will not see new/removed elements from after this method is called. The iterator is
     * immutable.
     */
    @Override
    public Stream<T> stream() {
        return read().stream();
    }

    /**
     * Note: the returned iterator will not see new/removed elements from after this method is called. The iterator is
     * immutable.
     */
    @Override
    public Stream<T> parallelStream() {
        return read().parallelStream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        read().forEach(action);
    }

    @Override
    public int hashCode() {
        return read().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof List)) {
            return false;
        }
        final ObjectImmutableList<T> myList = read();
        final List<?> otherList = (obj instanceof CasList) ? ((CasList<?>) obj).read() : (List<?>) obj;
        return myList.equals(otherList);
    }

    @Override
    public String toString() {
        return read().toString();
    }
}
