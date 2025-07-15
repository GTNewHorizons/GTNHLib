package com.gtnewhorizon.gtnhlib.nbt;

import net.minecraft.nbt.NBTTagList;

/**
 * An abstraction of the methods get the values from {@link NBTTagList}.
 *
 * @param <T> the desired type of the value.
 */
@FunctionalInterface
public interface NBTTagListGetter<T> {

    /**
     * Read the value from the {@link NBTTagList}.
     *
     * @param list  the list
     * @param index the index
     * @return the value
     * @throws ClassCastException when the type is not valid.
     */
    T get(NBTTagList list, int index) throws ClassCastException;
}
