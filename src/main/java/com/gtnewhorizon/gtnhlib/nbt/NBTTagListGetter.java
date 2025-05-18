package com.gtnewhorizon.gtnhlib.nbt;

import net.minecraft.nbt.NBTTagList;

/**
 * An abstraction of the methods get the values from {@link NBTTagList}.
 *
 * @param <T> the desired type of the value.
 */
@FunctionalInterface
public interface NBTTagListGetter<T> {
    T get(NBTTagList list, int index);
}
