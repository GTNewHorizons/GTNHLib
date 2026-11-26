package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.config.Config;

/// Settings for chat autocomplete.
///
/// Registered from the coremod, since the mixin settings must be loaded before early mixins are applied.
@Config(modid = GTNHLib.MODID, category = "autocomplete", filename = "gtnhlib-autocomplete")
@Config.LangKeyPattern(pattern = "gtnhlib.config.%cat.%field")
public final class AutocompleteConfig {

    /// Synced so that a server can turn autocomplete off for everyone who connects to it: clients then stop asking
    /// for suggestions, rather than showing their own suggestions until the server's empty reply arrives. The mixins
    /// follow the local value at startup, since they're applied before any server is joined.
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.Sync
    public static boolean enabled;

    /// Synced so that clients suggest whichever form of effect the server's `/effect` accepts.
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.Sync
    public static boolean effectNames;

    /// Synced so that clients suggest whichever form of enchantment the server's `/enchant` accepts.
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    @Config.Sync
    public static boolean enchantNames;

    @Config.Comment("Shows the popup listing suggestions above the chat bar. Arrow keys and tab work as in vanilla when it's hidden.")
    @Config.DefaultBoolean(true)
    public static boolean showPopup;

    @Config.Comment("Shows the rest of the command in gray after the text, such as '<player> <item> [amount]'.")
    @Config.DefaultBoolean(true)
    public static boolean showHint;

    @Config.Comment("How many suggestions the popup shows at once.")
    @Config.DefaultInt(11)
    @Config.RangeInt(min = 1, max = 30)
    public static int popupRows;

    @Config.Comment({ "The most suggestions sent to a client at once. Lists such as items can be very long, and are",
            "narrowed down as the player types." })
    @Config.DefaultInt(256)
    @Config.RangeInt(min = 1, max = 4096)
    public static int maxSuggestions;
}
