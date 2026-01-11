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
 * Helper for overrideable GUI text. Use this when a GUI draws hardcoded strings (like "+", "-1", "->", "/", etc.) that
 * you want resource packs to recolor or rewrite.
 *
 * <p>
 * Keys are namespaced automatically with "gtnhlib.", so you only pass the short key: "symbol.plus" -> looks up
 * "gtnhlib.symbol.plus".
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * {@code
 * fontRenderer.drawString(GuiText.format("symbol.plus", "+"), x, y, 0x404040);
* fontRenderer.drawString(GuiText.format("symbol.arrow_math", "->%s->", value), 63, 59, 0x404040);
 * button.displayString = GuiText.format("button.minus_one", "-1");
 * }
 * </pre>
 * <p>
 * Lang entries:
 * <pre>
 * {@code
 * gtnhlib.symbol.plus=+
 * gtnhlib.button.minus_one=-1
 * gtnhlib.symbol.arrow_math=->%s->
 * }
 * </pre>
 *
 * <p>
 * Dev flags:
 * 
 * <pre>
 * {@code
 * -Dgtnhlib.guiTextDebug=true (log missing keys once)
 * -Dgtnhlib.guiTextCrashOnMissing=true (throw on missing key)
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class GuiText {

    // Internal namespace to avoid key collisions
    private static final String PREFIX = "gtnhlib.";

    // Logger used only for debug output
    private static final Logger LOGGER = LogManager.getLogger("GTNHLib-GuiText");

    /*
     * Enables debug logging for missing language keys Enable with: -Dgtnhlib.guiTextDebug=true
     */
    private static final boolean DEBUG_LOG_MISSING_KEYS = Boolean
            .parseBoolean(System.getProperty("gtnhlib.guiTextDebug", "false"));

    /*
     * Enables crash-on-missing-key behavior This is STRICTLY opt-in and intended for development only Enable with:
     * -Dgtnhlib.guiTextCrashOnMissing=true
     */
    private static final boolean DEBUG_CRASH_ON_MISSING = Boolean
            .parseBoolean(System.getProperty("gtnhlib.guiTextCrashOnMissing", "false"));

    // Tracks which missing keys have already been logged, preventing spam during per-tick GUI rendering
    private static final Set<String> WARNED_KEYS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private GuiText() {
        // Utility class
    }

    /*
     * ----------------------- HOW TO USE (examples) ----------------------- 1) Simple symbol replacement:
     * fontRenderer.drawString(GuiText.format("symbol.plus", "+"), x, y, 0x404040) assets/gtnhlib/lang/en_US.lang:
     * gtnhlib.symbol.plus=+ resource pack override: gtnhlib.symbol.plus=§7+§r 2) Button label: button.displayString =
     * GuiText.format("button.minus_one", "-1") lang: gtnhlib.button.minus_one=-1 3) Replace string concatenation with a
     * formatted key: // BEFORE: // "->" + value + "->" // AFTER:
     * fontRenderer.drawString(GuiText.format("symbol.arrow_math", "->%s->", value), 63, 59, 0x404040) lang:
     * gtnhlib.symbol.arrow_math=->%s-> resource pack override (color different parts):
     * gtnhlib.symbol.arrow_math=§7->§f%s§4->§r Notes: - Use %s / %d placeholders in the lang value when passing args -
     * Inline '§' color codes are allowed in lang entries (resource-pack controlled)
     */

    /**
     * Returns overrideable GUI text. If the key exists, its translated value is returned. If the key does not exist,
     * the fallback is returned; optional debug logging may occur or an opt-in dev crash can be triggered.
     *
     * @param key      short key without the "gtnhlib." prefix
     * @param fallback fallback to return when no translation exists
     * @param args     format args used by I18n.format
     * @return translated value or fallback
     */
    public static String translate(String key, String fallback, Object... args) {
        String fullKey = PREFIX + key;
        String translated = I18n.format(fullKey, args);
        if (translated != null && !translated.equals(fullKey)) {
            return translated;
        }

        // Missing / invalid translation
        if (DEBUG_CRASH_ON_MISSING) {
            throw new IllegalStateException("Missing GUI lang key: '" + fullKey + "' (fallback: '" + fallback + "')");
        }

        if (DEBUG_LOG_MISSING_KEYS) {
            logMissingKeyOnce(fullKey, fallback);
        }

        return fallback;
    }

    // Alias for {@link #translate(String, String, Object...)} to keep call sites terse.
    public static String format(String key, String fallback, Object... args) {
        return translate(key, fallback, args);
    }

    private static void logMissingKeyOnce(String fullKey, String fallback) {
        if (WARNED_KEYS.add(fullKey)) {
            LOGGER.debug("Missing GUI lang key '{}' (using fallback '{}')", fullKey, fallback);
        }
    }

    /**
     * Common key constants for GUI text. These are optional helpers to avoid typos and keep key names consistent.
     * Example usage: GuiText.format(GuiText.Keys.ARROW_LEFT, "<")
     */
    public static final class Keys {

        private Keys() {}

        public static final String ARROW_LEFT = "symbol.arrow_left"; // "<"
        public static final String ARROW_RIGHT = "symbol.arrow_right"; // ">"
        public static final String ARROW_LEFT_BIG = "symbol.arrow_left_big"; // "<-"
        public static final String ARROW_RIGHT_BIG = "symbol.arrow_right_big"; // "->"
        public static final String SLASH = "symbol.slash"; // "/"
        public static final String PLUS = "symbol.plus"; // "+"
        public static final String MINUS = "symbol.minus"; // "-"
        public static final String SYMBOL_X = "symbol.x"; // "x" (count / multiply)
        public static final String SYMBOL_EQ = "symbol.eq"; // "="
        public static final String SYMBOL_COLON = "symbol.colon"; // ":"
        public static final String PERCENT = "symbol.percent"; // "%"
        public static final String BRACKET_OPEN = "symbol.bracket_open"; // "("
        public static final String BRACKET_CLOSE = "symbol.bracket_close"; // ")"

        public static final String ENERGY_EU = "symbol.energy.eu"; // "EU"
        public static final String ENERGY_RF = "symbol.energy.rf"; // "RF"
        public static final String ENERGY_MJ = "symbol.energy.mj"; // "MJ"
        public static final String ENERGY_LP = "symbol.energy.lp"; // "LP"
        public static final String ENERGY_AE = "symbol.energy.ae"; // "AE"
        public static final String ENERGY_W = "symbol.energy.w"; // "W"

        public static final String ENERGY_EU_T = "symbol.energy.eu_t"; // "EU/t"
        public static final String ENERGY_EU_S = "symbol.energy.eu_s"; // "EU/s"
        public static final String ENERGY_RF_T = "symbol.energy.rf_t"; // "RF/t"
        public static final String ENERGY_RF_S = "symbol.energy.rf_s"; // "RF/s"
        public static final String ENERGY_PER_T = "symbol.energy.per_t"; // "/t"
        public static final String ENERGY_PER_S = "symbol.energy.per_s"; // "/s"

        public static final String UNIT_MB = "unit.mb"; // "mB"
        public static final String UNIT_L = "unit.l"; // "L"

        public static final String STATE_ON = "state.on"; // "ON"
        public static final String STATE_OFF = "state.off"; // "OFF"
        public static final String STATE_YES = "state.yes"; // "YES"
        public static final String STATE_NO = "state.no"; // "NO"
        public static final String STATE_ENABLED = "state.enabled"; // "Enabled"
        public static final String STATE_DISABLED = "state.disabled"; // "Disabled"

        public static final String DIR_IN = "dir.in"; // "IN"
        public static final String DIR_OUT = "dir.out"; // "OUT"
        public static final String DIR_INPUT = "dir.input"; // "Input"
        public static final String DIR_OUTPUT = "dir.output"; // "Output"

        public static final String LABEL_MAX = "label.max"; // "Max"
        public static final String LABEL_MIN = "label.min"; // "Min"
        public static final String LABEL_AVG = "label.avg"; // "Avg"

        public static final String ACTION_ADD = "action.add"; // "Add"
        public static final String ACTION_REMOVE = "action.remove"; // "Remove"
        public static final String ACTION_CLEAR = "action.clear"; // "Clear"
        public static final String ACTION_RESET = "action.reset"; // "Reset"
        public static final String ACTION_APPLY = "action.apply"; // "Apply"

        public static final String STATUS_OK = "status.ok"; // "OK"
        public static final String STATUS_ACTIVE = "status.active"; // "Active"
        public static final String STATUS_IDLE = "status.idle"; // "Idle"

    }

}
