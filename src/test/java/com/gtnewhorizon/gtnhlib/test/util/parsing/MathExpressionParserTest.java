package com.gtnewhorizon.gtnhlib.test.util.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.parsing.MathExpressionParser;

class MathExpressionParserTest {

    MathExpressionParser.Context ctxEN = new MathExpressionParser.Context()
            .setNumberFormat(NumberFormat.getNumberInstance(Locale.US));
    MathExpressionParser.Context ctxFR = new MathExpressionParser.Context()
            .setNumberFormat(NumberFormat.getNumberInstance(Locale.FRENCH));
    MathExpressionParser.Context ctxES = new MathExpressionParser.Context()
            .setNumberFormat(NumberFormat.getNumberInstance(Locale.forLanguageTag("ES")));

    @Test
    void NumbersBasic_Test() {
        assertEquals(41, MathExpressionParser.parse("41"));
        assertEquals(42, MathExpressionParser.parse("  42  "));

        assertEquals(1000000, MathExpressionParser.parse("1 000 000"));
        assertEquals(1000000, MathExpressionParser.parse("1_000_000"));

        assertEquals(123456.789, MathExpressionParser.parse("123456.789", ctxEN));
        assertEquals(234567.891, MathExpressionParser.parse("234,567.891", ctxEN));

        assertEquals(345678.912, MathExpressionParser.parse("345 678,912", ctxFR));

        String s = NumberFormat.getNumberInstance(Locale.FRENCH).format(456789.123);
        assertEquals(456789.123, MathExpressionParser.parse(s, ctxFR));

        assertEquals(567891.234, MathExpressionParser.parse("567.891,234", ctxES));
    }

    @Test
    void ArithmeticBasic_Test() {
        assertEquals(5, MathExpressionParser.parse("2+3"));
        assertEquals(-1, MathExpressionParser.parse("2-3"));
        assertEquals(6, MathExpressionParser.parse("2*3"));
        assertEquals(2, MathExpressionParser.parse("6/3"));
        assertEquals(8, MathExpressionParser.parse("2^3"));
    }

    @Test
    void UnaryMinus_Test() {
        assertEquals(-5, MathExpressionParser.parse("-5"));
        assertEquals(-3, MathExpressionParser.parse("-5+2"));
        assertEquals(-7, MathExpressionParser.parse("-5-2"));
        assertEquals(-15, MathExpressionParser.parse("-5*3"));
        assertEquals(-2.5, MathExpressionParser.parse("-5/2"));
        assertEquals(-25, MathExpressionParser.parse("-5^2")); // ! this is -(5^2), not (-5)^2.

        assertEquals(16, MathExpressionParser.parse("(-4)^2"));
        assertEquals(-64, MathExpressionParser.parse("(-4)^3"));

        assertEquals(2, MathExpressionParser.parse("4+-2"));
        assertEquals(6, MathExpressionParser.parse("4--2"));

        assertEquals(7, MathExpressionParser.parse("--7"));
        assertEquals(-8, MathExpressionParser.parse("---8"));
    }

    @Test
    void UnaryPlus_Test() {
        assertEquals(5, MathExpressionParser.parse("+5"));
        assertEquals(7, MathExpressionParser.parse("+5+2"));
        assertEquals(3, MathExpressionParser.parse("+5-2"));
        assertEquals(15, MathExpressionParser.parse("+5*3"));
        assertEquals(2.5, MathExpressionParser.parse("+5/2"));
        assertEquals(25, MathExpressionParser.parse("+5^2"));

        assertEquals(6, MathExpressionParser.parse("4++2"));
        assertEquals(2, MathExpressionParser.parse("4-+2"));

        assertEquals(7, MathExpressionParser.parse("++7"));
        assertEquals(8, MathExpressionParser.parse("+++8"));
    }

    @Test
    void ArithmeticPriority_Test() {
        assertEquals(4, MathExpressionParser.parse("2+3-1"));
        assertEquals(14, MathExpressionParser.parse("2+3*4"));
        assertEquals(10, MathExpressionParser.parse("2*3+4"));
        assertEquals(7, MathExpressionParser.parse("2^3-1"));
        assertEquals(13, MathExpressionParser.parse("1+2^3+4"));

        // a^b^c = a^(b^c)
        assertEquals(262_144, MathExpressionParser.parse("4^3^2"));
    }

    @Test
    void Brackets_Test() {
        assertEquals(5, MathExpressionParser.parse("(2+3)"));
        assertEquals(20, MathExpressionParser.parse("(2+3)*4"));
        assertEquals(14, MathExpressionParser.parse("2+(3*4)"));
        assertEquals(42, MathExpressionParser.parse("(((42)))"));

        assertEquals(14, MathExpressionParser.parse("2(3+4)"));
    }

    @Test
    void ScientificBasic_Test() {
        assertEquals(2000, MathExpressionParser.parse("2e3"));
        assertEquals(3000, MathExpressionParser.parse("3E3"));
        assertEquals(0.04, MathExpressionParser.parse("4e-2"));
        assertEquals(0.05, MathExpressionParser.parse("5E-2"));
        assertEquals(6000, MathExpressionParser.parse("6e+3"));

        assertEquals(6000, MathExpressionParser.parse("6 e 3"));
        assertEquals(7800, MathExpressionParser.parse("7.8e3"));
        assertEquals(90_000, MathExpressionParser.parse("900e2"));
        assertEquals(1, MathExpressionParser.parse("1e0"));
    }

    @Test
    void ScientificArithmetic_Test() {
        assertEquals(4000, MathExpressionParser.parse("2*2e3"));
        assertEquals(6000, MathExpressionParser.parse("2e3 * 3"));
        assertEquals(-200, MathExpressionParser.parse("-2e2"));
        assertEquals(1024, MathExpressionParser.parse("2^1e1"));

        // Not supported, but shouldn't fail. (2e2)e2 = 200e2 = 20_000.
        assertEquals(20_000, MathExpressionParser.parse("2e2e2"));
    }

    @Test
    void SuffixesBasic_Test() {
        assertEquals(2000, MathExpressionParser.parse("2k"));
        assertEquals(3000, MathExpressionParser.parse("3K"));
        assertEquals(4_000_000, MathExpressionParser.parse("4m"));
        assertEquals(5_000_000, MathExpressionParser.parse("5M"));
        assertEquals(6_000_000_000D, MathExpressionParser.parse("6b"));
        assertEquals(7_000_000_000D, MathExpressionParser.parse("7B"));
        assertEquals(8_000_000_000D, MathExpressionParser.parse("8g"));
        assertEquals(9_000_000_000D, MathExpressionParser.parse("9G"));
        assertEquals(10_000_000_000_000D, MathExpressionParser.parse("10t"));
        assertEquals(11_000_000_000_000D, MathExpressionParser.parse("11T"));

        assertEquals(2050, MathExpressionParser.parse("2.05k", ctxEN));
        assertEquals(50, MathExpressionParser.parse("0.05k", ctxEN));
        assertEquals(3000, MathExpressionParser.parse("3 k"));
    }

    @Test
    void SuffixesArithmetic_Test() {
        assertEquals(2005, MathExpressionParser.parse("2k+5"));
        assertEquals(2005, MathExpressionParser.parse("5+2k"));
        assertEquals(4000, MathExpressionParser.parse("2k*2"));
        assertEquals(4000, MathExpressionParser.parse("2*2k"));
        assertEquals(-2000, MathExpressionParser.parse("-2k"));

        assertEquals(3_000_000, MathExpressionParser.parse("3kk"));
        assertEquals(4_000_000_000D, MathExpressionParser.parse("4kkk"));

        // Not supported, but shouldn't fail.
        assertEquals(6_000_000_000d, MathExpressionParser.parse("6km"));
        assertEquals(500_000, MathExpressionParser.parse("0.5ke3", ctxEN));

        // Please don't do this.
        assertEquals(20_000_000_000D, MathExpressionParser.parse("2e0.01k", ctxEN));
    }

    @Test
    void Percent_Test() {
        ctxEN.setHundredPercent(1000);

        assertEquals(100, MathExpressionParser.parse("10%", ctxEN));
        assertEquals(2000, MathExpressionParser.parse("200%", ctxEN));
        assertEquals(-300, MathExpressionParser.parse("-30%", ctxEN));

        assertEquals(450, MathExpressionParser.parse("40% + 50", ctxEN));
        assertEquals(500, MathExpressionParser.parse("(20+30)%", ctxEN));
    }
}
