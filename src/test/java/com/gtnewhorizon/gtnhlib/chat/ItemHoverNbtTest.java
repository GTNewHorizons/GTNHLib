package com.gtnewhorizon.gtnhlib.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class ItemHoverNbtTest {

    @Test
    void preservesTagTypesAndContents() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByteArray("bytes", new byte[] { -128, 0, 127 });
        nbt.setIntArray("ints", new int[] { Integer.MIN_VALUE, 42 });
        nbt.setString("key:with,delimiters", "quoted \"text\" with \\");

        assertEquals(nbt, ItemHoverNbt.decode(ItemHoverNbt.format(nbt)));
    }

    @Test
    void leavesLegacyHoverTextToVanilla() {
        assertFalse(ItemHoverNbt.isEncoded("{ints:[1,2],Count:1b}"));
    }

    @Test
    void rejectsMalformedBinaryHoverText() {
        assertNull(ItemHoverNbt.decode("gtnhlib:nbt1:!"));
        assertNull(ItemHoverNbt.decode("gtnhlib:nbt1:AAAA"));
    }
}
