package com.gtnewhorizon.gtnhlib.datastructs.space;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker2D.packChunk;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gtnewhorizon.gtnhlib.functional.Consumer2D;
import com.gtnewhorizon.gtnhlib.space.MutableXZ;
import com.gtnewhorizon.gtnhlib.space.XZAddressable;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker2D;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

@SuppressWarnings("unused")
public class HashSet2D extends LongOpenHashSet {

    public boolean contains(int posX, int posZ) {
        return super.contains(packChunk(posX, posZ));
    }

    public boolean contains(XZAddressable xyz) {
        return contains(xyz.getX(), xyz.getZ());
    }

    public boolean remove(int posX, int posZ) {
        return super.remove(packChunk(posX, posZ));
    }

    public boolean remove(XZAddressable xyz) {
        return remove(xyz.getX(), xyz.getZ());
    }

    public boolean add(int posX, int posZ) {
        return super.add(packChunk(posX, posZ));
    }

    public boolean add(XZAddressable xyz) {
        return add(xyz.getX(), xyz.getZ());
    }

    public void forEach(Consumer2D consumer) {
        for (var e : this.fastEntryIterable()) {
            consumer.accept(e.getX(), e.getZ());
        }
    }

    /// The returned iterator always yields the same mutated {@link XZAddressable} instance; do not retain the
    /// result of {@link Iterator#next()} past the following call.
    public Iterator<XZAddressable> fastIterator() {
        LongIterator iter = super.iterator();

        MutableXZ pos = new MutableXZ();

        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public XZAddressable next() {
                long l = iter.nextLong();

                pos.x = CoordinatePacker2D.unpackChunkX(l);
                pos.z = CoordinatePacker2D.unpackChunkZ(l);

                return pos;
            }
        };
    }

    public Iterable<XZAddressable> fastEntryIterable() {
        return this::fastIterator;
    }

    /// The stream elements are backed by the same mutated {@link XZAddressable} instance as
    /// {@link #fastIterator()}; collect {@code new MutableXZ(e.getX(), e.getZ())} if you need to retain them.
    public Stream<XZAddressable> fastEntryStream() {
        return StreamSupport.stream(
                Spliterators.spliterator(
                        fastEntryIterable().iterator(),
                        size(),
                        Spliterator.SIZED | Spliterator.NONNULL | Spliterator.DISTINCT),
                false);
    }
}
