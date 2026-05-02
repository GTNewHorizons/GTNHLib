package com.gtnewhorizon.gtnhlib.client.event;

import cpw.mods.fml.common.eventhandler.Event;

public final class WorldDeletionEvent extends Event {

    public final String worldName;

    /**
     * {@link WorldDeletionEvent} is posted on the {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} a world
     * deletion is initiated through the world selection menu in the client and the world directory name has been
     * resolved.
     * </p>
     * <p>
     * {@link #worldName} String contains the name of the world directory as it exists under {@code .minecraft/saves},
     * this is not a full or absolute filesystem path.
     * </p>
     * <p>
     * This event is not {@link cpw.mods.fml.common.eventhandler.Cancelable}.
     * </p>
     */
    public WorldDeletionEvent(String worldName) {
        this.worldName = worldName;
    }
}
