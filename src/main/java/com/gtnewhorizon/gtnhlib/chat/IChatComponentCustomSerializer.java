package com.gtnewhorizon.gtnhlib.chat;

import com.google.gson.JsonElement;

public interface IChatComponentCustomSerializer {

    JsonElement serialize();

    void deserialize(JsonElement jsonElement);

    String getID(); // Unique ID across all mods. Make it good!
}
