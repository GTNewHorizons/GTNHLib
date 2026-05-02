package com.gtnewhorizon.gtnhlib.client.event;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

public final class WorldDeletionEvent extends Event {

    public final String worldName;

    /**
     * {@link WorldDeletionEvent} is posted on the {@link MinecraftForge#EVENT_BUS} when a world deletion is about to
     * occur through the world selection menu on the client.
     * <p>
     * The {@link #worldName} field is the name of the world directory that is about to be deleted and that can be found
     * in {@code .minecraft/saves}, this is not a full or absolute filesystem path.
     * </p>
     * <p>
     * This event is not {@link Cancelable}.
     * </p>
     */
    public WorldDeletionEvent(String worldName) {
        this.worldName = worldName;
    }
}
