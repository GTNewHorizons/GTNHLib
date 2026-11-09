package com.gtnewhorizon.gtnhlib.templatedtext;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor.RawLineProcessor;

public class TemplatedTextLoaderTest {

    @Test
    void default_working() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays.asList("hai", EnumChatFormatting.GREEN + "hello");
        List<String> actual = TemplatedTextLoaders.DEFAULT.evaluateLines(text);
        Assertions.assertLinesMatch(expected, actual);
    }

    @Test
    void tooltip_working() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays.asList(
                TextThemingGuidelines.TOOLTIPS.getDefault() + "hai",
                TextThemingGuidelines.TOOLTIPS.getDefault() + EnumChatFormatting.GREEN + "hello");
        List<String> actual = TemplatedTextLoaders.TOOLTIP.evaluateLines(text);
        Assertions.assertLinesMatch(expected, actual);
    }

    @Test
    void tooltip_bad_final_symbol() {
        List<String> text = Arrays.asList("{red:x} 100§");
        List<String> expected = Arrays.asList("§cx§r 100");
        List<String> actual = TemplatedTextLoaders.DEFAULT.evaluateLines(text);
        Assertions.assertLinesMatch(expected, actual);
    }

    @Test
    void lineProcessor_global_works() {
        List<String> text = Arrays.asList("#raw:this is a {test}");
        List<String> expected = Arrays.asList("this is a {test}");
        List<String> actual = TemplatedTextLoaders.DEFAULT.evaluateLines(text);
        Assertions.assertLinesMatch(expected, actual);
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
}
