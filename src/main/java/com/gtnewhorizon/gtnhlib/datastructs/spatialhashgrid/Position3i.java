package com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid;

@FunctionalInterface
public interface Position3i<T> {

    Int3 getPosition(T obj);
}
