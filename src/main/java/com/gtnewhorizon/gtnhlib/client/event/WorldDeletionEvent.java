package com.gtnewhorizon.gtnhlib.client.event;

import cpw.mods.fml.common.eventhandler.Event;

public final class WorldDeletionEvent extends Event {

    // Not a full path, name of just the directory as it is in .minecraft\saves\
    public final String worldName;

    public WorldDeletionEvent(String worldName) {
        this.worldName = worldName;
    }
}
