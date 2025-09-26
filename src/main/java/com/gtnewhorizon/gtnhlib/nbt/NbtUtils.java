package com.gtnewhorizon.gtnhlib.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NbtUtils {

    // @formatter:off
    // spotless:off
    public static final int TYPE_BYTE       = 1;
    public static final int TYPE_SHORT      = 2;
    public static final int TYPE_INT        = 3;
    public static final int TYPE_LONG       = 4;
    public static final int TYPE_FLOAT      = 5;
    public static final int TYPE_DOUBLE     = 6;
    public static final int TYPE_BYTE_ARRAY = 7;
    public static final int TYPE_STRING     = 8;
    public static final int TYPE_LIST       = 9;
    public static final int TYPE_COMPOUND   = 10;
    public static final int TYPE_INT_ARRAY  = 11;
    // spotless:on
    // @formatter:on

    /**
     * Gets the internal map of the compound.
     *
     * @param tag the compound
     * @return the internal map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, NBTBase> getRawMap(@NotNull NBTTagCompound tag) {
        return tag.tagMap;
    }

    /**
     * Gets the internal list of the nbt list.
     *
     * @param list the nbt list
     * @return the internal list.
     */
    @SuppressWarnings("unchecked")
    public static List<NBTBase> getRawList(@NotNull NBTTagList list) {
        return list.tagList;
    }

    /**
     * Gets the nbt tag list from the given compound, while the type of the list is not checked.
     *
     * @return the tag list or {@code null} if it doesn't exist or not a list.
     */
    @NotNull
    public static NBTTagList getNbtTagListUnchecked(@NotNull NBTTagCompound tag, @NotNull String tagName) {
        NBTBase nbtTagList = getRawMap(tag).get(tagName);
        if (nbtTagList == null || nbtTagList.getId() != TYPE_LIST) {
            return new NBTTagList();
        }
        return (NBTTagList) nbtTagList;
    }

    /**
     * Create a nbt list with values that are transformed by the encoder.
     *
     * @param values  the values
     * @param encoder the encoder
     * @param <T>     the type of values
     * @return the new created nbt list
     */
    @NotNull
    public static <T> NBTTagList encodeToList(@NotNull Collection<T> values,
            @NotNull Function<T, ? extends NBTBase> encoder) {
        NBTTagList list = new NBTTagList();
        for (T value : values) {
            list.appendTag(encoder.apply(value));
        }
        return list;
    }

    /**
     * Return a list with entries in the nbt list via the getter.
     *
     * @param list   the nbt list
     * @param getter the entry getter
     * @param <T>    the type of values
     * @return the list with transformed entries
     * @throws ClassCastException when the type is not valid.
     * @see NBTTagListGetter
     */
    @NotNull
    public static <T> List<T> decodeFromList(@NotNull NBTTagList list, @NotNull NBTTagListGetter<T> getter)
            throws ClassCastException {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            T value = getter.get(list, i);
            result.add(value);
        }
        return result;
    }

    /**
     * Set the encoded list to the compound with given tag name.
     *
     * @see #encodeToList(Collection, Function)
     */
    public static <T> void writeList(@NotNull NBTTagCompound nbt, @NotNull String tagName, @NotNull List<T> values,
            @NotNull Function<T, ? extends NBTBase> func) {
        nbt.setTag(tagName, encodeToList(values, func));
    }

    /**
     * Read the decoded list from the compound with given tag name, while the type is not checked.
     *
     * @see #decodeFromList(NBTTagList, NBTTagListGetter)
     */
    @NotNull
    public static <T> List<T> readList(@NotNull NBTTagCompound nbt, @NotNull String tagName,
            @NotNull NBTTagListGetter<T> getter) {
        NBTTagList tagList = getNbtTagListUnchecked(nbt, tagName);
        return decodeFromList(tagList, getter);
    }

    /**
     * Read the decoded list from the compound with given tag name.
     *
     * @see #decodeFromList(NBTTagList, NBTTagListGetter)
     */
    @NotNull
    public static <T> List<T> readList(@NotNull NBTTagCompound nbt, @NotNull String tagName,
            @MagicConstant(valuesFromClass = NbtUtils.class) int type, @NotNull NBTTagListGetter<T> getter) {
        NBTTagList tagList = nbt.getTagList(tagName, type);
        return decodeFromList(tagList, getter);
    }

    /**
     * Gets the existing compound or create it.
     *
     * @param nbt     the parent compound
     * @param tagName the tag name
     * @return the compound
     */
    @NotNull
    public static NBTTagCompound getOrCreateCompound(@NotNull NBTTagCompound nbt, @NotNull String tagName) {
        if (!nbt.hasKey(tagName)) {
            nbt.setTag(tagName, new NBTTagCompound());
        }
        return nbt.getCompoundTag(tagName);
    }

    /**
     * Gets the existing list or create it.
     * 
     * @param nbt     the compound
     * @param tagName the tag name
     * @param type    the type of the values
     * @return the list
     */
    @NotNull
    public static NBTTagList getOrCreateList(@NotNull NBTTagCompound nbt, @NotNull String tagName, int type) {
        if (!nbt.hasKey(tagName)) {
            nbt.setTag(tagName, new NBTTagList());
            // make type to 0, because the type of an new empty list is 0.
            // otherwise, we won't get the correctly object ref because the types are mismatching.
            type = 0;
        }
        return nbt.getTagList(tagName, type);
    }

    @Nullable
    private static NBTBase getElementAtList(@NotNull NBTTagList list, int index) {
        if (index < 0 || index >= list.tagCount()) {
            return getRawList(list).get(index);
        }
        return null;
    }

    public static byte getByteAtList(@NotNull NBTTagList list, int index) throws ClassCastException {
        NBTBase e = getElementAtList(list, index);
        return e != null && e.getId() == TYPE_BYTE ? ((NBTTagByte) e).func_150290_f() : 0;
    }

    public static short getShortAtList(@NotNull NBTTagList list, int index) throws ClassCastException {
        NBTBase e = getElementAtList(list, index);
        return e != null && e.getId() == TYPE_SHORT ? ((NBTTagShort) e).func_150289_e() : 0;
    }

    public static int getIntAtList(@NotNull NBTTagList list, int index) throws ClassCastException {
        NBTBase e = getElementAtList(list, index);
        return e != null && e.getId() == TYPE_INT ? ((NBTTagInt) e).func_150287_d() : 0;
    }

    public static long getLongAtList(@NotNull NBTTagList list, int index) throws ClassCastException {
        NBTBase e = getElementAtList(list, index);
        return e != null && e.getId() == TYPE_LONG ? ((NBTTagLong) e).func_150291_c() : 0;
    }

    public static byte[] getByteArrayAtList(@NotNull NBTTagList list, int index) throws ClassCastException {
        NBTBase e = getElementAtList(list, index);
        return e != null && e.getId() == TYPE_BYTE_ARRAY ? ((NBTTagByteArray) e).func_150292_c() : new byte[0];
    }

    public static NBTTagList getNbtTagListAtList(@NotNull NBTTagList list, int index) throws ClassCastException {
        NBTBase e = getElementAtList(list, index);
        return e != null && e.getId() == TYPE_LIST ? (NBTTagList) e : null;
    }
}
