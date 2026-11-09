package com.gtnewhorizon.gtnhlib.codec.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagString;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;

class StringRepresentableTest {

    private enum Phase implements StringRepresentable {

        NEW_MOON("new_moon"),
        FULL_MOON("full_moon");

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private enum Clash implements StringRepresentable {

        A,
        B;

        @Override
        public String getSerializedName() {
            return "same";
        }
    }

    @Test
    void enumsAreSavedByNameAndNamesMustBeKnownAndUnique() {
        StringRepresentable.EnumCodec<Phase> codec = StringRepresentable.fromEnum(Phase::values);
        assertEquals(
                new JsonPrimitive("full_moon"),
                codec.encodeStart(JsonOps.INSTANCE, Phase.FULL_MOON).getOrThrow(IllegalStateException::new));
        assertEquals(
                Phase.NEW_MOON,
                codec.parse(NbtOps.INSTANCE, new NBTTagString("new_moon")).getOrThrow(IllegalStateException::new));
        assertTrue(codec.parse(JsonOps.INSTANCE, new JsonPrimitive("FULL_MOON")).isError());
        assertThrows(IllegalStateException.class, () -> StringRepresentable.fromEnum(Clash::values));
    }
}
