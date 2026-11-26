package com.gtnewhorizon.gtnhlib.templatedtext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentest4j.AssertionFailedError;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.extensions.DefaultLineSeparator;
import com.gtnewhorizon.gtnhlib.extensions.DisableFormattedNotation;
import com.gtnewhorizon.gtnhlib.extensions.ForceDisplayLocale;
import com.gtnewhorizon.gtnhlib.extensions.ForceFormatLocale;
import com.gtnewhorizon.gtnhlib.extensions.GlobalDisableExponentialNotation;
import com.gtnewhorizon.gtnhlib.extensions.InjectLocalisation;
import com.gtnewhorizon.gtnhlib.extensions.UseDebugFluidUnit;
import com.gtnewhorizon.gtnhlib.extensions.UseMillibuckets;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.RawLineProcessor;
import com.gtnewhorizon.gtnhlib.util.TextFormatConfig;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@DisableFormattedNotation(false)
@GlobalDisableExponentialNotation(false)
@ForceFormatLocale("en-US")
@ForceDisplayLocale("en-US")
@DefaultLineSeparator(style = TextFormatUtil.LineSeparatorStyle.Dashes, length = 41)
public class TemplatedTextLoaderTest {

    @AfterEach
    void afterEach() {
        TemplatedTextLoader.clearCache();
    }

    private static final Pattern errPattern = Pattern.compile("§c\\[([^#]+?)\\s#\\s+([^]]+)]", 0);

    @Test
    void default_working() {
        String def = TextThemingGuidelines.TOOLTIPS.getDefault();
        expectString("this is a test", "this is a test");
        expectString("{green:this} {red:is} a test", "§athis§r §cis§r a test");
    }

    @Test
    void tooltip_working() {
        String def = TextThemingGuidelines.TOOLTIPS.getDefault();
        expectString(TemplatedTextLoaders.TOOLTIP, "this is a test", def + "this is a test");
        expectString(
                TemplatedTextLoaders.TOOLTIP,
                "{green:this} {red:is} a test",
                "§r§athis§r §cis§r a test".replace("§r", def));
    }

    @Test
    void parsing_badFinalFormatCodeIgnoredGracefully() {
        expectString("{red:x} 100§", "§cx§r 100");
    }

    @Test
    void parsing_escapedBrackets() {
        expectString("{red:a\\}b}", "§ca}b");
        expectString("\\{red:a}b}", "{red:a}b}");
        expectString("\\{}", "{}");
        expectString("some text\\{", "some text{");
    }

    @Test
    void parsing_escaping() {
        expectString("\\\\", "\\");
        expectString("\\", "");
        expectError("{red\\:asd}");
        expectString("{plural-en:var\\:int:true:false}", "true", ImmutableMap.of("var:int", 1));

        expectString("{plural-en:var_int:\\{red:true}:false}", "{red:false}", ImmutableMap.of("var_int", 1));
        expectString("{plural-en:var_int:{red:true}:false}", "§ctrue", ImmutableMap.of("var_int", 1));
    }

    @Test
    void lineProcessor_global_works() {
        expectString("#raw:this is a {test}", "this is a {test}");
    }

    @Test
    void lineProcessor_local_works() {
        try (TemplatedTextLoader loader = new TemplatedTextLoader("text")) {
            loader.addLineProcessor(new RawLineProcessor(), "local-test");
            List<String> text = Arrays.asList("#local-test:this is a {test}");
            List<String> expected = Arrays.asList("this is a {test}");
            List<String> actual = loader.evaluateLines(text);
            Assertions.assertLinesMatch(expected, actual);
        }
    }

    @Test
    void command_colorPattern_works() {
        String def = TextThemingGuidelines.TOOLTIPS.getDefault();
        expectString("{color-pattern:red,green:1234}", "§c1§r§a2§r§c3§r§a4");
        expectString(
                TemplatedTextLoaders.TOOLTIP,
                "{color-pattern:red,green:1234}",
                "§r§c1§r§a2§r§c3§r§a4".replace("§r", def));
    }

    @Test
    void command_colorPattern_errorHandling() {
        expectErrorAt(0, "{color-pattern::sometext}");
        // does not error thanks to a quirk of string.split and doesn't cause problems so not counted as an error
        // expectError(0, "{color-pattern:red,:sometext}");
        expectErrorAt(0, "{color-pattern:,red:sometext}");
        expectErrorAt(0, "{color-pattern:x*red:sometext}");
        expectErrorAt(0, "{color-pattern:x*notacolor:sometext}");
        expectErrorAt(0, "{color-pattern:1*notacolor}");
        expectErrorAt(0, "{color-pattern:ret}");
        expectErrorAt(0, "{color-pattern:}");
        expectErrorAt(0, "{color-pattern}");
    }

    @Test
    @InjectLocalisation(key = "templated_text", text = "{red:this is a test}")
    void command_lang_works() {
        // does not evaluate
        expectString("{lang:templated_text}", "{red:this is a test}");
        // ignores args
        expectString("{lang:templated_text:asd}", "{red:this is a test}");
        expectString("{lang:lang.does.not.exist:asd}", "lang.does.not.exist");
    }

    @Test
    @InjectLocalisation(key = "templated_text", text = "{red:this is a test}")
    @InjectLocalisation(key = "text", text = "this is a test")
    void command_langEval_works() {
        // ignores extra args safely
        expectString("{lang-eval:text:one:two}", "this is a test");
        // does evaluate contents after parsing
        expectString("{lang-eval:templated_text:one:two}", "§cthis is a test");

        expectString("{lang-eval:%s %s:one:two}", "one two");
        expectString("{lang-eval:%s %s:one:two}", "one two");
        expectString("{lang-eval:{var:key}:one:two}", "two one", ImmutableMap.of("key", "%2$s %1$s"));
        expectString("{lang-eval:%2$s %1$s:{var:one}:{var:two}}", "2 1", ImmutableMap.of("one", 1, "two", 2));
        expectString(
                "{lang-eval:{var:key}:{var:one}:{var:two}}",
                "2 1",
                ImmutableMap.of("key", "%2$s %1$s", "one", 1, "two", 2));
        expectString(
                "{lang-eval:{var:key}:{var:one}:{var:two}}",
                "§cthis is red",
                ImmutableMap.of("key", "%s%s", "one", "{red:this is red", "two", "}"));
    }

    @Test
    void command_lang_missingKeyHandling() {
        final String missingKey = "gtnhlib.this.key.does.not.exist";
        // falls back to key when not found
        expectString("{lang:" + missingKey + "}", missingKey);
        expectString("{lang:" + missingKey + ":one}", missingKey);
        expectString("{lang:" + missingKey + ":{bad-command}:two}", missingKey);
    }

    @Test
    void command_langEval_missingKeyHandling() {
        final String missingKey = "gtnhlib.this.key.does.not.exist";
        // falls back to key when not found
        expectString("{lang-eval:" + missingKey + "}", missingKey);
        expectString("{lang-eval:" + missingKey + ":one}", missingKey);
        expectString("{lang-eval:" + missingKey + ":{bad-command}:two}", missingKey);
        expectString(
                "{lang-eval:{var:key}:{var:one}:{var:two}}",
                missingKey,
                ImmutableMap.of("key", missingKey, "one", 1, "two", 2));
    }

    @ParameterizedTest
    @EnumSource
    void command_hr_works(TextFormatUtil.LineSeparatorStyle style) {
        TextFormatConfig.lineSeparatorStyle = style;
        expectNoError("{hr}");
        expectNoError("{hr:21}");
    }

    @ParameterizedTest
    @EnumSource
    void command_hr_errorHandling(TextFormatUtil.LineSeparatorStyle style) {
        TextFormatConfig.lineSeparatorStyle = style;
        expectErrorAt(1, "a{hr:a}");
        expectErrorAt(1, "a{hr:-1}");
        expectErrorAt(1, "a{hr:1.0}");
        expectErrorAt(1, "a{hr:1E3}");
    }

    @Test
    @UseDebugFluidUnit(false)
    @UseMillibuckets(true)
    void caching_configAffectsFluidUnits() {
        expectString("{fluid-unit}", "mB");
        expectString("{fluid-unit:amount}", "1 mB", ImmutableMap.of("amount", 1));
        expectString("{fluid-unit/s:amount}", "1 mB/s", ImmutableMap.of("amount", 1));

        NumberFormatConfig.useForgeFluidMillibuckets = false;
        // not pushed so it doesn't update yet
        expectString("{fluid-unit}", "L");
        expectString("{fluid-unit:amount}", "1 L", ImmutableMap.of("amount", 1));
        expectString("{fluid-unit/s:amount}", "1 mB/s", ImmutableMap.of("amount", 1));
        NumberFormatUtil.postConfiguration();
        expectString("{fluid-unit}", "L");
        expectString("{fluid-unit:amount}", "1 L", ImmutableMap.of("amount", 1));
        expectString("{fluid-unit/s:amount}", "1 L/s", ImmutableMap.of("amount", 1));

        NumberFormatConfig.debugFluidUnit = true;
        NumberFormatUtil.postConfiguration();
        expectString("{fluid-unit}", "X");
        expectString("{fluid-unit:amount}", "1 X", ImmutableMap.of("amount", 1));
        expectString("{fluid-unit/s:amount}", "1 X/s", ImmutableMap.of("amount", 1));
    }

    // region expectString

    private static void expectString(String original, String expected) {
        expectString(TemplatedTextLoaders.STANDARD, original, expected, Collections.emptyMap());
    }

    private static void expectString(TemplatedTextLoader loader, String original, String expected) {
        expectString(loader, original, expected, Collections.emptyMap());
    }

    private static void expectString(String original, String expected, Map<String, Object> vars) {
        expectString(TemplatedTextLoaders.STANDARD, original, expected, vars);
    }

    private static void expectString(TemplatedTextLoader loader, String original, String expected,
            Map<String, Object> vars) {
        List<String> lines = loader.evaluateLines(Arrays.asList(original), vars);
        String actual = lines.get(0);
        try {
            Assertions.assertEquals(expected, actual, String.format("got the wrong value for <%s>", original));
        } catch (AssertionFailedError error) {
            System.err.println("full output:");
            for (String line : lines) {
                System.err.println(line);
            }
            throw error;
        }
    }

    // endregion expectString

    // region expectNoError

    private static void expectNoError(String original) {
        expectNoError(TemplatedTextLoaders.STANDARD, original, Collections.emptyMap());
    }

    private static void expectNoError(TemplatedTextLoader loader, String original) {
        expectNoError(loader, original, Collections.emptyMap());
    }

    private static void expectNoError(String original, Map<String, Object> vars) {
        expectNoError(TemplatedTextLoaders.STANDARD, original, vars);
    }

    private static void expectNoError(TemplatedTextLoader loader, String original, Map<String, Object> vars) {
        List<String> lines = loader.evaluateLines(Arrays.asList(original), vars);
        String actual = lines.get(0);
        Matcher matcher = errPattern.matcher(actual);
        try {
            Assertions.assertFalse(
                    matcher.find(),
                    String.format("Expected no error from <%s> but got <%s>", original, actual));
        } catch (AssertionFailedError error) {
            System.err.println("full output:");
            for (String line : lines) {
                System.err.println(line);
            }
            throw error;
        }
    }

    // endregion expectNoError

    // region expectErrorAt

    private static void expectErrorAt(int expectAt, String original) {
        expectErrorAt(TemplatedTextLoaders.STANDARD, expectAt, original, Collections.emptyMap());
    }

    private static void expectErrorAt(TemplatedTextLoader loader, int expectAt, String original) {
        expectErrorAt(loader, expectAt, original, Collections.emptyMap());
    }

    private static void expectErrorAt(int expectAt, String original, Map<String, Object> vars) {
        expectErrorAt(TemplatedTextLoaders.STANDARD, expectAt, original, vars);
    }

    private static void expectErrorAt(TemplatedTextLoader loader, int expectAt, String original,
            Map<String, Object> vars) {
        List<String> lines = loader.evaluateLines(Arrays.asList(original), vars);
        String actual = lines.get(0);
        Matcher matcher = errPattern.matcher(actual);
        try {
            Assertions.assertTrue(
                    matcher.find(),
                    String.format("Expected an error from <%s> but got <%s>", original, actual));
            Assertions.assertEquals(
                    expectAt,
                    matcher.start(),
                    String.format("Expected an error at <%d> in: <%s> from <%s>", expectAt, actual, original));
        } catch (AssertionFailedError error) {
            System.err.println("full output:");
            for (String line : lines) {
                System.err.println(line);
            }
            throw error;
        }
    }

    // endregion expectErrorAt

    // region expectError

    private static void expectError(String original) {
        expectError(TemplatedTextLoaders.STANDARD, original, Collections.emptyMap());
    }

    private static void expectError(TemplatedTextLoader loader, String original) {
        expectError(loader, original, Collections.emptyMap());
    }

    private static void expectError(String original, Map<String, Object> vars) {
        expectError(TemplatedTextLoaders.STANDARD, original, vars);
    }

    private static void expectError(TemplatedTextLoader loader, String original, Map<String, Object> vars) {
        List<String> lines = loader.evaluateLines(Arrays.asList(original), vars);
        String actual = lines.get(0);
        Matcher matcher = errPattern.matcher(actual);
        try {
            Assertions.assertTrue(
                    matcher.find(),
                    String.format("Expected an error from <%s> but got <%s>", original, actual));
        } catch (AssertionFailedError error) {
            System.err.println("full output:");
            for (String line : lines) {
                System.err.println(line);
            }
            throw error;
        }
    }

    // endregion expectError
}
