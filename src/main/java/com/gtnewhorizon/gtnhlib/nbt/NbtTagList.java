package com.gtnewhorizon.gtnhlib.nbt;

import java.util.AbstractList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A wrapper view of a {@link NBTTagList}.
 *
 * @param <T>
 */
@UnmodifiableView
public class NbtTagList<T> extends AbstractList<T> {

    protected final NBTTagList delegate;
    protected final NBTTagListGetter<T> getter;

    @SuppressWarnings("MagicConstant")
    protected NbtTagList(NBTTagList delegate, @MagicConstant(valuesFromClass = NbtUtils.class) int typeId,
            NBTTagListGetter<T> getter) {
        this.delegate = delegate;
        this.getter = getter;
        if (this.delegate.getId() != typeId) {
            throw new IllegalArgumentException(
                    "Invalid tag list type, expected " + typeId + ", got " + delegate.getId());
        }
    }

    // @formatter:off
    // spotless:off
    public static NbtTagList<Byte>              ofByte      (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_BYTE,       NbtUtils::getByteAtList         );}
    public static NbtTagList<Short>             ofShort     (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_SHORT,      NbtUtils::getShortAtList        );}
    public static NbtTagList<Integer>           ofInt       (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_INT,        NbtUtils::getIntAtList          );}
    public static NbtTagList<Long>              ofLong      (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_LONG,       NbtUtils::getLongAtList         );}
    public static NbtTagList<Float>             ofFloat     (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_FLOAT,      NBTTagList::func_150308_e       );}
    public static NbtTagList<Double>            ofDouble    (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_DOUBLE,     NBTTagList::func_150309_d       );}
    public static NbtTagList<byte[]>            ofByteArray (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_BYTE_ARRAY, NbtUtils::getByteArrayAtList    );}
    public static NbtTagList<String>            ofString    (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_STRING,     NBTTagList::getStringTagAt      );}
    public static NbtTagList<NBTTagList>        ofList      (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_LIST,       NbtUtils::getNbtTagListAtList   );}
    public static NbtTagList<NBTTagCompound>    ofCompound  (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_COMPOUND,   NBTTagList::getCompoundTagAt    );}
    public static NbtTagList<int[]>             ofIntArray  (NBTTagList list) { return new NbtTagList<>(list, NbtUtils.TYPE_INT_ARRAY,  NBTTagList::func_150306_c       );}
    // spotless:on
    // @formatter:on

    @Override
    public T get(int index) {
        return getter.get(delegate, index);
    }

    @Override
    public int size() {
        return delegate.tagCount();
    }
}
