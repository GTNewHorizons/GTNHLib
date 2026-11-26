package com.gtnewhorizon.gtnhlib.chat;

import java.io.IOException;
import java.util.Base64;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

public final class ItemHoverNbt {

    private static final String PREFIX = "gtnhlib:nbt1:";
    private static final int MAX_NBT_SIZE = 2 * 1024 * 1024;

    private ItemHoverNbt() {}

    public static String format(NBTTagCompound nbt) {
        try {
            return PREFIX + Base64.getEncoder().encodeToString(CompressedStreamTools.compress(nbt));
        } catch (IOException e) {
            return nbt.toString();
        }
    }

    public static boolean isEncoded(String text) {
        return text.startsWith(PREFIX);
    }

    public static NBTTagCompound decode(String text) {
        try {
            byte[] data = Base64.getDecoder().decode(text.substring(PREFIX.length()));
            return CompressedStreamTools.func_152457_a(data, new NBTSizeTracker(MAX_NBT_SIZE));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
