package com.gtnewhorizon.gtnhlib.test.nbt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.nbt.NbtUtils;

public class NbtUtilsTest {

    @Test
    public void testEncodeToList() {
        List<String> values = Arrays.asList("Hello", "World", "!");
        NBTTagList result = NbtUtils.encodeToList(values, NBTTagString::new);
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i < result.tagCount(); i++) {
            resultList.add(result.getStringTagAt(i));
        }
        Assertions.assertEquals(values, resultList);
    }

    @Test
    public void testDecodeFromList() {
        List<String> values = Arrays.asList("Hello", "World", "!");
        NBTTagList list = new NBTTagList();
        values.forEach(e -> list.appendTag(new NBTTagString(e)));
        List<String> result = NbtUtils.decodeFromList(list, NBTTagList::getStringTagAt);
        Assertions.assertEquals(values, result);
    }

    @Test
    public void testWriteList() {
        String key = "list";
        List<String> values = Arrays.asList("Hello", "World", "!");

        NBTTagCompound compound = new NBTTagCompound();
        NbtUtils.writeList(compound, key, values, NBTTagString::new);

        Assertions.assertEquals(values.size(), compound.getTagList(key, NbtUtils.TYPE_STRING).tagCount());
    }

    @Test
    public void testReadList1() {
        String key = "list";
        List<String> values = Arrays.asList("Hello", "World", "!");

        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        values.forEach(e -> list.appendTag(new NBTTagString(e)));
        compound.setTag(key, list);

        List<String> result = NbtUtils.readList(compound, key, NBTTagList::getStringTagAt);
        Assertions.assertEquals(values, result);
    }

    @Test
    public void testReadList2() {
        String key = "list";
        List<String> values = Arrays.asList("Hello", "World", "!");

        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        values.forEach(e -> list.appendTag(new NBTTagString(e)));
        compound.setTag(key, list);

        List<String> result = NbtUtils.readList(compound, key, NbtUtils.TYPE_STRING, NBTTagList::getStringTagAt);
        List<String> resultEmpty = NbtUtils
                .readList(compound, key, NbtUtils.TYPE_BYTE_ARRAY, NBTTagList::getStringTagAt);

        Assertions.assertEquals(values, result);
        Assertions.assertEquals(new ArrayList<>(), resultEmpty);
    }

}
