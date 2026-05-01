package com.gtnewhorizon.gtnhlib.client.event;

import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;

public final class WorldDeletionEvent extends Event {

    @Getter
    private final String worldName;

    public WorldDeletionEvent(String worldName) {
        this.worldName = worldName;
    }
}
