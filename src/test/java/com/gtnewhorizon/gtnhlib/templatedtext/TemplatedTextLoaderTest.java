package com.gtnewhorizon.gtnhlib.templatedtext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.RawLineProcessor;

public class TemplatedTextLoaderTest {

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
    void tooltip_bad_final_symbol() {
        expectString("{red:x} 100§", "§cx§r 100");
    }

    @Test
    void lineProcessor_global_works() {
        expectString("#raw:this is a {test}", "this is a {test}");
    }

    @Test
    void lineProcessor_local_works() {
        TemplatedTextLoader loader = new TemplatedTextLoader("text");
        loader.addLineProcessor(new RawLineProcessor(), "local-test");
        List<String> text = Arrays.asList("#local-test:this is a {test}");
        List<String> expected = Arrays.asList("this is a {test}");
        List<String> actual = loader.evaluateLines(text);
        Assertions.assertLinesMatch(expected, actual);
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
        expectError(0, "{color-pattern::sometext}");
        // does not error thanks to a quirk of string.split and doesn't cause problems so not counted as an error
        // expectError(0, "{color-pattern:red,:sometext}");
        expectError(0, "{color-pattern:,red:sometext}");
        expectError(0, "{color-pattern:x*red:sometext}");
        expectError(0, "{color-pattern:x*notacolor:sometext}");
        expectError(0, "{color-pattern:1*notacolor}");
        expectError(0, "{color-pattern:ret}");
        expectError(0, "{color-pattern:}");
        expectError(0, "{color-pattern}");
    }

    private static void expectString(String original, String expected) {
        expectString(TemplatedTextLoaders.STANDARD, original, expected);
    }

    private static void expectString(TemplatedTextLoader loader, String original, String expected) {
        String actual = loader.evaluateLines(Arrays.asList(original)).get(0);
        Assertions.assertEquals(expected, actual, "got the wrong value for <" + original + ">");
    }

    private static void expectError(int expectAt, String original) {
        expectError(TemplatedTextLoaders.STANDARD, expectAt, original);
    }

    private static void expectError(TemplatedTextLoader loader, int expectAt, String original) {
        String actual = loader.evaluateLines(Arrays.asList(original)).get(0);
        Matcher matcher = errPattern.matcher(actual);
        Assertions.assertTrue(matcher.find(), "Expected an error in: <" + actual + ">");
        Assertions.assertEquals(
                expectAt,
                matcher.start(),
                "Expected an error at " + expectAt + " in: <" + actual + "> from <" + original + ">");
    }
}
