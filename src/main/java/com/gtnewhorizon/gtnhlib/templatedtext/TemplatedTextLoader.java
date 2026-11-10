package com.gtnewhorizon.gtnhlib.templatedtext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.command.ColorPatternCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.CommentCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.FluidCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.IfCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.IfDefCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.IsolationCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.ItemCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.LangCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.PluralPresetCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.PluralityCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.StaticFormatCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.UnitCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.VarCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.ExampleLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.IfDefLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.IfLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.IncludeLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.RawLineProcessor;
import com.gtnewhorizon.gtnhlib.util.ResourceUtil;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleParser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.util.internal.ConcurrentSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/// A loader that reads a templated text file from a mod's jar or resource pack and converts it to text that can be
/// rendered normally. Templating is done though `commands` and `line processors`.
///
/// A command is denoted by an open brace, followed by some text, followed by a close brace. Some commands accept an
/// argument, which must be inserted after a colon: `{command_name:command_argument}`. Commands run an arbitrary java
/// lambda, so they can perform almost any action, but custom commands should follow the below standard unless they have
/// a good reason not to.
///
/// 1. Commands can be nested, but most commands will not evaluate nested commands by default.
/// These commands have a variant with an `-eval` suffix.
/// 2. Command arguments should be colon-separated or comma separated
/// 3. Commands should clean up after themselves, unless explicitly disabled by the user.
/// As an example, when given an argument, the MC color/format commands use [TextFormatUtil#FORMAT_PUSH_STACK] to
/// prevent format clobbering.
///
/// A line processor is similar to a command, but it governs the formatting or rendering of an entire line. All line
/// processors start with a `#` and end with a `:`. Command processors recieve the rest of the line as their argument
/// and are fully responsible for it's rendering. They can also prevent certain lines from being rendered.
///
/// Lines starting with `//` are considered to be comments and will be ignored entirely.
public class TemplatedTextLoader implements AutoCloseable {

    private static final String FALLBACK_LOCALE = "en_US";
    /** The extension for templated text files. */
    public static final String FILE_EXT = ".txt";
    public static final String FILE_EXT_OLD = ".md";
    /** The prefix for comments */
    public static final String COMMENT_START = "//";
    /** The prefix for escaping command brackets */
    public static final char ESCAPE_CHAR = '\\';
    /** The separator used to split command and line processor arguments. */
    public static final char ARG_SPLITTER = ':';
    /** The regex pattern used for string.split() calls to find simple arguments */
    public static final String ARG_SPLITTER_PATTERN = ":(?<!\\\\:)";
    /** The character used to begin a command invocation. */
    public static final char COMMAND_START = '{';
    /** The character used to end a command invocation. */
    public static final char COMMAND_END = '}';
    /** The prefix for all line processors. */
    public static final String LINE_PROCESSOR_START = "#";

    /** Line processors made available to all this templated text loaders. Don't prepend the keys with the # prefix. */
    public static final Map<@NotNull String, ITemplatedTextLineProcessor> GLOBAL_LINE_PROCESSORS = new Object2ObjectOpenHashMap<>();
    /** Commands made available to all templated text loaders. */
    public static final Map<@NotNull String, ITemplatedTextCommand> GLOBAL_COMMANDS = new Object2ObjectOpenHashMap<>();

    /** Line processors unique to this templated text loader. Don't prepend the keys with the # prefix. */
    protected final Object2ObjectOpenHashMap<String, ITemplatedTextLineProcessor> lineProcessor = new Object2ObjectOpenHashMap<>();
    /** Commands unique to this templated text loader. */
    protected final Object2ObjectOpenHashMap<String, ITemplatedTextCommand> commands = new Object2ObjectOpenHashMap<>();
    /**
     * Used to memoize the results of tooltips lookup since most don't tend to change and not having to constantly
     * reparse the files every frame will save a lot of processing time.
     */
    private final @NotNull ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();

    /**
     * The name of the sub folder for the text resources.
     */
    private final String folder;

    // region reload handler

    /**
     * Used to clear the cache when resources are reloaded.
     */
    @SideOnly(Side.CLIENT)
    private static class ReloadListener implements IResourceManagerReloadListener {

        private static final IResourceManagerReloadListener INSTANCE = new ReloadListener();

        @Override
        public void onResourceManagerReload(IResourceManager manager) {
            clearCache();
        }

        private ReloadListener() {}
    }

    private static final ConcurrentSet<TemplatedTextLoader> INSTANCES = new ConcurrentSet<>();

    public void initialize() {
        // empty on purpose, this just needs to be initialized before post load so the commands can be instantiated
        // and themed correctly.
    }

    @ApiStatus.Internal
    @SideOnly(Side.CLIENT)
    public static IResourceManagerReloadListener getReloadListener() {
        return ReloadListener.INSTANCE;
    }

    @ApiStatus.Internal
    public static void clearCache() {
        INSTANCES.forEach(x -> x.cache.clear());
    }

    // endregion reload handler

    private static final List<Pair<Supplier<String>, String[]>> throughputVariations = new ArrayList<>() {

        {
            // spotless:off
            this.add(Pair.of(NumberFormatUtil.LOC_CACHE::getUnitTick, new String[] { "tick", "Tick", "t", "T" }));
            this.add(Pair.of(NumberFormatUtil.LOC_CACHE::getUnitSecond, new String[] { "second", "Second", "sec", "Sec", "s", "S" }));
            this.add(Pair.of(NumberFormatUtil.LOC_CACHE::getUnitMinute,new String[] { "minute", "Minute", "min", "Min", "m", "M" }));
            this.add(Pair.of(NumberFormatUtil.LOC_CACHE::getUnitHour, new String[] { "hour", "Hour", "h", "H" }));
            this.add(Pair.of(NumberFormatUtil.LOC_CACHE::getUnitDay, new String[] { "day", "Day", "d", "d" }));
            // spotless:on
        }
    };

    public static void addThroughputVariations(@NotNull Map<@NotNull String, @NotNull ITemplatedTextCommand> map,
            @NotNull Supplier<@NotNull String> formatProvider, @NotNull Supplier<@NotNull String> unitFormatProvider,
            @NotNull Supplier<@NotNull String> unitSupplier, @NotNull String... amountUnitVariations) {
        addThroughputVariations(
                map,
                (timeSupplier) -> new UnitCommand(
                        new NumberFormatUtil.ThroughputUnitFormatter(
                                formatProvider,
                                unitFormatProvider,
                                unitSupplier,
                                timeSupplier)),
                amountUnitVariations);
    }

    public static <T extends NumberFormatUtil.ThroughputUnitFormatter> void addThroughputVariations(
            @NotNull Map<@NotNull String, @NotNull ITemplatedTextCommand> map,
            @NotNull Function<@NotNull Supplier<@NotNull String>, @NotNull ITemplatedTextCommand> commandSupplier,
            @NotNull String... amountUnitVariations) {
        for (Pair<Supplier<String>, String[]> timeVariation : throughputVariations) {
            ITemplatedTextCommand command = commandSupplier.apply(timeVariation.getLeft());
            String[] aliases = new String[amountUnitVariations.length * timeVariation.getRight().length];
            int i = 0;
            for (String timeUnit : timeVariation.getRight()) {
                for (String amountUnit : amountUnitVariations) {
                    aliases[i++] = amountUnit + timeUnit;
                }
            }
            for (String alias : aliases) {
                map.put(alias, command);
            }
        }
    }

    static {
        // global line processors
        addGlobalLineProcessor(new RawLineProcessor(), "raw");
        addGlobalLineProcessor(new ExampleLineProcessor(), "example");
        addGlobalLineProcessor(($1, $2, $3, $4, $5) -> {}, "comment", "");
        addGlobalLineProcessor(new IncludeLineProcessor(), "include", "import");
        addGlobalLineProcessor(new IfLineProcessor(true), "if-true", "ift", "if");
        addGlobalLineProcessor(new IfLineProcessor(false), "if-false", "iif", "if-not", "ifn");
        addGlobalLineProcessor(new IfDefLineProcessor(true), "if-exists", "ife", "if-defined", "if-def", "ifdef");
        addGlobalLineProcessor(
                new IfDefLineProcessor(false),
                "if-not-exists",
                "ifne",
                "if-not-defined",
                "if-ndef",
                "ifndef");

        // coloring commands
        for (EnumChatFormatting format : EnumChatFormatting.values()) {
            addGlobalCommand(new StaticFormatCommand(format.toString()), format.getFriendlyName());
        }
        addGlobalCommand(new ColorPatternCommand(), "color-pattern");

        // evaluating vars
        addGlobalCommand(new ItemCommand(), "item");
        addGlobalCommand(new FluidCommand(), "fluid");
        addGlobalCommand(new VarCommand(false), "var");
        addGlobalCommand(new VarCommand(true), "var-eval");
        addGlobalCommand(new LangCommand(false), "lang");
        addGlobalCommand(new LangCommand(true), "lang-eval");
        addGlobalCommand(new IsolationCommand(), "isolate");
        // spotless:off
        addGlobalCommand(new CommentCommand(), "comment", "#", "//");
        addGlobalCommand(new IfCommand(true), "if-true", "if");
        addGlobalCommand(new IfCommand(false), "if-false", "if-not");
        addGlobalCommand(new IfDefCommand(true), "if-exists", "ife", "if-defined", "if-def", "ifdef");
        addGlobalCommand(new IfDefCommand(false), "if-not-exists", "ifne", "if-not-defined", "if-ndef", "ifndef");
        addGlobalCommand(new PluralityCommand(true), "singular", "if-1");
        addGlobalCommand(new PluralityCommand(false), "plural", "if-n-1");
        for (Map.Entry<String, CLDRPluralRuleParser> kv : CLDRPluralRuleParser.getKnownRules()) {
            String name = kv.getKey().replace("_", "-");
            addGlobalCommand(new PluralPresetCommand(kv.getValue(), name.toUpperCase()), "plural-" + name.toLowerCase());
        }
        // spotless:on

        // unit standardization commands
        // spotless:off
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_FLUID), "fluid-unit", "fluid-amount", "l", "L", "liter", "Liter", "mb", "mB");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_RF), "RF", "redstone-flux-unit", "rf");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MJ), "MJ", "mj");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_AE), "AE", "ae");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_EU),"EU", "energy-unit", "eu");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_AMP),"amperage-unit", "amp-unit", "A", "a");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_HEAT), "heat-unit", "K", "k", "Kelvin", "kelvin");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_ACIDITY), "acidity-unit", "ph", "pH", "PH");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_RADIATION), "radiation-unit", "sv", "Sv", "SV");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_TEMPERATURE), "temperature-unit", "C");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_FORCE), "force-unit", "newton-unit", "N", "n");
        // No "ev"/"EV" aliases: EV is also a gregtech voltage tier name.
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_ELECTRON_VOLT), "electron-volt-unit", "eV");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_KEV), "kilo-electron-volt-unit", "kev", "KeV", "KEV");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MEV), "mega-electron-volt-unit", "mev", "MeV", "MEV");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_GEV), "giga-electron-volt-unit", "gev", "GeV", "GEV");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_TEV), "tera-electron-volt-unit", "tev", "TeV", "TEV");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_NANOGRAM), "nanogram-unit", "ng");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MICROGRAM), "microgram-unit", "μg");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MILLIGRAM), "milligram-unit", "mg");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_GRAM), "gram-unit", "g");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_KILOGRAM), "kilogram-unit", "kg");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_NANOMETER), "nanometer-unit", "nm");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MICROMETER), "micrometer-unit", "μm");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MILLIMETER), "millimeter-unit", "mm");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_CENTIMETER), "centimeter-unit", "cm");
        // no m because minutes would be more common
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_METER), "meter-unit");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_KILOMETER), "kilometer-unit", "km", "KM");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_NANOSECOND), "nanosecond-unit", "ns");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MICROSECOND), "microsecond-unit", "μs");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MILLISECOND), "millisecond-unit", "ms");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_TICK), "tick-unit", "t", "T");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_SECOND), "second-unit", "s", "S", "sec", "Sec");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_MINUTE), "minute-unit", "m", "m", "min", "Min");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_HOUR), "hour-unit", "h", "H");
        addGlobalCommand(new UnitCommand(NumberFormatUtil.FORMAT_DAY), "day-unit", "d", "D");
        // spotless:on

        // throughput unit standardization commands
        // spotless:off
        NumberFormatUtil.LocalisationCache locs = NumberFormatUtil.LOC_CACHE;
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingFluidThroughput, locs::getUnitFormatFluidThroughput, NumberFormatUtil::getFluidUnit, "fluid/", "fluid-unit/", "L/", "l/", "Liter/", "liter/", "mb/", "mB/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingEnergyThroughput, locs::getUnitFormatEnergyThroughput, locs::getUnitRF, "RF/", "rf/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingEnergyThroughput, locs::getUnitFormatEnergyThroughput, locs::getUnitMJ, "MJ/", "mj/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingEnergyThroughput, locs::getUnitFormatEnergyThroughput, locs::getUnitAE, "AE/", "ae/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingEnergyThroughput, locs::getUnitFormatEnergyThroughput, NumberFormatUtil::getEnergyUnit, "energy-unit/", "eu/", "eu", "EU/", "EU");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingSpeed, locs::getUnitFormatSpeed, locs::getUnitMeter, "m/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingSpeed, locs::getUnitFormatSpeed, locs::getUnitKilometer, "km/", "Km/", "KM/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingMassThroughput, locs::getUnitFormatMassThroughput, locs::getUnitGram, "g/");
        addThroughputVariations(GLOBAL_COMMANDS, locs::getSpacingMassThroughput, locs::getUnitFormatMassThroughput, locs::getUnitKilogram, "kg/", "Kg/", "KG/");
        // spotless:on

        addGlobalCommand(
                (a, b, c, d) -> { throw new RuntimeException("The crash command was executed!"); },
                "throw-err");
    }

    public TemplatedTextLoader(@NotNull String folder) {
        this.folder = "/" + folder + "/";
        INSTANCES.add(this);
    }

    @Override
    public void close() {
        INSTANCES.remove(this);
    }

    // command adding
    public void addCommand(@NotNull ITemplatedTextCommand command, @NotNull String name, String @NotNull... aliases) {
        this.commands.put(name, command);
        for (String alias : aliases) {
            commands.put(alias, command);
        }
    }

    public static void addGlobalCommand(@NotNull ITemplatedTextCommand command, @NotNull String name,
            String @NotNull... aliases) {
        GLOBAL_COMMANDS.put(name, command);
        for (String alias : aliases) {
            GLOBAL_COMMANDS.put(alias, command);
        }
    }

    // line processor adding

    public void addLineProcessor(ITemplatedTextLineProcessor processor, @NotNull String name,
            String @NotNull... aliases) {
        this.lineProcessor.put(name, processor);
        for (String alias : aliases) {
            this.lineProcessor.put(alias, processor);
        }
    }

    public static void addGlobalLineProcessor(ITemplatedTextLineProcessor processor, @NotNull String name,
            String @NotNull... aliases) {
        GLOBAL_LINE_PROCESSORS.put(name, processor);
        for (String alias : aliases) {
            GLOBAL_LINE_PROCESSORS.put(alias, processor);
        }
    }

    // resolvers

    private @Nullable ITemplatedTextLineProcessor getLineProcessor(String name) {
        ITemplatedTextLineProcessor ret = GLOBAL_LINE_PROCESSORS.getOrDefault(name, null);
        return ret == null ? lineProcessor.getOrDefault(name, null) : ret;
    }

    private @Nullable ITemplatedTextCommand getCommand(String name) {
        ITemplatedTextCommand ret = GLOBAL_COMMANDS.getOrDefault(name, null);
        return ret == null ? commands.getOrDefault(name, null) : ret;
    }

    // region file parsing

    /**
     * Loads a templated text document at `assets/[mod id]/lang/[language id]/[style]/[path].md`. On the client,
     * resource packs are scanned. On the server, the file is loaded directly from the current jar.
     *
     * @apiNote The result is memoized until resources are reloaded.
     *
     * @param resource The resource location for the file you want to load.
     * @return The processed output of the file.
     */
    public List<String> loadStandardPath(@NotNull ResourceLocation resource) {
        return loadStandardPath(resource, "", Collections::emptyMap);
    }

    /**
     * Loads a templated text document at `assets/[mod id]/lang/[language id]/[style]/[path].md`. On the client,
     * resource packs are scanned. On the server, the file is loaded directly from the current jar.
     *
     * @apiNote The result is memoized until resources are reloaded.
     *
     * @param resource      The resource location for the file you want to load.
     * @param cacheKeyExtra A string added to the resource location when computing the cache key.
     * @return The processed output of the file.
     */
    public List<String> loadStandardPath(@NotNull ResourceLocation resource, @NotNull String cacheKeyExtra) {
        return loadStandardPath(resource, cacheKeyExtra, Collections::emptyMap);
    }

    /**
     * Loads a templated text document at `assets/[mod id]/lang/[language id]/[style]/[path].md`. On the client,
     * resource packs are scanned. On the server, the file is loaded directly from the current jar.
     *
     * @apiNote The result is memoized until resources are reloaded.
     *
     * @param resource The resource location for the file you want to load.
     * @param lazyVars A lazy provider for the variables used when processing the file.
     * @return The processed output of the file.
     */
    public List<String> loadStandardPath(@NotNull ResourceLocation resource,
            @NotNull Supplier<@NotNull Map<@NotNull String, Object>> lazyVars) {
        return loadStandardPath(resource, "", lazyVars);
    }

    /**
     * Loads a templated text document at `assets/[mod id]/lang/[language id]/[style]/[path].md`. On the client,
     * resource packs are scanned. On the server, the file is loaded directly from the current jar.
     *
     * @apiNote The result is memoized until resources are reloaded.
     *
     * @param resource      The resource location for the file you want to load.
     * @param cacheKeyExtra A string added to the resource location when computing the cache key.
     * @param lazyVars      A lazy provider for the variables used when processing the file.
     * @return The processed output of the file.
     */
    public List<String> loadStandardPath(@NotNull ResourceLocation resource, @NotNull String cacheKeyExtra,
            @NotNull Supplier<@NotNull Map<@NotNull String, Object>> lazyVars) {
        String key = resource + cacheKeyExtra;
        return cache.computeIfAbsent(key, (k) -> this.loadStandardPath(resource, lazyVars.get()));
    }

    private static String getCurrentLanguage() {
        try {
            if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
            }
        } catch (RuntimeException t) {
            // empty on purpose
        }
        return "en_US";
    }

    private ResourceLocation getExactPath(ResourceLocation resource, String language) {
        return new ResourceLocation(
                resource.getResourceDomain(),
                "lang/" + language + this.folder + resource.getResourcePath());
    }

    /**
     * Loads and parses a templated text file from the resources.
     *
     * @param resource The resource location for the file you want to load.
     * @param vars     The variables used when processing the file.
     * @return The parsed lines with processed stacks and the special line formatting if one is specified.
     */
    public List<String> loadStandardPath(ResourceLocation resource, Map<String, Object> vars) {
        List<String> out = loadStandardPathNoFormat(resource, vars, null);
        out.replaceAll(parsedLine -> TextFormatUtil.processFormatStacks(formatLine(parsedLine)));
        return out;
    }

    /**
     * Loads and parses a templated text file from the resources without applying the line formatting or the resolving
     * format stacks.
     *
     * @apiNote This is mostly an internal function only made public for its use by line processors and testing. Use one
     *          of the {@link #loadStandardPath} functions instead if you want renderable text.
     *
     * @param resource       The resource location for the file you want to load.
     * @param vars           The variables used when processing the file.
     * @param includeHistory The list of files previously included, leave null if you are at the root.
     * @return The parsed lines without processed stacks or the special line formatting if one is specified.
     */
    public List<String> loadStandardPathNoFormat(ResourceLocation resource, Map<String, Object> vars,
            @Nullable Stack<ResourceLocation> includeHistory) {
        List<String> ret = new ArrayList<>();
        String currentLanguage = getCurrentLanguage();
        ResourceLocation exactPath = this.getExactPath(resource, currentLanguage);
        try {
            try {
                ret.addAll(this.loadExactPath(exactPath, vars, includeHistory));
                return ret;
            } catch (Throwable t) {
                // don't fall back if there is no fallback
                if (currentLanguage.equals(FALLBACK_LOCALE)) throw t;
                // if it's not a file not found error, append the error
                if (!(t instanceof FileNotFoundException)) {
                    appendFileLoadError(ret, t, exactPath);
                }
                // update the path to the fallback and try to load the fallback file.
                exactPath = this.getExactPath(resource, FALLBACK_LOCALE);
                ret.addAll(this.loadExactPath(exactPath, vars, includeHistory));
                return ret;
            }
        } catch (FileNotFoundException fnfe) {
            StringBuilder sb = new StringBuilder();
            appendError(sb, "FILE-NOT-FOUND", exactPath + FILE_EXT);
            ret.add(sb.toString());
            return ret;
        } catch (Throwable t) {
            appendFileLoadError(ret, t, exactPath);
            return ret;
        }
    }

    // C0bra5: making this private because realistically no mods should ever attempt to force a locale when loading
    // this.
    private List<String> loadExactPath(ResourceLocation resource, Map<String, Object> vars,
            @Nullable Stack<ResourceLocation> includeHistory) throws IOException {

        // prevent infinite loops
        if (includeHistory == null) {
            includeHistory = new Stack<>();
        } else {
            for (ResourceLocation rs : includeHistory) {
                if (rs.equals(resource)) {
                    StringBuilder sb = new StringBuilder();
                    appendError(sb, "INCLUDE-LOOP", resource + FILE_EXT);
                    List<String> out = new LinkedList<>();
                    out.add(sb.toString());
                    return out;
                }
            }
        }
        includeHistory.push(resource);

        try {
            List<String> lines;
            // TODO: remove the .md file extension support for gtnh 2.10
            try {
                lines = ResourceUtil.readLinesFrom(
                        new ResourceLocation(resource.getResourceDomain(), resource.getResourcePath() + FILE_EXT));
            } catch (FileNotFoundException e) {
                lines = ResourceUtil.readLinesFrom(
                        new ResourceLocation(resource.getResourceDomain(), resource.getResourcePath() + FILE_EXT_OLD));
            }
            List<String> out = new ArrayList<>();
            evaluateLinesNoFormat(out, lines, vars, includeHistory);
            return out;
        } finally {
            includeHistory.pop();
        }
    }

    // endregion file parsing

    // region parsing

    /**
     * Parses arbitrary lines of text using the templated text processor.
     *
     * @param lines The lines to process
     * @return The parsed lines with processed stacks and the special line formatting if one is specified.
     */
    public List<String> evaluateLines(Iterable<String> lines) {
        return evaluateLines(lines, Collections.emptyMap());
    }

    /**
     * Parses arbitrary lines of text using the templated text processor.
     *
     * @param lines The lines to process
     * @param vars  The variables used to process the lines.
     * @return The parsed lines with processed stacks and the special line formatting if one is specified.
     */
    public List<String> evaluateLines(Iterable<String> lines, Map<String, Object> vars) {
        List<String> out = new LinkedList<>();
        try {
            evaluateLinesNoFormat(out, lines, vars, new Stack<>());
            out.replaceAll(parsedLine -> TextFormatUtil.processFormatStacks(formatLine(parsedLine)));
        } catch (RuntimeException e) {
            appendException(out, "UNEXPECTED-ERROR", e);
        }
        return out;
    }

    /**
     * Parses arbitrary lines of text using the templated text processor without resolving format stacks or appending
     * the special line formatting if one is specified.
     *
     * @apiNote This is mostly an internal function only made public for its use by line processors and testing. Use
     *          {@link #evaluateLines(Iterable, Map)} instead if you want renderable text.
     *
     * @param out            A list to append the output to.
     * @param lines          The lines to process
     * @param vars           The variables used to process the lines.
     * @param includeHistory The list of files previously included files, leave <b>empty</b> if you are at the root.
     */
    public void evaluateLinesNoFormat(List<String> out, Iterable<String> lines, Map<String, Object> vars,
            Stack<ResourceLocation> includeHistory) {
        for (String line : lines) {
            // ignore comments
            if (line.startsWith(COMMENT_START)) continue;
            try {
                if (line.startsWith(LINE_PROCESSOR_START)) {
                    String[] parts = line.substring(LINE_PROCESSOR_START.length()).split(ARG_SPLITTER_PATTERN, 2);
                    if (parts.length < 2) {
                        throw TemplatedTextParsingException
                                .lineProcessorStandard(parts.length >= 1 ? parts[0].trim() : "", "NOT ENOUGH ARGS");
                    }
                    ITemplatedTextLineProcessor processor = getLineProcessor(parts[0]);
                    if (processor == null) {
                        throw TemplatedTextParsingException
                                .lineProcessorStandard("UNKNOWN LINE PROCESSOR", parts[0].trim());
                    }
                    processor.processLine(this, out, parts[1], vars, includeHistory);
                    continue;
                }
                out.add(eval(line, vars));
            } catch (TemplatedTextParsingException ise) {
                for (String errLine : ise.userLines) {
                    out.add(TextFormatUtil.wrapFormatting(EnumChatFormatting.RED, errLine));
                }
            } catch (RuntimeException e) {
                appendException(out, "LINE-ERROR", e);
            }
        }
    }

    protected String formatLine(String parsedLine) {
        return parsedLine;
    }

    public String eval(String unparsed, Map<String, Object> vars) {
        StringBuilder sb = new StringBuilder(unparsed.length());

        emit(unparsed, sb, vars);

        return sb.toString();
    }

    public void emit(String unparsed, StringBuilder out, Map<String, Object> vars) {
        parseCommands(unparsed, new CommandVisitor() {

            @Override
            public void visitChar(char c, boolean escaped) {
                out.append(c);
            }

            @Override
            public void visitCommand(String command) {
                command = command.substring(1, command.length() - 1);

                int colon = command.indexOf(ARG_SPLITTER);
                String commandName = colon == -1 ? command : command.substring(0, colon);
                String arg = colon == -1 ? "" : command.substring(colon + 1);

                ITemplatedTextCommand commandHandler = TemplatedTextLoader.this.getCommand(commandName.trim());
                if (commandHandler == null) {
                    appendError(out, "UNKNOWN COMMAND", commandName.trim());
                } else {
                    commandHandler.process(TemplatedTextLoader.this, arg, out, vars);
                }
            }
        });
    }

    public interface CommandVisitor {

        void visitChar(char c, boolean escaped);

        void visitCommand(String command);
    }

    public static void parseCommands(String unparsed, CommandVisitor visitor) {
        final char[] chars = unparsed.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char curr = chars[i];

            if (curr == ESCAPE_CHAR) {
                if (i < chars.length - 1) {
                    visitor.visitChar(chars[i + 1], true);
                    i++;
                }
            } else if (curr == COMMAND_START) {
                int end = i;
                int open = 0;

                while (end < chars.length) {
                    if (chars[end] == ESCAPE_CHAR) {
                        end++;
                        if (end < chars.length) end++;
                        continue;
                    }

                    if (chars[end] == COMMAND_START) {
                        open++;
                    }

                    if (chars[end] == COMMAND_END) {
                        open--;
                    }

                    if (open <= 0) {
                        break;
                    }

                    end++;
                }

                if (open > 0) {
                    char[] spaces = new char[i];
                    Arrays.fill(spaces, ' ');

                    StringBuilder sb = new StringBuilder();
                    appendError(sb, "SYNTAX ERROR", "UNCLOSED BRACE");
                    throw new TemplatedTextParsingException(
                            new String[] { "Unclosed open brace in line:", unparsed, new String(spaces) + "^" },
                            new String[] { sb.toString(), unparsed, unparsed.substring(0, i) + "^" });
                }

                String command = unparsed.substring(i, end + 1);

                if (command.length() <= 2) {
                    throw new TemplatedTextParsingException(
                            "Invalid command: '" + command
                                    + "': expected open + close brace that contains the command name and (optional) arguments");
                }

                visitor.visitCommand(command);

                i = end;
            } else {
                visitor.visitChar(chars[i], false);
            }
        }
    }

    // endregion parsing

    // region standardized errors

    /**
     * An exception thrown by line processors and the text loader itself when the syntax of something is broken.
     */
    public static class TemplatedTextParsingException extends RuntimeException {

        public final String[] userLines;

        /**
         * Throws an exception with formatting specific to line processor errors.
         *
         * @param processorName The name of the line processor that failed.
         * @param message       The message to include.
         * @return An exception ready to throw.
         */
        public static TemplatedTextParsingException lineProcessorStandard(String processorName, String message) {
            StringBuilder sb = new StringBuilder();
            appendError(sb, processorName, "ERROR");
            return new TemplatedTextParsingException(
                    new String[] { "LINE PROCESSOR ERROR IN " + processorName + ": " + message },
                    new String[] { sb.toString(), message });
        }

        public TemplatedTextParsingException(String... lines) {
            super(String.join("\n", lines));
            this.userLines = lines;
        }

        public TemplatedTextParsingException(String[] logLines, String[] userLines) {
            super(String.join("\n", logLines));
            this.userLines = userLines;
        }
    }

    private void appendFileLoadError(@NotNull List<String> out, @NotNull Throwable t,
            @NotNull ResourceLocation exactPath) {
        StringBuilder sb = new StringBuilder();
        appendError(sb, "UNEXPECTED-ERROR", "file: " + exactPath + FILE_EXT);
        appendException(out, "UNEXPECTED-ERROR", t);
    }

    private void appendException(@NotNull List<String> out, @NotNull String header, @NotNull Throwable t) {
        StringBuilder sb = new StringBuilder();
        out.add(sb.toString());
        String message = t.toString();
        for (String errLine : message.split("[\r\n]+")) {
            sb.delete(0, sb.length());
            appendError(sb, header, errLine);
            out.add(sb.toString());
        }
    }

    /**
     * Appends a command error to the given string builder with a define coloring and layout.
     *
     * @param lineOutput  The string builder to append to.
     * @param commandName The name of the command that failed.
     * @param message     The error message.
     */
    public static void appendError(StringBuilder lineOutput, String commandName, String message) {
        lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK).append(EnumChatFormatting.RED).append("[")
                .append(commandName).append(" # ").append(message).append("]").append(TextFormatUtil.FORMAT_POP_STACK);
    }

    public static String[] splitArgs(String args, int max) {
        String[] parts = args.split(ARG_SPLITTER_PATTERN, max);
        // un-escape escaped splitters
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("\\:", ":");
        }
        return parts;
    }

    public static List<String> splitArgsWithCommands(@NotNull TemplatedTextLoader loader, String arg, int max,
            boolean eval, @NotNull Map<@NotNull String, Object> vars) {
        if (max == 0) {
            List<String> ret = new ArrayList<>(1);
            ret.add(arg);
            return ret;
        }
        StringBuilder paramBuffer = new StringBuilder();
        List<String> paramList = new ArrayList<>();

        // We use the same parse for eval and no-eval to keep the syntax the same.
        // If the command 'quoting' isn't desirable, the end-user can escape braces to split on colons within
        // command-like structures.
        TemplatedTextLoader.parseCommands(arg, new TemplatedTextLoader.CommandVisitor() {

            @Override
            public void visitChar(char c, boolean escaped) {
                if (!escaped && (max < 0 || paramList.size() < max) && c == TemplatedTextLoader.ARG_SPLITTER) {
                    paramList.add(paramBuffer.toString());
                    paramBuffer.setLength(0);
                } else {
                    paramBuffer.append(c);
                }
            }

            @Override
            public void visitCommand(String command) {
                if (eval) {
                    paramBuffer.append(loader.eval(command, vars));
                } else {
                    paramBuffer.append(command);
                }
            }
        });

        if (paramBuffer.length() > 0) {
            paramList.add(paramBuffer.toString());
        }

        return paramList;
    }

    // endregion standardized errors

}
