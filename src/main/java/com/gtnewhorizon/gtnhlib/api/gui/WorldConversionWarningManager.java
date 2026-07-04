package com.gtnewhorizon.gtnhlib.api.gui;

import java.util.HashMap;

/**
 * Allows to register more confirmation screens to show before the FML missing mapping warning screen. Specifically
 * designed for world conversion warnings, these will only fire if the currently loading world contains any missing
 * mapping. Fires *after* {@link cpw.mods.fml.common.event.FMLMissingMappingsEvent}s but *before* the warning gui /
 * server message.
 */
public class WorldConversionWarningManager {

    public static final HashMap<String, IWorldConversionWarning> WARNINGS = new HashMap<>();

    /**
     * Call during game startup to register a {@link IWorldConversionWarning}.
     *
     * @param id A unique id for your warning
     */
    public static void register(String id, IWorldConversionWarning wcw) {
        WARNINGS.put(id, wcw);
    }

}
