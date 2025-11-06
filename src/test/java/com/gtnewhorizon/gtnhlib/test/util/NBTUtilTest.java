package com.gtnewhorizon.gtnhlib.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.gtnewhorizon.gtnhlib.util.NBTUtil;

public class NBTUtilTest {

    private static class Data {
        public byte a;
        public short b;
        public int c;
        public long d;
        public float e;
        public double f;
        public String g;
        public int[] h;
        public byte[] i;

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Data data)) return false;

            return a == data.a
                && b == data.b
                && c == data.c
                && d == data.d
                && Math.abs(e - data.e) < 0.0001
                && Math.abs(f - data.f) < 0.0001
                && Objects.equals(g, data.g)
                && Arrays.equals(h, data.h)
                && Arrays.equals(i, data.i);
        }

        @Override
        public String toString() {
            return "Data{"
                + "a="
                + a
                + ", b="
                + b
                + ", c="
                + c
                + ", d="
                + d
                + ", e="
                + e
                + ", f="
                + f
                + ", g='"
                + g
                + '\''
                + ", h="
                + Arrays.toString(h)
                + ", i="
                + Arrays.toString(i)
                + '}';
        }
    }

    @Test
    void testNBTGson() {
        Data data = new Data();

        data.a = (byte) 0xF0;
        data.b = (short) 0xF0F0;
        data.c = 0xF0F0F0F0;
        data.d = 0xF0F0F0F0F0F0F0F0L;
        data.e = 123.456f;
        data.f = 123.456;
        data.g = "hello world";
        data.h = new int[] { 1, 2, 3, 4, 5 };
        data.i = new byte[] { 1, 2, 3, 4, 5 };

        Gson gson = new Gson();

        NBTTagCompound tag = NBTUtil.toNbt(gson.toJsonTree(data));

        Data data2 = gson.fromJson(NBTUtil.toJsonObject(tag), Data.class);

        assertEquals(data, data2);
    }

    @Test
    void testNBTExact() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("a", (byte) 0xF0);
        tag.setShort("b", (short) 0xF0F0);
        tag.setInteger("c", 0xF0F0F0F0);
        tag.setLong("d", 0xF0F0F0F0F0F0F0F0L);
        tag.setFloat("e", 123.456f);
        tag.setDouble("f", 123.456);
        tag.setString("g", "hello world");
        tag.setIntArray("h", new int[] { 1, 2, 3, 4, 5 });
        tag.setByteArray("i", new byte[] { 1, 2, 3, 4, 5 });

        NBTTagList list = new NBTTagList();
        list.appendTag(tag);

        assertEquals(list, NBTUtil.toNbtExact(NBTUtil.toJsonObjectExact(list)));
    }
}
