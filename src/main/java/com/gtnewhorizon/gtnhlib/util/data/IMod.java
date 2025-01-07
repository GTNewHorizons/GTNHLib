package com.gtnewhorizon.gtnhlib.util.data;

/**
 * An interface for any mod enums. Represents a mod.
 */
public interface IMod {

    boolean isModLoaded();

    /** Gets the mod id. */
    String getID();

    /** Gets the mod's resource location prefix. */
    String getResourceLocation();
}
