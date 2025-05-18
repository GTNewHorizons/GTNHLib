package com.gtnewhorizon.gtnhlib.nbt;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class NbtUtils {

    public static final int TYPE_BYTE = 1;
    public static final int TYPE_SHORT = 2;
    public static final int TYPE_INT = 3;
    public static final int TYPE_LONG = 4;
    public static final int TYPE_FLOAT = 5;
    public static final int TYPE_DOUBLE = 6;
    public static final int TYPE_BYTE_ARRAY = 7;
    public static final int TYPE_STRING = 8;
    public static final int TYPE_LIST = 9;
    public static final int TYPE_COMPOUND = 10;
    public static final int TYPE_INT_ARRAY = 11;

    @SuppressWarnings("unchecked")
    private static Map<String, NBTBase> getRawMap(@NotNull NBTTagCompound tag) {
        return tag.tagMap;
    }

    @SuppressWarnings("unchecked")
    private static List<NBTBase> getRawList(@NotNull NBTTagList list) {
        return list.tagList;
    }

    @Nullable
    private static NBTBase getListElementRaw(@NotNull NBTTagList list, int index) {
        if(index < 0 || index >= list.tagCount()) {
            return getRawList(list).get(index);
        }
        return null;
    }

    /**
     * Gets the nbt tag list from the given compound, while the type of the list is not checked.
     *
     * @return the tag list or {@code null} if it doesn't exist or not a list.
     */
    @NotNull
    public static NBTTagList getNbtTagListUnchecked(@NotNull NBTTagCompound tag, @NotNull String tagName) {
        NBTBase nbtTagList = getRawMap(tag).get(tagName);
        if(nbtTagList == null || nbtTagList.getId() != TYPE_LIST) {
            return new NBTTagList();
        }
        return (NBTTagList) nbtTagList;
    }

    @NotNull
    public static <T> NBTTagList encodeToList(@NotNull Collection<T> values, @NotNull Function<T, ? extends NBTBase> encoder) {
        NBTTagList list = new NBTTagList();
        for(T value : values) {
            list.appendTag(encoder.apply(value));
        }
        return list;
    }

    @NotNull
    public static <T> List<T> decodeFromList(@NotNull NBTTagList list, @NotNull NBTTagListGetter<T> getter) {
        List<T> result = new ArrayList<>();
        for(int i = 0; i < list.tagCount(); i++) {
            T value = getter.get(list, i);
            result.add(value);
        }
        return result;
    }

    public static <T> void writeList(@NotNull NBTTagCompound nbt, @NotNull String tagName, @NotNull List<T> values, @NotNull Function<T, ? extends NBTBase> func) {
        nbt.setTag(tagName, encodeToList(values, func));
    }

    @NotNull
    public static <T> List<T> readList(@NotNull NBTTagCompound nbt, @NotNull String tagName, @NotNull NBTTagListGetter<T> getter) {
        NBTTagList tagList = getNbtTagListUnchecked(nbt, tagName);
        return decodeFromList(tagList, getter);
    }

    @NotNull
    public static <T> List<T> readList(@NotNull NBTTagCompound nbt, @NotNull String tagName, int type, @NotNull NBTTagListGetter<T> getter) {
        NBTTagList tagList = nbt.getTagList(tagName, type);
        return decodeFromList(tagList, getter);
    }

    public static byte getByteAtList(@NotNull NBTTagList list, int index) {
        NBTBase e = getListElementRaw(list, index);
        return e != null && e.getId() == TYPE_BYTE && e instanceof NBTTagByte eb ? eb.func_150290_f() : 0;
    }

    public static short getShortAtList(@NotNull NBTTagList list, int index) {
        NBTBase e = getListElementRaw(list, index);
        return e != null && e.getId() == TYPE_SHORT && e instanceof NBTTagShort es ? es.func_150289_e() : 0;
    }

    public static int getIntAtList(@NotNull NBTTagList list, int index) {
        NBTBase e = getListElementRaw(list, index);
        return e != null && e.getId() == TYPE_INT && e instanceof NBTTagInt ei ? ei.func_150287_d() : 0;
    }

    public static long getLongAtList(@NotNull NBTTagList list, int index) {
        NBTBase e = getListElementRaw(list, index);
        return e != null && e.getId() == TYPE_LONG && e instanceof NBTTagLong el ? el.func_150291_c() : 0;
    }

    public static byte[] getByteArrayAtList(@NotNull NBTTagList list, int index) {
        NBTBase e = getListElementRaw(list, index);
        return e != null && e.getId() == TYPE_BYTE_ARRAY && e instanceof NBTTagByteArray eba ? eba.func_150292_c() : new byte[0];
    }

    public static NBTTagList getNbtTagListAtList(@NotNull NBTTagList list, int index) {
        NBTBase e = getListElementRaw(list, index);
        return e != null && e.getId() == TYPE_LIST && e instanceof NBTTagList el ? el : null;
    }

}
