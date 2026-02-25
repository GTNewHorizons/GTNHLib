package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.util.Locale;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "gtnhlib", category = "number_formatting", filename = "gtnhlib-number-formatting")
public final class NumberFormatConfig {

    @Config.Comment({ "Completely disables ALL number formatting, all numbers will show 'raw', i.e. 1000000", })
    @Config.DefaultBoolean(false)
    public static boolean disableFormattedNotation = false;

    @Config.Comment({
            "Completely disables exponential notation. No scientific, standard form or engineering notation." })
    @Config.DefaultBoolean(false)
    public static boolean globalDisableExponentialNotation = false;

    @Config.Comment({ "True indicates to use mB, false uses GregTech L." })
    @Config.DefaultBoolean(true)
    public static boolean useForgeFluidMillibuckets = true;

    @Config.Comment({ "Controls how very large numbers are rendered when scientific notation is used.",
            "  SCIENTIFIC   – Standard scientific notation (e.g. 1.23e12)",
            "  ENGINEERING  – Engineering notation with exponent in multiples of 3 (e.g. 123e9)",
            "  POWER_OF_TEN – Power-of-ten notation using explicit multiplication (e.g. 1.23*10^12)",
            "Invalid values will cause the game to fail fast with an error." })
    @Config.DefaultString("SCIENTIFIC")
    public static String formatPattern = "SCIENTIFIC";

    @Config.Comment({ "Select language format for number formatting.", "Each option shows example formatting:",
            "  SYSTEM_DEFAULT – Use your system language settings",
            "  ENGLISH_US     – 1,234.56 (comma grouping, period decimal)",
            "  RUSSIAN        – 1 234,56 (space grouping, comma decimal)",
            "  GERMAN         – 1.234,56 (period grouping, comma decimal)",
            "  FRENCH         – 1 234,56 (space grouping, comma decimal)",
            "  SPANISH        – 1.234,56 (period grouping, comma decimal)",
            "  ITALIAN        – 1.234,56 (period grouping, comma decimal)",
            "  JAPANESE       – 1,234.56 (comma grouping, period decimal)",
            "  CHINESE        – 1,234.56 (comma grouping, period decimal)",
            "  POLISH         – 1 234,56 (space grouping, comma decimal)",
            "  PORTUGUESE_BR  – 1.234,56 (period grouping, comma decimal)",
            "  DUTCH          – 1.234,56 (period grouping, comma decimal)",
            "  SWEDISH        – 1 234,56 (space grouping, comma decimal)",
            "  NORWEGIAN      – 1 234,56 (space grouping, comma decimal)",
            "  FINNISH        – 1 234,56 (space grouping, comma decimal)",
            "  CZECH          – 1 234,56 (space grouping, comma decimal)",
            "  HUNGARIAN      – 1 234,56 (space grouping, comma decimal)" })
    @Config.DefaultEnum("SYSTEM_DEFAULT")
    public static LocaleOption numberFormatLocale = LocaleOption.SYSTEM_DEFAULT;

    @Config.Ignore
    public static ExponentialFormat EXPONENTIAL_FORMAT = ExponentialFormat.SCIENTIFIC;

    @Config.Ignore
    private static Locale customLocale = LocaleOption.SYSTEM_DEFAULT.getLocale();

    /**
     * Enum representing available locale options for number formatting
     */
    public enum LocaleOption {

        SYSTEM_DEFAULT("System Default", Locale.getDefault(Locale.Category.FORMAT)),
        ENGLISH_US("English (US) - 1,234.56", new Locale("en", "US")),
        RUSSIAN("Russian - 1 234,56", new Locale("ru", "RU")),
        GERMAN("German - 1.234,56", new Locale("de", "DE")),
        FRENCH("French - 1 234,56", new Locale("fr", "FR")),
        SPANISH("Spanish - 1.234,56", new Locale("es", "ES")),
        ITALIAN("Italian - 1.234,56", new Locale("it", "IT")),
        JAPANESE("Japanese - 1,234.56", new Locale("ja", "JP")),
        CHINESE("Chinese - 1,234.56", new Locale("zh", "CN")),
        POLISH("Polish - 1 234,56", new Locale("pl", "PL")),
        PORTUGUESE_BR("Portuguese (Brazil) - 1.234,56", new Locale("pt", "BR")),
        DUTCH("Dutch - 1.234,56", new Locale("nl", "NL")),
        SWEDISH("Swedish - 1 234,56", new Locale("sv", "SE")),
        NORWEGIAN("Norwegian - 1 234,56", new Locale("no", "NO")),
        FINNISH("Finnish - 1 234,56", new Locale("fi", "FI")),
        CZECH("Czech - 1 234,56", new Locale("cs", "CZ")),
        HUNGARIAN("Hungarian - 1 234,56", new Locale("hu", "HU"));

        private final String displayName;
        private final Locale locale;

        LocaleOption(String displayName, Locale locale) {
            this.displayName = displayName;
            this.locale = locale;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Locale getLocale() {
            if (this == SYSTEM_DEFAULT) {
                return Locale.getDefault(Locale.Category.FORMAT);
            }
            return locale;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Synchronizes the number formatting locale from config. Called during mod initialization and when config changes.
     */
    public static void syncNumberFormatting() {
        // Parse and apply exponential format pattern
        try {
            EXPONENTIAL_FORMAT = ExponentialFormat.parse(formatPattern);
        } catch (Exception e) {
            GTNHLib.error("Invalid exponential format pattern: " + formatPattern + ", using SCIENTIFIC");
            EXPONENTIAL_FORMAT = ExponentialFormat.SCIENTIFIC;
        }

        customLocale = numberFormatLocale.getLocale();
        GTNHLib.info(
                "Number formatting: Using " + numberFormatLocale.getDisplayName()
                        + " ("
                        + customLocale.toString()
                        + ")");
    }

    /**
     * Gets the currently active locale for number formatting.
     *
     * @return Active locale selected by configuration
     */
    public static Locale getActiveLocale() {
        if (numberFormatLocale == LocaleOption.SYSTEM_DEFAULT) {
            return Locale.getDefault(Locale.Category.FORMAT);
        }
        return customLocale;
    }

    /**
     * Checks if a non-system locale override is active.
     *
     * @return true if a locale other than system default is selected
     */
    public static boolean hasCustomLocale() {
        return numberFormatLocale != LocaleOption.SYSTEM_DEFAULT;
    }
}
