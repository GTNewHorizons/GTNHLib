package com.gtnewhorizon.gtnhlib.templatedtext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;

public class TemplatedTextLoaderTest {

    @Test
    void default_working() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays.asList("hai", EnumChatFormatting.GREEN + "hello");
        List<String> actual = TemplatedTextLoaders.DEFAULT.evaluateLines(text, Collections.emptyMap());
        Assertions.assertLinesMatch(expected, actual);
        assertEquals(String.join(System.lineSeparator(), expected), String.join(System.lineSeparator(), actual));
    }

    @Test
    void tooltip_working() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays.asList(
                TextThemingGuidelines.TOOLTIPS.getDefault() + "hai",
                TextThemingGuidelines.TOOLTIPS.getDefault() + EnumChatFormatting.GREEN + "hello");
        List<String> actual = TemplatedTextLoaders.TOOLTIP.evaluateLines(text, Collections.emptyMap());
        Assertions.assertLinesMatch(expected, actual);
        assertEquals(String.join(System.lineSeparator(), expected), String.join(System.lineSeparator(), actual));
    }

    @Test
    void tooltip_bad_final_symbol() {
        List<String> text = Arrays.asList("{red:x} 100§");
        List<String> expected = Arrays.asList("§cx§r 100");
        List<String> actual = TemplatedTextLoaders.DEFAULT.evaluateLines(text, Collections.emptyMap());
        Assertions.assertLinesMatch(expected, actual);
    }
}
