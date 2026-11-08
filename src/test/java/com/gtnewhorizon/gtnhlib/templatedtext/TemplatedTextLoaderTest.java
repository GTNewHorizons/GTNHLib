package com.gtnewhorizon.gtnhlib.templatedtext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

public class TemplatedTextLoaderTest {

    @Test
    void default_working() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays.asList("hai", "§ahello");
        List<String> actual = TemplatedTextLoader.DEFAULT.evaluateLines(text, Collections.emptyMap());
        Assertions.assertLinesMatch(expected, actual);
        assertEquals(String.join(System.lineSeparator(), expected), String.join(System.lineSeparator(), actual));
    }

    @Test
    void tooltip_working() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays
                .asList(TextFormatUtil.TOOLTIPS.getDefault() + "hai", TextFormatUtil.TOOLTIPS.getDefault() + "§ahello");
        List<String> actual = TemplatedTextLoader.TOOLTIP_DESCRIPTION.evaluateLines(text, Collections.emptyMap());
        Assertions.assertLinesMatch(expected, actual);
        assertEquals(String.join(System.lineSeparator(), expected), String.join(System.lineSeparator(), actual));
    }
}
