package com.gtnewhorizon.gtnhlib.datastructs.space;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gtnewhorizon.gtnhlib.functional.Consumer3D;
import com.gtnewhorizon.gtnhlib.space.MutableXYZ;
import com.gtnewhorizon.gtnhlib.space.XYZAddressable;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

@SuppressWarnings("unused")
public class HashSet3D extends LongOpenHashSet {

    public boolean contains(int posX, int posY, int posZ) {
        return super.contains(pack(posX, posY, posZ));
    }

    public boolean contains(XYZAddressable xyz) {
        return contains(xyz.getX(), xyz.getY(), xyz.getZ());
    }

    public boolean remove(int posX, int posY, int posZ) {
        return super.remove(pack(posX, posY, posZ));
    }

    public boolean remove(XYZAddressable xyz) {
        return remove(xyz.getX(), xyz.getY(), xyz.getZ());
    }

    public boolean add(int posX, int posY, int posZ) {
        return super.add(pack(posX, posY, posZ));
    }

    public boolean add(XYZAddressable xyz) {
        return add(xyz.getX(), xyz.getY(), xyz.getZ());
    }

    public void forEach(Consumer3D consumer) {
        for (var e : this.fastEntryIterable()) {
            consumer.accept(e.getX(), e.getY(), e.getZ());
        }
    }

    /// The returned iterator always yields the same mutated {@link XYZAddressable} instance; do not retain the
    /// result of {@link Iterator#next()} past the following call.
    public Iterator<XYZAddressable> fastIterator() {
        LongIterator iter = super.iterator();

        MutableXYZ pos = new MutableXYZ();

        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public XYZAddressable next() {
                long l = iter.nextLong();

                pos.x = CoordinatePacker.unpackX(l);
                pos.y = CoordinatePacker.unpackY(l);
                pos.z = CoordinatePacker.unpackZ(l);

                return pos;
            }
        };
    }

    public Iterable<XYZAddressable> fastEntryIterable() {
        return this::fastIterator;
    }

    /// The stream elements are backed by the same mutated {@link XYZAddressable} instance as
    /// {@link #fastIterator()}; collect {@code new MutableXYZ(e.getX(), e.getY(), e.getZ())} if you need to retain
    /// them.
    public Stream<XYZAddressable> fastEntryStream() {
        return StreamSupport.stream(
                Spliterators.spliterator(
                        fastEntryIterable().iterator(),
                        size(),
                        Spliterator.SIZED | Spliterator.NONNULL | Spliterator.DISTINCT),
                false);
    }
}
