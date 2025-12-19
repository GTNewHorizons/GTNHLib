package com.gtnewhorizon.gtnhlib.chat;

import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;

public interface IChatComponentCustomSerializer {

    JsonElement serialize(IChatComponent chatComponent);

    IChatComponent deserialize(JsonElement jsonElement);

    String getID(); // Unique ID across all mods. Make it good!
}
