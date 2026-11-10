package com.gtnewhorizon.gtnhlib.test.util.parsing;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.extensions.DisableFormattedNotation;
import com.gtnewhorizon.gtnhlib.extensions.ForceDisplayLocale;
import com.gtnewhorizon.gtnhlib.extensions.ForceFormatLocale;
import com.gtnewhorizon.gtnhlib.extensions.GlobalDisableExponentialNotation;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleNumber;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleParser;

@DisableFormattedNotation(false)
@GlobalDisableExponentialNotation(false)
@ForceFormatLocale("en-US")
@ForceDisplayLocale("en-US")
public class CLDRPluralRuleParserTest {

    @Test
    void getIndex_testSpanish() {
        FormatOptions opts = new FormatOptions();
        opts.setDecimalPlaces(0, 2);
        opts.setExponentialThreshold(BD_TRILLION);
        opts.enableExponentialFormatting();
        opts.setRoundingMode(RoundingMode.HALF_UP);

        CLDRPluralRuleParser parser = CLDRPluralRuleParser.fromCode("es");
        assertNotNull(parser, "spanish parser not found!");

        assertEquals(0, parser.getIndex(num(1, opts)));
        opts.setDecimalPlaces(1, 2);
        assertEquals(0, parser.getIndex(num(1, opts)));
        opts.setDecimalPlaces(2, 2);
        assertEquals(0, parser.getIndex(num(1, opts)));
        opts.setDecimalPlaces(0, 0);

        // many
        // 1000 dias
        assertEquals(1, parser.getIndex(num(1000000, opts)));
        opts.setExponentialThreshold(BigDecimal.ZERO);
        assertEquals(1, parser.getIndex(num(1e6, opts)));
        assertEquals(1, parser.getIndex(num(2e6, opts)));
        assertEquals(1, parser.getIndex(num(3e6, opts)));
        assertEquals(1, parser.getIndex(num(4e6, opts)));
        assertEquals(1, parser.getIndex(num(5e6, opts)));
        assertEquals(1, parser.getIndex(num(6e6, opts)));
        assertEquals(1, parser.getIndex(num(1.0000001e6, opts)));
        assertEquals(1, parser.getIndex(num(1.1e6, opts)));
        assertEquals(1, parser.getIndex(num(2.0000001e6, opts)));
        assertEquals(1, parser.getIndex(num(2.1e6, opts)));
        assertEquals(1, parser.getIndex(num(3.0000001e6, opts)));
        assertEquals(1, parser.getIndex(num(3.1e6, opts)));
        opts.setExponentialThreshold(BD_TRILLION);

        // others
        // 1,000,000 de dias
        assertEquals(2, parser.getIndex(num(0, opts)));
        assertEquals(2, parser.getIndex(num(2, opts)));
        assertEquals(2, parser.getIndex(num(16, opts)));
        assertEquals(2, parser.getIndex(num(100, opts)));
        assertEquals(2, parser.getIndex(num(1000, opts)));
        assertEquals(2, parser.getIndex(num(10000, opts)));
        assertEquals(2, parser.getIndex(num(100000, opts)));
        opts.setExponentialThreshold(BigDecimal.ZERO);
        assertEquals(2, parser.getIndex(num(1e3, opts)));
        assertEquals(2, parser.getIndex(num(2e3, opts)));
        assertEquals(2, parser.getIndex(num(3e3, opts)));
        assertEquals(2, parser.getIndex(num(4e3, opts)));
        assertEquals(2, parser.getIndex(num(5e3, opts)));
        assertEquals(2, parser.getIndex(num(6e3, opts)));
        opts.setExponentialThreshold(BD_TRILLION);
        opts.setDecimalPlaces(1, 2);
        assertEquals(2, parser.getIndex(num(0.0, opts)));
        opts.setDecimalPlaces(0, 2);
        assertEquals(2, parser.getIndex(num(0.9, opts)));
        assertEquals(2, parser.getIndex(num(1.1, opts)));
        assertEquals(2, parser.getIndex(num(1.6, opts)));
        opts.setDecimalPlaces(1, 2);
        assertEquals(2, parser.getIndex(num(10.0, opts)));
        assertEquals(2, parser.getIndex(num(100.0, opts)));
        assertEquals(2, parser.getIndex(num(1000.0, opts)));
        assertEquals(2, parser.getIndex(num(10000.0, opts)));
        assertEquals(2, parser.getIndex(num(100000.0, opts)));
        assertEquals(2, parser.getIndex(num(1000000.0, opts)));
        opts.setDecimalPlaces(0, 2);
        opts.setExponentialThreshold(BigDecimal.ZERO);
        assertEquals(2, parser.getIndex(num(1.0001e3, opts)));
        assertEquals(2, parser.getIndex(num(1.1e3, opts)));
        assertEquals(2, parser.getIndex(num(2.0001e3, opts)));
        assertEquals(2, parser.getIndex(num(2.1e3, opts)));
        assertEquals(2, parser.getIndex(num(3.0001e3, opts)));
        assertEquals(2, parser.getIndex(num(3.1e3, opts)));
        opts.setExponentialThreshold(BD_TRILLION);
    }

    @Test
    void getIndex_testRussian() {
        FormatOptions opts = new FormatOptions();
        opts.setDecimalPlaces(0, 2);
        opts.setExponentialThreshold(BD_TRILLION);
        opts.enableExponentialFormatting();
        opts.setRoundingMode(RoundingMode.HALF_UP);

        CLDRPluralRuleParser parser = CLDRPluralRuleParser.fromCode("ru");
        assertNotNull(parser, "russian parser not found!");

        // few
        assertEquals(0, parser.getIndex(num(1, opts)));
        assertEquals(0, parser.getIndex(num(21, opts)));
        assertEquals(0, parser.getIndex(num(31, opts)));
        assertEquals(0, parser.getIndex(num(41, opts)));
        assertEquals(0, parser.getIndex(num(51, opts)));
        assertEquals(0, parser.getIndex(num(61, opts)));
        assertEquals(0, parser.getIndex(num(71, opts)));
        assertEquals(0, parser.getIndex(num(81, opts)));
        assertEquals(0, parser.getIndex(num(101, opts)));
        assertEquals(0, parser.getIndex(num(1001, opts)));

        // many
        assertEquals(1, parser.getIndex(num(2, opts)));
        assertEquals(1, parser.getIndex(num(4, opts)));
        assertEquals(1, parser.getIndex(num(22, opts)));
        assertEquals(1, parser.getIndex(num(24, opts)));
        assertEquals(1, parser.getIndex(num(32, opts)));
        assertEquals(1, parser.getIndex(num(34, opts)));
        assertEquals(1, parser.getIndex(num(42, opts)));
        assertEquals(1, parser.getIndex(num(44, opts)));
        assertEquals(1, parser.getIndex(num(52, opts)));
        assertEquals(1, parser.getIndex(num(54, opts)));
        assertEquals(1, parser.getIndex(num(62, opts)));
        assertEquals(1, parser.getIndex(num(102, opts)));
        assertEquals(1, parser.getIndex(num(1002, opts)));

        // other
        assertEquals(2, parser.getIndex(num(0, opts)));
        assertEquals(2, parser.getIndex(num(5, opts)));
        assertEquals(2, parser.getIndex(num(19, opts)));
        assertEquals(2, parser.getIndex(num(100, opts)));
        assertEquals(2, parser.getIndex(num(1000, opts)));
        assertEquals(2, parser.getIndex(num(10000, opts)));
        assertEquals(2, parser.getIndex(num(100000, opts)));
        assertEquals(2, parser.getIndex(num(1000000, opts)));

        opts.setDecimalPlaces(1, 2);
        assertEquals(3, parser.getIndex(num(0.0, opts)));
        assertEquals(3, parser.getIndex(num(1.5, opts)));
        assertEquals(3, parser.getIndex(num(10.0, opts)));
        assertEquals(3, parser.getIndex(num(100.0, opts)));
        assertEquals(3, parser.getIndex(num(1000.0, opts)));
        assertEquals(3, parser.getIndex(num(10000.0, opts)));
        assertEquals(3, parser.getIndex(num(100000.0, opts)));
        assertEquals(3, parser.getIndex(num(1000000.0, opts)));
    }

    @Test
    void rules_inRelation_onlyAcceptsInts() {
        FormatOptions opts = new FormatOptions();
        opts.setDecimalPlaces(2, 2);
        opts.setExponentialThreshold(BD_TRILLION);
        opts.enableExponentialFormatting();
        opts.setRoundingMode(RoundingMode.HALF_UP);

        CLDRPluralRuleParser parser = CLDRPluralRuleParser.getParser("n = 1..10");
        assertEquals(1, parser.getIndex(num(5.3123, opts)));
        assertEquals(0, parser.getIndex(num(1, opts)));
        assertEquals(0, parser.getIndex(num(10, opts)));
        opts.setDecimalPlaces(0, 0);
        assertEquals(0, parser.getIndex(num(5.1231, opts)));
    }

    @Test
    void rules_inRelation_onlyAcceptsInside() {
        FormatOptions opts = new FormatOptions();
        opts.setDecimalPlaces(2, 2);
        opts.setExponentialThreshold(BD_TRILLION);
        opts.enableExponentialFormatting();
        opts.setRoundingMode(RoundingMode.HALF_UP);

        CLDRPluralRuleParser parser = CLDRPluralRuleParser.getParser("n = 1..10");
        assertEquals(0, parser.getIndex(num(1, opts)));
        assertEquals(0, parser.getIndex(num(5, opts)));
        assertEquals(0, parser.getIndex(num(10, opts)));
        opts.setDecimalPlaces(0, 0);
        assertEquals(1, parser.getIndex(num(10.99, opts)));
        assertEquals(1, parser.getIndex(num(0.12, opts)));
        opts.setRoundingMode(RoundingMode.FLOOR);
        assertEquals(0, parser.getIndex(num(10.99, opts)));
        opts.setRoundingMode(RoundingMode.CEILING);
        assertEquals(0, parser.getIndex(num(0.12, opts)));
    }

    private CLDRPluralRuleNumber num(Number value, NumberOptionsBase<?> options) {
        return new CLDRPluralRuleNumber(value, options);
    }

}
