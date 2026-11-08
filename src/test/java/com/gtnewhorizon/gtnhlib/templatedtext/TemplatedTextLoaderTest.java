package com.gtnewhorizon.gtnhlib.templatedtext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.junit.jupiter.api.Test;

public class TemplatedTextLoaderTest {

    @Test
    void TestWorking() {
        List<String> text = Arrays.asList("hai", "{green:hello}");
        List<String> expected = Arrays.asList("§7hai", "§7$ahello");
        List<String> actual = new ArrayList<>();
        TemplatedTextLoader.TOOLTIP_DESCRIPTION.evaluateLines(actual, expected, Collections.emptyMap(), new Stack<>());
        assertEquals(String.join(System.lineSeparator(), expected), String.join(System.lineSeparator(), actual));
    }
}
