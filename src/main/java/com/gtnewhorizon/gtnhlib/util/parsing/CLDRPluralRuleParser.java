package com.gtnewhorizon.gtnhlib.util.parsing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/// An implementation of the UNICODE CLDR plurality rules.
///
/// See https://www.unicode.org/cldr/charts/43/supplemental/language_plural_rules.html for more details on the rule
/// formatting.
///
/// See https://github.com/unicode-org/cldr/blob/main/common/supplemental/plurals.xml for existing specification from
/// the UNICODE consortium
public class CLDRPluralRuleParser {

    private static final @NotNull String UNICODE_WHITESPACE_PATTERN = "[\\u0020\\u200E\\u200F\\u0009-\\u000D\\u0085\\u2028\\u2029]+";

    // memoization cache for complex parsing rules
    private static final @NotNull Object2ObjectOpenHashMap<@NotNull String, @NotNull CLDRPluralRuleParser> PARSER_CACHE = new Object2ObjectOpenHashMap<>();
    private static final @NotNull Object2ObjectOpenHashMap<@NotNull String, @NotNull Predicate<CLDRPluralRuleNumber>> PREDICATE_CACHE = new Object2ObjectOpenHashMap<>();

    private static final @NotNull Map<@NotNull String, @NotNull CLDRPluralRuleParser> KNOWN_LOCALES = createKnownLocales();

    public static final @NotNull CLDRPluralRuleParser ENGLISH = getParser("i = 1 and v = 0");

    /**
     * @param code one of the codes featured on this page:
     *             https://www.unicode.org/cldr/charts/43/supplemental/language_plural_rules.html
     * @return The parser if there is a known ruleset for the given code else null.
     */
    public static @Nullable CLDRPluralRuleParser fromCode(@NotNull String code) {
        return KNOWN_LOCALES.get(code);
    }

    public static @NotNull Set<Map.Entry<@NotNull String, @NotNull CLDRPluralRuleParser>> getKnownRules() {
        return KNOWN_LOCALES.entrySet();
    }

    /**
     * @param code one of the codes featured on this page:
     *             https://www.unicode.org/cldr/charts/43/supplemental/language_plural_rules.html
     * @return True if the code corresponds to a known ruleset.
     */
    public static boolean isKnownCode(@NotNull String code) {
        return KNOWN_LOCALES.containsKey(code);
    }

    public static @NotNull CLDRPluralRuleParser getParser(@NotNull String @NotNull... parts) {
        String key = Arrays.stream(parts).map(String::trim).collect(Collectors.joining(":"));
        return PARSER_CACHE.computeIfAbsent(key, (k) -> new CLDRPluralRuleParser(parts));
    }

    private @NotNull final ArrayList<Predicate<CLDRPluralRuleNumber>> predicates;

    private CLDRPluralRuleParser(@NotNull String @NotNull [] rules) {
        this.predicates = new ArrayList<>(rules.length);
        for (String rule : rules) {
            this.predicates.add(PREDICATE_CACHE.computeIfAbsent(rule, (k) -> parse(rule.trim())));
        }
    }

    public int getRuleCount() {
        return predicates.size();
    }

    public int getIndex(CLDRPluralRuleNumber container) {
        if (container.v < 0) return this.getRuleCount() - 1;
        int i = 0;
        for (; i < predicates.size(); i++) {
            if (predicates.get(i).test(container)) return i;
        }
        return i;
    }

    private static @NotNull LinkedList<@NotNull String> tokenize(@NotNull String toParse) {
        LinkedList<String> tokens = new LinkedList<>();
        for (final String token : toParse.split(UNICODE_WHITESPACE_PATTERN)) {
            tokens.addLast(token);
        }
        return tokens;
    }

    public static class PluralityRuleParsingException extends IllegalStateException {

        public PluralityRuleParsingException(String message, MutableInt counter) {
            super(message + " @ token " + counter.toInteger());
        }
    }

    public static @NotNull Predicate<CLDRPluralRuleNumber> parse(@NotNull String toParse) {

        return parse(tokenize(toParse).iterator(), new MutableInt(0));
    }

    private static @NotNull Predicate<CLDRPluralRuleNumber> parse(@NotNull Iterator<@NotNull String> iter,
            @NotNull MutableInt counter) {
        // parse or conditions
        if (!iter.hasNext()) {
            throw new PluralityRuleParsingException("Rule is empty", counter);
        }
        String token;
        // and binds tightly so:
        // a and b or c or d and e and f
        // (a and b) or (c) or (d and e and f)
        Predicate<CLDRPluralRuleNumber> orPredicate = null;
        do {
            Predicate<CLDRPluralRuleNumber> andPredicate = null;
            do {
                token = iter.next();
                counter.increment();
                Predicate<CLDRPluralRuleNumber> relation = switch (token) {
                    case "n" -> new PluralityNRelation(iter, counter);
                    case "i" -> new PluralityIRelation(iter, counter);
                    case "f" -> new PluralityFRelation(iter, counter);
                    case "t" -> new PluralityTRelation(iter, counter);
                    case "v" -> new PluralityVRelation(iter, counter);
                    case "w" -> new PluralityWRelation(iter, counter);
                    case "c", "e" -> new PluralityCRelation(iter, counter);
                    default -> throw new PluralityRuleParsingException(
                            "Unexpected relation operand: " + token,
                            counter);
                };

                andPredicate = andPredicate == null ? relation : andPredicate.and(relation);
                if (!iter.hasNext()) {
                    break;
                }
                token = iter.next();
                counter.increment();
            } while (token.equals("and"));
            orPredicate = orPredicate == null ? andPredicate : orPredicate.or(andPredicate);
            if (!iter.hasNext()) {
                break;
            }
        } while (token.equals("or"));
        return orPredicate;
    }

    public static abstract class PluralityRelation<T extends Comparable<T>> implements Predicate<CLDRPluralRuleNumber> {

        public class ValueRange {

            public final T min;
            public final T max;

            private ValueRange(T min, T max) {
                this.min = min;
                this.max = max;
            }
        }

        private final @NotNull Function<@NotNull CLDRPluralRuleNumber, @NotNull T> valueGetter;
        private final @Nullable T modulo;
        private final boolean wantsTrue;
        private final @NotNull ImmutableList<T> individualValues;
        private final @NotNull ImmutableList<ValueRange> rangeValues;

        public PluralityRelation(@NotNull Function<@NotNull CLDRPluralRuleNumber, @NotNull T> valueGetter,
                @NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            this.valueGetter = valueGetter;
            if (!iter.hasNext()) {
                throw new PluralityRuleParsingException("Relation missing equality or modulo operator", counter);
            }

            // parse modulo
            String operand = iter.next();
            counter.increment();
            if (operand.equals("%") || operand.equals("mod")) {
                if (!iter.hasNext()) {
                    throw new PluralityRuleParsingException("Relation modulo value not found", counter);
                }
                // parse modulo value
                String moduloValue = iter.next();
                counter.increment();
                this.modulo = this.parseNumberInternal(moduloValue, counter, true);
                // try load equality operator
                if (!iter.hasNext()) {
                    throw new PluralityRuleParsingException("Relation missing equality operator", counter);
                }
                operand = iter.next();
                counter.increment();
            } else {
                this.modulo = null;
            }

            // parse equality operator
            // not fully implementing all 3 relationship types since they can all be and are generalized into one format
            // value % mod_value (=|!=) value,rmin..rmax
            if (operand.equals("=")) {
                this.wantsTrue = true;
            } else if (operand.equals("!=")) {
                this.wantsTrue = false;
            } else {
                throw new PluralityRuleParsingException("Expected \"=\" or \"!=\" got: " + operand, counter);
            }

            // parse values
            if (!iter.hasNext()) {
                throw new PluralityRuleParsingException("Relation missing equality value", counter);
            }
            var individualValues = ImmutableList.<T>builder();
            var rangeValues = ImmutableList.<ValueRange>builder();
            for (String valueSection : iter.next().split(",")) {
                int splitIndex = valueSection.indexOf("..");
                if (splitIndex >= 0) {
                    // the internal will catch empty ranges
                    rangeValues.add(
                            new ValueRange(
                                    this.parseNumberInternal(valueSection.substring(0, splitIndex), counter, false),
                                    this.parseNumberInternal(valueSection.substring(splitIndex + 2), counter, false)));
                } else {
                    individualValues.add(this.parseNumberInternal(valueSection, counter, false));
                }
            }
            this.individualValues = individualValues.build();
            this.rangeValues = rangeValues.build();
        }

        private @NotNull T parseNumberInternal(@NotNull String unparsed, @NotNull MutableInt counter,
                boolean isModulo) {
            // check if valid
            if (unparsed.isEmpty()) {
                throw new PluralityRuleParsingException("Numeric value is empty!", counter);
            }
            for (final char c : unparsed.toCharArray()) {
                if (c < '0' || c > '9') {
                    throw new PluralityRuleParsingException("Invalid char in number: " + c, counter);
                }
            }
            try {
                return parseNumber(unparsed, counter, isModulo);
            } catch (NumberFormatException nfe) {
                throw new PluralityRuleParsingException("Failed to parse numeric value: " + unparsed, counter);
            }
        }

        protected abstract @NotNull T parseNumber(@NotNull String unparsed, @NotNull MutableInt counter,
                boolean isModulo);

        protected abstract @NotNull T applyModulo(@NotNull T value, @NotNull T modulo);

        @Override
        public boolean test(CLDRPluralRuleNumber numberContainer) {

            return execute(numberContainer) == this.wantsTrue;
        }

        public boolean execute(CLDRPluralRuleNumber container) {
            T value = this.valueGetter.apply(container);
            if (this.modulo != null) {
                value = this.applyModulo(value, this.modulo);
            }
            // check individual values
            for (T other : this.individualValues) {
                if (other.equals(value)) return true;
            }
            // check individual values
            for (ValueRange range : this.rangeValues) {
                if (value.compareTo(range.min) < 0) continue;
                if (value.compareTo(range.max) > 0) continue;
                return true;
            }
            return false;
        }
    }

    public static class PluralityBigDecimalRelation extends PluralityRelation<BigDecimal> {

        public PluralityBigDecimalRelation(
                @NotNull Function<@NotNull CLDRPluralRuleNumber, @NotNull BigDecimal> valueGetter,
                @NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(valueGetter, iter, counter);
        }

        @Override
        protected @NotNull BigDecimal parseNumber(@NotNull String unparsed, @NotNull MutableInt counter,
                boolean isModulo) throws NumberFormatException {
            BigDecimal ret = new BigDecimal(unparsed);
            if (isModulo && ret.signum() <= 0) {
                throw new PluralityRuleParsingException("Modulo value cannot be zero: " + unparsed, counter);
            }
            return ret;
        }

        @Override
        protected @NotNull BigDecimal applyModulo(@NotNull BigDecimal value, @NotNull BigDecimal modulo) {
            return value.remainder(modulo);
        }
    }

    public static class PluralityBigIntegerRelation extends PluralityRelation<BigInteger> {

        public PluralityBigIntegerRelation(
                @NotNull Function<@NotNull CLDRPluralRuleNumber, @NotNull BigInteger> valueGetter,
                @NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(valueGetter, iter, counter);
        }

        @Override
        protected @NotNull BigInteger parseNumber(@NotNull String unparsed, @NotNull MutableInt counter,
                boolean isModulo) throws NumberFormatException {
            BigInteger ret = new BigInteger(unparsed);
            if (isModulo && ret.signum() <= 0) {
                throw new PluralityRuleParsingException("Modulo value cannot be zero: " + unparsed, counter);
            }
            return ret;
        }

        @Override
        protected @NotNull BigInteger applyModulo(@NotNull BigInteger value, @NotNull BigInteger modulo) {
            return value.remainder(modulo);
        }
    }

    public static class PluralityIntRelation extends PluralityRelation<Integer> {

        public PluralityIntRelation(@NotNull Function<@NotNull CLDRPluralRuleNumber, @NotNull Integer> valueGetter,
                @NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(valueGetter, iter, counter);
        }

        @Override
        protected @NotNull Integer parseNumber(@NotNull String unparsed, @NotNull MutableInt counter, boolean isModulo)
                throws NumberFormatException {
            int ret = Integer.parseInt(unparsed);
            if (isModulo && ret <= 0) {
                throw new PluralityRuleParsingException("Modulo value cannot be zero: " + unparsed, counter);
            }
            return ret;
        }

        @Override
        protected @NotNull Integer applyModulo(@NotNull Integer value, @NotNull Integer modulo) {
            return value % modulo;
        }
    }

    private static class PluralityNRelation extends PluralityBigDecimalRelation {

        public PluralityNRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityNRelation::getValue, iter, counter);
        }

        private static @NotNull BigDecimal getValue(CLDRPluralRuleNumber container) {
            return container.n;
        }
    }

    private static class PluralityIRelation extends PluralityBigIntegerRelation {

        public PluralityIRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityIRelation::getValue, iter, counter);
        }

        private static @NotNull BigInteger getValue(CLDRPluralRuleNumber container) {
            return container.i;
        }
    }

    private static class PluralityVRelation extends PluralityIntRelation {

        public PluralityVRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityVRelation::getValue, iter, counter);
        }

        private static @NotNull Integer getValue(CLDRPluralRuleNumber container) {
            return container.v;
        }
    }

    private static class PluralityWRelation extends PluralityIntRelation {

        public PluralityWRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityWRelation::getValue, iter, counter);
        }

        private static @NotNull Integer getValue(CLDRPluralRuleNumber container) {
            return container.w;
        }
    }

    private static class PluralityFRelation extends PluralityBigIntegerRelation {

        public PluralityFRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityFRelation::getValue, iter, counter);
        }

        private static @NotNull BigInteger getValue(CLDRPluralRuleNumber container) {
            return container.f;
        }
    }

    private static class PluralityTRelation extends PluralityBigIntegerRelation {

        public PluralityTRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityTRelation::getValue, iter, counter);
        }

        private static @NotNull BigInteger getValue(CLDRPluralRuleNumber container) {
            return container.t;
        }
    }

    private static class PluralityCRelation extends PluralityIntRelation {

        public PluralityCRelation(@NotNull Iterator<@NotNull String> iter, @NotNull MutableInt counter) {
            super(PluralityCRelation::getValue, iter, counter);
        }

        private static @NotNull Integer getValue(CLDRPluralRuleNumber container) {
            return container.c;
        }
    }

    private static @NotNull Map<@NotNull String, @NotNull CLDRPluralRuleParser> createKnownLocales() {
        String presets = """
                am as bn doi fa gu hi kn kok kok_Latn pcm tg vi zu
                i = 0 or n = 1

                ff hy kab
                i = 0,1

                ast de en et fi fy ia ie io ji lij nl sc sv sw ur yi
                i = 1 and v = 0

                si
                n = 0,1 or i = 0 and f = 1

                ak bho csw guw ln mg nso pa ti wa
                n = 0..1

                tzm
                n = 0..1 or n = 11..99

                af an asa az bal bem bez bg brx ce cgg chr ckb dv ee el eo eu fo fur gsw ha haw hu jgo jmc ka kaj kcg kk kkj kl ks ksb ku ky lb lg mas mgo ml mn mr nah nb nd ne nn nnh no nr ny nyn om or os pap ps rm rof rwk saq sd sdh seh sn so sq ss ssy st syr ta te teo tig tk tn tr ts ug uz ve vo vun wae xh xog
                n = 1

                da
                n = 1 or t != 0 and i = 0,1

                is
                t = 0 and i % 10 = 1 and i % 100 != 11 or t % 10 = 1 and t % 100 != 11

                mk
                v = 0 and i % 10 = 1 and i % 100 != 11 or f % 10 = 1 and f % 100 != 11

                ceb fil tl
                v = 0 and i = 1,2,3 or v = 0 and i % 10 != 4,6,9 or v != 0 and f % 10 != 4,6,9

                lv prg
                n % 10 = 0 or n % 100 = 11..19 or v = 2 and f % 100 = 11..19
                n % 10 = 1 and n % 100 != 11 or v = 2 and f % 10 = 1 and f % 100 != 11 or v != 2 and f % 10 = 1

                lag
                n = 0
                i = 0,1 and n != 0

                blo cv ksh
                n = 0
                n = 1

                he iw
                i = 1 and v = 0 or i = 0 and v != 0
                i = 2 and v = 0

                iu naq sat se sma smi smj smn sms
                n = 1
                n = 2

                shi
                i = 0 or n = 1
                n = 2..10

                mo ro
                i = 1 and v = 0
                v != 0 or n = 0 or n != 1 and n % 100 = 1..19

                bs hr sh sr
                v = 0 and i % 10 = 1 and i % 100 != 11 or f % 10 = 1 and f % 100 != 11
                v = 0 and i % 10 = 2..4 and i % 100 != 12..14 or f % 10 = 2..4 and f % 100 != 12..14

                fr pt
                i = 0..1
                e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5

                ca gl it lld pt_PT scn vec
                i = 1 and v = 0
                e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5

                es
                n = 1
                e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5

                gd
                n = 1,11
                n = 2,12
                n = 3..10,13..19

                sl
                v = 0 and i % 100 = 1
                v = 0 and i % 100 = 2
                v = 0 and i % 100 = 3..4 or v != 0

                dsb hsb
                v = 0 and i % 100 = 1 or f % 100 = 1
                v = 0 and i % 100 = 2 or f % 100 = 2
                v = 0 and i % 100 = 3..4 or f % 100 = 3..4

                cs sk
                i = 1 and v = 0
                i = 2..4 and v = 0
                v != 0

                pl
                i = 1 and v = 0
                v = 0 and i % 10 = 2..4 and i % 100 != 12..14
                v = 0 and i != 1 and i % 10 = 0..1 or v = 0 and i % 10 = 5..9 or v = 0 and i % 100 = 12..14

                be
                n % 10 = 1 and n % 100 != 11
                n % 10 = 2..4 and n % 100 != 12..14
                n % 10 = 0 or n % 10 = 5..9 or n % 100 = 11..14

                lt
                n % 10 = 1 and n % 100 != 11..19
                n % 10 = 2..9 and n % 100 != 11..19
                f != 0

                ru uk
                v = 0 and i % 10 = 1 and i % 100 != 11
                v = 0 and i % 10 = 2..4 and i % 100 != 12..14
                v = 0 and i % 10 = 0 or v = 0 and i % 10 = 5..9 or v = 0 and i % 100 = 11..14


                sgs
                n % 10 = 1 and n % 100 != 11
                n = 2
                n != 2 and n % 10 = 2..9 and n % 100 != 11..19
                f != 0

                br
                n % 10 = 1 and n % 100 != 11,71,91
                n % 10 = 2 and n % 100 != 12,72,92
                n % 10 = 3..4,9 and n % 100 != 10..19,70..79,90..99
                n != 0 and n % 1000000 = 0

                mt
                n = 1
                n = 2
                n = 0 or n % 100 = 3..10
                n % 100 = 11..19

                ga
                n = 1
                n = 2
                n = 3..6
                n = 7..10

                gv
                v = 0 and i % 10 = 1
                v = 0 and i % 10 = 2
                v = 0 and i % 100 = 0,20,40,60,80
                v != 0

                kw
                n = 0
                n = 1
                n % 100 = 2,22,42,62,82 or n % 1000 = 0 and n % 100000 = 1000..20000,40000,60000,80000 or n != 0 and n % 1000000 = 100000
                n % 100 = 3,23,43,63,83
                n != 1 and n % 100 = 1,21,41,61,81

                ar ars
                n = 0
                n = 1
                n = 2
                n % 100 = 3..10
                n % 100 = 11..99

                cy
                n = 0
                n = 1
                n = 2
                n = 3
                n = 6
                """;

        Object2ObjectOpenHashMap<@NotNull String, @NotNull CLDRPluralRuleParser> ret = new Object2ObjectOpenHashMap<>();
        String[] sections = presets.split("(?m)$[\\r\\n]+?(^\\s*$[\\r\\n]+)+");
        for (String section : sections) {
            String[] parts = section.split("[\\r\\n]+", 2);
            if (parts.length != 2) throw new RuntimeException("Sections should contain at least 2 lines!");

            String header = parts[0].trim();
            String[] rules = parts[1].split("[\\r\\n]+");
            var parser = getParser(rules);
            for (String code : header.split(UNICODE_WHITESPACE_PATTERN)) {
                ret.put(code, parser);
            }
        }

        return ret;
    }
}
