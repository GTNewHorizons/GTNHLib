package com.gtnewhorizon.gtnhlib.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.resources.I18n;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Helper for overrideable GUI text.
 *
 * Use this when a GUI draws hardcoded strings (like "+", "-1", "->", "/", etc.) that you want resource packs to recolor
 * / rewrite.
 *
 * Keys are namespaced automatically with "gtnhlib." so you only pass the short key: "symbol.plus" -> looks up
 * "gtnhlib.symbol.plus"
 *
 * Client-side only.
 */
@SideOnly(Side.CLIENT)
public final class GuiText {

    /** Internal namespace to avoid key collisions */
    private static final String PREFIX = "gtnhlib.";

    /** Logger used only for debug output */
    private static final Logger LOGGER = LogManager.getLogger("GTNHLib-GuiText");

    /**
     * Enables debug logging for missing language keys.
     *
     * Enable with: -Dgtnhlib.guiTextDebug=true
     */
    private static final boolean DEBUG_LOG_MISSING_KEYS = Boolean
            .parseBoolean(System.getProperty("gtnhlib.guiTextDebug", "false"));

    /**
     * Enables crash-on-missing-key behavior.
     *
     * This is STRICTLY opt-in and intended for development only.
     *
     * Enable with: -Dgtnhlib.guiTextCrashOnMissing=true
     */
    private static final boolean DEBUG_CRASH_ON_MISSING = Boolean
            .parseBoolean(System.getProperty("gtnhlib.guiTextCrashOnMissing", "false"));

    /**
     * Tracks which missing keys have already been logged, preventing spam during per-tick GUI rendering.
     */
    private static final Set<String> WARNED_KEYS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private GuiText() {
        // Utility class
    }

    /*
     * ----------------------- HOW TO USE (examples) ----------------------- 1) Simple symbol replacement:
     * fontRenderer.drawString( GuiText.get("symbol.plus", "+"), x, y, 0x404040 ); assets/gtnhlib/lang/en_US.lang:
     * gtnhlib.symbol.plus=+ resource pack override: gtnhlib.symbol.plus=§7+§r 2) Button label: button.displayString =
     * GuiText.get("button.minus_one", "-1"); lang: gtnhlib.button.minus_one=-1 3) Replace string concatenation with a
     * formatted key: // BEFORE: // "->" + value + "->" // AFTER: fontRenderer.drawString(
     * GuiText.format("symbol.arrow_math", "->%s->", value), 63, 59, 0x404040 ); lang: gtnhlib.symbol.arrow_math=->%s->
     * resource pack override (color different parts): gtnhlib.symbol.arrow_math=§7->§f%s§4->§r Notes: - Use %s / %d
     * placeholders in the lang value when passing args. - Inline '§' color codes are allowed in lang entries
     * (resource-pack controlled).
     */

    /**
     * Returns overrideable GUI text.
     *
     * If the key exists, its translated value is returned. If the key does not exist: - fallback is returned - optional
     * debug logging may occur - optional crash may be triggered (dev only)
     */
    public static String get(String key, String fallback, Object... args) {
        String fullKey = PREFIX + key;
        String translated = I18n.format(fullKey, args);

        // In MC 1.7.10, missing keys usually return the key itself
        if (translated == null || translated.equals(fullKey)) {

            if (DEBUG_CRASH_ON_MISSING) {
                throw new IllegalStateException(
                        "Missing GUI lang key: '" + fullKey + "' (fallback: '" + fallback + "')");
            }

            if (DEBUG_LOG_MISSING_KEYS) {
                logMissingKeyOnce(fullKey, fallback);
            }

            return fallback;
        }

        return translated;
    }

    /**
     * Alias for get(...).
     *
     * Exists purely for readability in formatting-heavy call sites.
     */
    public static String format(String key, String fallback, Object... args) {
        return get(key, fallback, args);
    }

    private static void logMissingKeyOnce(String fullKey, String fallback) {
        if (WARNED_KEYS.add(fullKey)) {
            LOGGER.debug("Missing GUI lang key '{}' (using fallback '{}')", fullKey, fallback);
        }
    }
}
