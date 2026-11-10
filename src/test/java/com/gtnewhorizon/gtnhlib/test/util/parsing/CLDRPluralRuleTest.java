package com.gtnewhorizon.gtnhlib.test.util.parsing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import com.gtnewhorizon.gtnhlib.util.numberformatting.ExponentialFormat;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;

public class CLDRPluralRuleTest {

    protected static FormatOptions getBaseOpts() {
        // spotless:off
        return new FormatOptions()
            .setDecimalPlaces(0, 2)
            .setRoundingMode(RoundingMode.HALF_UP)
            .disableExponentialFormatting()
            .setExponentialThreshold(0)
            .setExponentialFormatOverride(ExponentialFormat.SCIENTIFIC);
        // spotless:on
    }

    protected static FormatOptions getSciOpts() {
        // spotless:off
        return getBaseOpts()
            .enableExponentialFormatting()
            .setExponentialFormatOverride(ExponentialFormat.SCIENTIFIC);
        // spotless:on
    }

    protected static FormatOptions getEngOpts() {
        // spotless:off
        return getBaseOpts()
            .enableExponentialFormatting()
            .setExponentialFormatOverride(ExponentialFormat.ENGINEERING);
        // spotless:on
    }

    protected static FormatOptions getPoTOpts() {
        // spotless:off
        return getBaseOpts()
            .enableExponentialFormatting()
            .setExponentialFormatOverride(ExponentialFormat.POWER_OF_TEN);
        // spotless:on
    }

    protected static CompactOptions getCompactOps() {
        // spotless:off
        return getBaseOpts()
            .setExponentialThreshold(new BigDecimal("1E1000"))
            .toCompactOptions()
            .setCompactThreshold(0);
        // spotless:on
    }

    protected static Stream<Arguments> tryAllExponentialOptions() {
        return Stream.of(
                Arguments.argumentSet("scientific", getSciOpts()),
                Arguments.argumentSet("power_of_ten", getPoTOpts()),
                Arguments.argumentSet("engineering", getEngOpts()));
    }

    protected static Stream<Arguments> tryAllStandardNotationOptions() {
        return Stream.of(
                Arguments.argumentSet("scientific", getSciOpts()),
                Arguments.argumentSet("power_of_ten", getPoTOpts()));
    }

    protected static Stream<Arguments> tryAllOptions() {
        return Stream.of(
                Arguments.argumentSet("base", getBaseOpts()),
                Arguments.argumentSet("scientific", getSciOpts()),
                Arguments.argumentSet("power_of_ten", getPoTOpts()),
                Arguments.argumentSet("engineering", getEngOpts()),
                Arguments.argumentSet("compact", getCompactOps()));
    }
}
