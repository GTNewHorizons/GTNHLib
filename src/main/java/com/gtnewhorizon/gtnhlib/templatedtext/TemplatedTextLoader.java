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
import org.jetbrains.annotations.VisibleForTesting;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.command.ColorPatternCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.CommentCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.DynamicFormatCommand;
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
    public static final String ARG_SPLITTER_PATTERN = ":";
    /** The character used to begin a command invocation. */
    public static final char COMMAND_START = '{';
    /** The character used to end a command invocation. */
    public static final char COMMAND_END = '}';
    /** The prefix for all line processors. */
    public static final String LINE_PROCESSOR_sTART = "#";

    /**
     * A templated text loader that uses gray as it's default text color.<br>
     * Expected asset path: <code>assets/lang/tooltip</code>
     */
    public static final TemplatedTextLoader TOOLTIP_DESCRIPTION = new TemplatedTextLoader("tooltip") {

        {
            // load in all the tooltip guidelines into the tooltip renderer
            TextFormatUtil.TOOLTIPS.getEntries()
                    .forEach(entry -> this.addCommand("color_" + entry.key(), new DynamicFormatCommand(entry.value())));

            // TODO: remove before 2.10 stable
            // spotless:off
            // backwards compatibility with some gt5u pre move
            this.addCommand("color_tier", new DynamicFormatCommand(TextFormatUtil.TOOLTIPS::getUnit));
            this.addCommand("color_hatch", new DynamicFormatCommand(TextFormatUtil.TOOLTIPS::getHatchNotice));
            this.addCommand("color_efficiency", new DynamicFormatCommand(TextFormatUtil.TOOLTIPS::getPowerEfficiency));
            this.addCommand("color_eu_usage", new DynamicFormatCommand(TextFormatUtil.TOOLTIPS::getVoltage));
            this.addCommand("color_eu_capacity", new DynamicFormatCommand(TextFormatUtil.TOOLTIPS::getPowerCapacity));
            // spotless:on
        }

        @Override
        protected String formatLine(String parsedLine) {
            return TextFormatUtil.TOOLTIPS.getDefault() + parsedLine;
        }

    };

    /**
     * A templated text loader that does have any default text color.<br>
     * Expected asset path: <code>assets/lang/text</code>.
     */
    public static final TemplatedTextLoader DEFAULT = new TemplatedTextLoader("text");

    /** Line processors unique to this templated text loader. */
    private final Object2ObjectOpenHashMap<String, ITemplatedTextLineProcessor> lineProcessor = new Object2ObjectOpenHashMap<>();
    /** Commands unique to this templated text loader. */
    private final Object2ObjectOpenHashMap<String, ITemplatedTextCommand> commands = new Object2ObjectOpenHashMap<>();
    /**
     * Used to memoize the results of tooltips lookup since most don't tend to change and not having to constantly
     * reparse the files every frame will save a lot of processing time.
     */
    private final @NotNull ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();

    /**
     *
     */
    private static final Object2ObjectOpenHashMap<@NotNull String, ITemplatedTextLineProcessor> GLOBAL_LINE_PROCESSORS = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<@NotNull String, ITemplatedTextCommand> GLOBAL_COMMANDS = new Object2ObjectOpenHashMap<>();

    /**
     * Used to clear the cache when resources are reloaded.
     */
    private static class ReloadListener {

        @SideOnly(Side.CLIENT)
        private static class Listener implements IResourceManagerReloadListener {

            private static final IResourceManagerReloadListener INSTANCE = new Listener();

            @Override
            public void onResourceManagerReload(IResourceManager manager) {
                INSTANCES.forEach(x -> x.cache.clear());
            }

            private Listener() {}
        }

        private static final ConcurrentSet<TemplatedTextLoader> INSTANCES = new ConcurrentSet<>();

        private ReloadListener() {

        }
    }

    @ApiStatus.Internal
    @SideOnly(Side.CLIENT)
    public static IResourceManagerReloadListener getReloadListener() {
        return ReloadListener.Listener.INSTANCE;
    }

    public static class MarkdownTooltipParsingException extends IllegalStateException {

        public final String[] userLines;

        public static MarkdownTooltipParsingException lineProcessorStandard(String processorName, String message) {
            StringBuilder sb = new StringBuilder();
            appendError(sb, processorName, "ERROR");
            return new MarkdownTooltipParsingException(
                    new String[] { "LINE PROCESSOR ERROR IN " + processorName + ": " + message },
                    new String[] { sb.toString(), message });
        }

        public MarkdownTooltipParsingException(String... lines) {
            super(String.join("\n", lines));
            this.userLines = lines;
        }

        public MarkdownTooltipParsingException(String[] logLines, String[] userLines) {
            super(String.join("\n", logLines));
            this.userLines = userLines;
        }
    }

    public static void addGlobalCommandWithAliases(ITemplatedTextCommand command, String... aliases) {
        for (String alias : aliases) {
            addGlobalCommand(alias, command);
        }
    }

    public static void addGlobalCommand(String name, ITemplatedTextCommand command) {
        GLOBAL_COMMANDS.put(name, command);
    }

    public static void addGlobalLineProcessorWithAliases(ITemplatedTextLineProcessor command, String... aliases) {
        for (String alias : aliases) {
            addGlobalLineProcessor(alias, command);
        }
    }

    public static void addGlobalLineProcessor(String name, ITemplatedTextLineProcessor command) {
        GLOBAL_LINE_PROCESSORS.put(LINE_PROCESSOR_sTART + name, command);
    }

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

    public static void addThroughputVariations(
            @NotNull Object2ObjectOpenHashMap<@NotNull String, @NotNull ITemplatedTextCommand> map,
            @NotNull Supplier<@NotNull String> formatProvider, @NotNull Supplier<@NotNull String> unitFormatProvider,
            @NotNull Supplier<@NotNull String> unitSupplier, @NotNull String... amountUnitVariations) {
        for (Pair<Supplier<String>, String[]> timeVariation : throughputVariations) {
            ITemplatedTextCommand command = new UnitCommand(
                    new NumberFormatUtil.ThroughputUnitFormatter(
                            formatProvider,
                            unitFormatProvider,
                            unitSupplier,
                            timeVariation.getLeft()));
            String[] aliases = new String[amountUnitVariations.length * timeVariation.getRight().length];
            int i = 0;
            for (String timeUnit : timeVariation.getRight()) {
                for (String amountUnit : amountUnitVariations) {
                    aliases[i++] = amountUnit + timeUnit;
                }
            }
            addGlobalCommandWithAliases(command, aliases);
        }
    }

    static {
        // global line processors
        addGlobalLineProcessor("raw", new RawLineProcessor());
        addGlobalLineProcessor("example", new ExampleLineProcessor());
        addGlobalLineProcessorWithAliases(($1, $2, $3, $4, $5) -> {}, "comment", "");
        addGlobalLineProcessorWithAliases(new IncludeLineProcessor(), "include", "import");
        addGlobalLineProcessorWithAliases(new IfLineProcessor(true), "if-true", "ift", "if");
        addGlobalLineProcessorWithAliases(new IfLineProcessor(false), "if-false", "iif", "if-not", "ifn");
        addGlobalLineProcessorWithAliases(
                new IfDefLineProcessor(true),
                "if-exists",
                "ife",
                "if-defined",
                "if-def",
                "ifdef");
        addGlobalLineProcessorWithAliases(
                new IfDefLineProcessor(false),
                "if-not-exists",
                "ifne",
                "if-not-defined",
                "if-ndef",
                "ifndef");

        // coloring commands
        for (EnumChatFormatting format : EnumChatFormatting.values()) {
            addGlobalCommand(format.getFriendlyName(), new StaticFormatCommand(format.toString()));
        }
        addGlobalCommand("color-pattern", new ColorPatternCommand());

        // evaluating vars
        addGlobalCommand("item", new ItemCommand());
        addGlobalCommand("fluid", new FluidCommand());
        addGlobalCommand("var", new VarCommand(false));
        addGlobalCommand("var-eval", new VarCommand(true));
        addGlobalCommand("lang", new LangCommand(false));
        addGlobalCommand("lang-eval", new LangCommand(true));
        addGlobalCommand("isolate", new IsolationCommand());
        // spotless:off
        addGlobalCommandWithAliases(new CommentCommand(), "comment", "#", "//");
        addGlobalCommandWithAliases(new IfCommand(true), "if-true", "if");
        addGlobalCommandWithAliases(new IfCommand(false), "if-false", "if-not");
        addGlobalCommandWithAliases(new IfDefCommand(true), "if-exists", "ife", "if-defined", "if-def", "ifdef");
        addGlobalCommandWithAliases(new IfDefCommand(false), "if-not-exists", "ifne", "if-not-defined", "if-ndef", "ifndef");
        addGlobalCommandWithAliases(new PluralityCommand(true), "singular", "if-1");
        addGlobalCommandWithAliases(new PluralityCommand(false), "plural", "if-n-1");
        for (Map.Entry<String, CLDRPluralRuleParser> kv : CLDRPluralRuleParser.getKnownRules()) {
            String name = kv.getKey().replace("_", "-");
            addGlobalCommandWithAliases(new PluralPresetCommand(kv.getValue(), name.toUpperCase()), "plural-" + name.toLowerCase());
        }
        // spotless:on

        // unit standardization commands
        // spotless:off
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_FLUID), "fluid-unit", "fluid-amount", "l", "L", "liter", "Liter", "mb", "mB");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_RF), "RF", "redstone-flux-unit", "rf");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MJ), "MJ", "mj");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_AE), "AE", "ae");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_EU),"EU", "energy-unit", "eu");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_AMP),"amperage-unit", "amp-unit", "A", "a");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_HEAT), "heat-unit", "K", "k", "Kelvin", "kelvin");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_ACIDITY), "acidity-unit", "ph", "pH", "PH");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_RADIATION), "radiation-unit", "sv", "Sv", "SV");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_TEMPERATURE), "temperature-unit", "C");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_FORCE), "force-unit", "newton-unit", "N", "n");
        // No "ev"/"EV" aliases: EV is also a gregtech voltage tier name.
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_ELECTRON_VOLT), "electron-volt-unit", "eV");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_KEV), "kilo-electron-volt-unit", "kev", "KeV", "KEV");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MEV), "mega-electron-volt-unit", "mev", "MeV", "MEV");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_GEV), "giga-electron-volt-unit", "gev", "GeV", "GEV");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_TEV), "tera-electron-volt-unit", "tev", "TeV", "TEV");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_NANOGRAM), "nanogram-unit", "ng");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MICROGRAM), "microgram-unit", "μg");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MILLIGRAM), "milligram-unit", "mg");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_GRAM), "gram-unit", "g");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_KILOGRAM), "kilometer-unit", "kg");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_NANOMETER), "nanometer-unit", "nm");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MICROMETER), "micrometer-unit", "μm");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MILLIMETER), "millimeter-unit", "mm");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_CENTIMETER), "centimeter-unit", "cm");
        // no m because minutes would be more common
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_METER), "meter-unit");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_KILOMETER), "kilometer-unit", "km", "KM");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_NANOSECOND), "nanosecond-unit", "ns");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MICROSECOND), "microsecond-unit", "μs");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MILLISECOND), "millisecond-unit", "ms");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_TICK), "tick-unit", "t", "T");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_SECOND), "second-unit", "s", "S", "sec", "Sec");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_MINUTE), "minute-unit", "m", "m", "min", "Min");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_HOUR), "hour-unit", "h", "H");
        addGlobalCommandWithAliases(new UnitCommand(NumberFormatUtil.FORMAT_DAY), "day-unit", "d", "D");
        // spotless:on

        // throughput unit standardization commands
        // spotless:off
        NumberFormatUtil.LocalisationCache locs = NumberFormatUtil.LOC_CACHE;
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingFluidThroughput,
                locs::getUnitFormatFluidThroughput,
                NumberFormatUtil::getFluidUnit,
                "fluid/",
                "fluid-unit/",
                "L/",
                "l/",
                "Liter/",
                "liter/",
                "mb/",
                "mB/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingEnergyThroughput,
                locs::getUnitFormatEnergyThroughput,
                locs::getUnitRF,
                "RF/",
                "rf/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingEnergyThroughput,
                locs::getUnitFormatEnergyThroughput,
                locs::getUnitMJ,
                "MJ/",
                "mj/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingEnergyThroughput,
                locs::getUnitFormatEnergyThroughput,
                locs::getUnitAE,
                "AE/",
                "ae/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingEnergyThroughput,
                locs::getUnitFormatEnergyThroughput,
                NumberFormatUtil::getEnergyUnit,
                "energy-unit/",
                "eu/",
                "eu",
                "EU/",
                "EU");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingSpeed,
                locs::getUnitFormatSpeed,
                locs::getUnitMeter,
                "m/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingSpeed,
                locs::getUnitFormatSpeed,
                locs::getUnitKilogram,
                "km/",
                "Km/",
                "KM/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingMassThroughput,
                locs::getUnitFormatMassThroughput,
                locs::getUnitGram,
                "g/");
        addThroughputVariations(
                GLOBAL_COMMANDS,
                locs::getSpacingMassThroughput,
                locs::getUnitFormatMassThroughput,
                locs::getUnitKilogram,
                "kg/",
                "Kg/",
                "KG/");

        addGlobalCommand(
                "throw-err",
                (a, b, c, d) -> { throw new RuntimeException("The crash command was executed!"); });
    }

    private final String style;

    public TemplatedTextLoader(@NotNull String style) {
        this.style = "/" + style + "/";
        ReloadListener.INSTANCES.add(this);
    }

    @VisibleForTesting
    @Override
    public void close() throws Exception {
        ReloadListener.INSTANCES.remove(this);
        this.cache.clear();
    }

    public void addCommandWithAliases(ITemplatedTextCommand command, String... aliases) {
        for (String alias : aliases) {
            addCommand(alias, command);
        }
    }

    public void addCommand(String name, ITemplatedTextCommand command) {
        commands.put(name, command);
    }

    public void addLineProcessorWithAliases(ITemplatedTextLineProcessor processor, String... aliases) {
        for (String alias : aliases) {
            addLineProcessor(alias, processor);
        }
    }

    public void addLineProcessor(String name, ITemplatedTextLineProcessor processor) {
        lineProcessor.put(name, processor);
    }

    private @Nullable ITemplatedTextLineProcessor getLineProcessor(String name) {
        ITemplatedTextLineProcessor ret = GLOBAL_LINE_PROCESSORS.getOrDefault(name, null);
        return ret == null ? lineProcessor.getOrDefault(name, null) : ret;
    }

    private @Nullable ITemplatedTextCommand getCommand(String name) {
        ITemplatedTextCommand ret = GLOBAL_COMMANDS.getOrDefault(name, null);
        return ret == null ? commands.getOrDefault(name, null) : ret;
    }

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

    private ResourceLocation getExactPath(ResourceLocation resource, String language) {
        return new ResourceLocation(
                resource.getResourceDomain(),
                "lang/" + language + this.style + resource.getResourcePath());
    }

    private void createFileLoadError(List<String> ret, Throwable err, ResourceLocation exactPath) {
        StringBuilder sb = new StringBuilder();
        appendError(sb, "FILE-ERROR", "file: " + exactPath + FILE_EXT);
        ret.add(sb.toString());
        for (String errLine : err.getMessage().split("[\r\n]+")) {
            sb.delete(0, sb.length());
            appendError(sb, "FILE-ERROR", errLine);
            ret.add(sb.toString());
        }
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
     * Parses arbitrary lines of text using the templated text processor.
     *
     * @param lines The lines to process
     * @param vars  The variables used to process the lines.
     * @return The parsed lines with processed stacks and the special line formatting if one is specified.
     */
    public List<String> evaluateLines(Iterable<String> lines, Map<String, Object> vars) {
        List<String> out = new LinkedList<>();
        evaluateLinesNoFormat(out, lines, vars, new Stack<>());
        out.replaceAll(parsedLine -> TextFormatUtil.processFormatStacks(formatLine(parsedLine)));
        return out;
    }

    /**
     * Loads and parses a templated text file from the resources without applying the line formatting or the resolving
     * format stacks.
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
                    createFileLoadError(ret, t, exactPath);
                }
                // update the path to the fallback and try to load the fallback file.
                exactPath = this.getExactPath(resource, FALLBACK_LOCALE);
                ret.addAll(this.loadExactPath(exactPath, vars, includeHistory));
                return ret;
            }
        } catch (Throwable t) {
            if (t instanceof FileNotFoundException) {
                StringBuilder sb = new StringBuilder();
                appendError(sb, "FILE-NOT-FOUND", exactPath + FILE_EXT);
                ret.add(sb.toString());
                return ret;
            }
            createFileLoadError(ret, t, exactPath);
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

    /**
     * Parses arbitrary lines of text using the templated text processor without resolving format stacks or appending
     * the special line formatting if one is specified.
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
                if (line.startsWith(LINE_PROCESSOR_sTART)) {
                    String[] parts = line.split(ARG_SPLITTER_PATTERN, 2);
                    if (parts.length < 2) {
                        throw MarkdownTooltipParsingException
                                .lineProcessorStandard(parts.length >= 1 ? parts[0].trim() : "", "NOT ENOUGH ARGS");
                    }
                    ITemplatedTextLineProcessor processor = getLineProcessor(parts[0]);
                    if (processor == null) {
                        throw MarkdownTooltipParsingException
                                .lineProcessorStandard("UNKNOWN LINE PROCESSOR", parts[0].trim());
                    }
                    processor.processLine(this, out, parts[1], vars, includeHistory);
                    continue;
                }
                out.add(eval(line, vars));
            } catch (MarkdownTooltipParsingException ise) {
                for (String errLine : ise.userLines) {
                    out.add(TextFormatUtil.wrapFormatting(EnumChatFormatting.RED, errLine));
                }
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
            public void visitChar(char c) {
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

        void visitChar(char c);

        void visitCommand(String command);
    }

    public static void parseCommands(String unparsed, CommandVisitor visitor) {
        final char[] chars = unparsed.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char curr = chars[i];

            if (curr == ESCAPE_CHAR) {
                if (i < chars.length - 1) {
                    visitor.visitChar(chars[i + 1]);
                    i++;
                }
            } else if (curr == COMMAND_START) {
                int end = i;
                int open = 0;

                while (end < chars.length) {
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
                    throw new MarkdownTooltipParsingException(
                            new String[] { "Unclosed open brace in line:", unparsed, new String(spaces) + "^" },
                            new String[] { sb.toString(), unparsed, unparsed.substring(0, i) + "^" });
                }

                String command = unparsed.substring(i, end + 1);

                if (command.length() <= 2) {
                    throw new MarkdownTooltipParsingException(
                            "Invalid command: '" + command
                                    + "': expected open + close brace that contains the command name and (optional) arguments");
                }

                visitor.visitCommand(command);

                i = end;
            } else {
                visitor.visitChar(chars[i]);
            }
        }
    }

    public static String getCurrentLanguage() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        } else {
            return "en_US";
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

}
