package com.gtnewhorizon.gtnhlib.blockstate;

import java.util.Map;

import net.minecraft.init.Blocks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateImpl;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;

class BlockStateTests {

    @Test
    void parsing() {
        BlockStateImpl state = new BlockStateImpl();

        state.setBlock(Blocks.gold_block);
        state.setPropertyValue("property", "something");
        state.setPropertyValue("other", "foo");

        Assertions
                .assertEquals(state, BlockState.fromString(null, "minecraft:gold_block[property=something,other=foo]"));
    }

    // Deferred properties (set via name, no BlockProperty reference)

    @Test
    void setDeferredProperty_getWithIncludeDeferred_returnsValue() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");

        Assertions.assertEquals("bar", state.getPropertyValue("foo", true));
    }

    @Test
    void setDeferredProperty_getWithoutIncludeDeferred_returnsNull() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");

        Assertions.assertNull(state.getPropertyValue("foo", false));
    }

    @Test
    void setDeferredPropertyTwice_secondValueOverwritesFirst() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "first");
        state.setPropertyValue("foo", "second");

        Assertions.assertEquals("second", state.getPropertyValue("foo", true));
    }

    // Non-deferred properties (set via BlockProperty reference)

    @Test
    void setPropertyByReference_getByReference_returnsValue() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue(prop, true);

        Assertions.assertEquals(Boolean.TRUE, state.getPropertyValue(prop));
    }

    @Test
    void setPropertyByReference_getByName_returnsValue() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue(prop, true);

        Assertions.assertEquals(Boolean.TRUE, state.getPropertyValue("lit", false));
    }

    /// After setting a property by reference, updating it by name with a String parses
    /// the string via the property's parse method and updates the value in place.
    @Test
    void setPropertyByReferenceThenUpdateByNameWithString_parsesAndUpdates() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue(prop, false);
        state.setPropertyValue("lit", "true");

        Assertions.assertEquals(Boolean.TRUE, state.getPropertyValue(prop));
    }

    // Equality

    @Test
    void equals_bothDeferred_sameNameAndValue_equal() {
        BlockStateImpl a = new BlockStateImpl();
        a.setPropertyValue("x", "hello");

        BlockStateImpl b = new BlockStateImpl();
        b.setPropertyValue("x", "hello");

        Assertions.assertEquals(a, b);
    }

    @Test
    void equals_bothNonDeferred_samePropertyAndValue_equal() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);

        BlockStateImpl a = new BlockStateImpl();
        a.setPropertyValue(prop, true);

        BlockStateImpl b = new BlockStateImpl();
        b.setPropertyValue(prop, true);

        Assertions.assertEquals(a, b);
    }

    /// A deferred entry with value "true" and a non-deferred entry holding Boolean.TRUE
    /// for the same property name must compare equal, since equals converts both to text.
    @Test
    void equals_deferredVsNonDeferred_equivalentTextValue_equal() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);

        BlockStateImpl deferred = new BlockStateImpl();
        deferred.setPropertyValue("lit", "true");

        BlockStateImpl resolved = new BlockStateImpl();
        resolved.setPropertyValue(prop, true);

        Assertions.assertEquals(deferred, resolved);
    }

    @Test
    void equals_differentPropertyValue_notEqual() {
        BlockStateImpl a = new BlockStateImpl();
        a.setPropertyValue("x", "foo");

        BlockStateImpl b = new BlockStateImpl();
        b.setPropertyValue("x", "bar");

        Assertions.assertNotEquals(a, b);
    }

    @Test
    void equals_extraPropertyInOne_notEqual() {
        BlockStateImpl a = new BlockStateImpl();
        a.setPropertyValue("x", "foo");

        BlockStateImpl b = new BlockStateImpl();
        b.setPropertyValue("x", "foo");
        b.setPropertyValue("y", "bar");

        Assertions.assertNotEquals(a, b);
    }

    // toMap

    @Test
    void toMap_deferredProperty_usesStringValueOf() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");

        Map<String, String> map = state.toMap();
        Assertions.assertEquals("bar", map.get("foo"));
    }

    @Test
    void toMap_nonDeferredProperty_usesPropertyStringify() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue(prop, true);

        Map<String, String> map = state.toMap();
        Assertions.assertEquals("true", map.get("lit"));
    }

    @Test
    void toMap_multipleProperties_allPresent() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue(prop, false);
        state.setPropertyValue("powered", "true");

        Map<String, String> map = state.toMap();
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("false", map.get("lit"));
        Assertions.assertEquals("true", map.get("powered"));
    }

    // reset

    @Test
    void reset_clearsAllProperties() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");
        state.setPropertyValue(BooleanBlockProperty.flag("lit", 1), true);

        state.reset();

        Assertions.assertNull(state.getPropertyValue("foo", true));
        Assertions.assertNull(state.getPropertyValue("lit", true));
    }

    @Test
    void reset_clearsBlock() {
        BlockStateImpl state = new BlockStateImpl();
        state.setBlock(Blocks.stone);
        state.reset();

        Assertions.assertNull(state.getBlock());
    }

    // clone

    /// Non-pooled instances (pool == null) must be cloneable without NPE.
    @Test
    void clone_nonPooledInstance_returnsEqualCopy() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("foo", "bar");
        state.setPropertyValue(BooleanBlockProperty.flag("lit", 1), true);

        BlockStateImpl cloned = state.clone();

        Assertions.assertEquals(state, cloned);
        Assertions.assertNotSame(state, cloned);
    }

    /// Cloning must not share mutable state: modifying the clone must not affect the original.
    @Test
    void clone_mutatingCloneDoesNotAffectOriginal() {
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue("x", "original");

        BlockStateImpl cloned = state.clone();
        cloned.setPropertyValue("x", "modified");

        Assertions.assertEquals("original", state.getPropertyValue("x", true));
    }

    // copy

    /// copy() must populate `this` from `other`, not the reverse.
    @Test
    void copy_populatesThisFromOther() {
        BlockStateImpl src = new BlockStateImpl();
        src.setPropertyValue("key", "value");

        BlockStateImpl dst = new BlockStateImpl();
        dst.copy(src);

        Assertions.assertEquals("value", dst.getPropertyValue("key", true));
        // src must be unmodified
        Assertions.assertEquals("value", src.getPropertyValue("key", true));
    }

    /// copy() must not corrupt the source when `this.entries` is larger than `other.entries`.
    @Test
    void copy_largerDestinationDoesNotCorruptSource() {
        BlockStateImpl src = new BlockStateImpl();
        src.setPropertyValue("a", "1");

        // Pre-populate dst with more entries so its list is larger
        BlockStateImpl dst = new BlockStateImpl();
        dst.setPropertyValue("p", "x");
        dst.setPropertyValue("q", "y");
        dst.setPropertyValue("r", "z");

        dst.copy(src);

        Assertions.assertEquals("1", dst.getPropertyValue("a", true));
        Assertions.assertNull(dst.getPropertyValue("p", true));
        Assertions.assertNull(dst.getPropertyValue("q", true));
        // src must be completely untouched
        Assertions.assertEquals("1", src.getPropertyValue("a", true));
        Assertions.assertNull(src.getPropertyValue("p", true));
    }

    // hashCode

    /// hashCode() must not NPE when block is null.
    @Test
    void hashCode_nullBlock_doesNotThrow() {
        BlockStateImpl state = new BlockStateImpl();
        Assertions.assertDoesNotThrow(state::hashCode);
    }

    /// Two states that are equal must have the same hashCode (equals/hashCode contract).
    @Test
    void hashCode_equalStates_sameHashCode() {
        BlockStateImpl a = new BlockStateImpl();
        a.setPropertyValue("x", "hello");

        BlockStateImpl b = new BlockStateImpl();
        b.setPropertyValue("x", "hello");

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    // setPropertyValue — deferred-to-property upgrade (duplicate name prevention)

    /// Setting a property by reference after a deferred entry with the same name exists must
    /// upgrade the existing entry rather than creating a duplicate.
    @Test
    void setPropertyByReference_afterDeferredSameName_upgradesInPlace() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();

        state.setPropertyValue("lit", "true"); // deferred
        state.setPropertyValue(prop, false); // should upgrade, not duplicate

        // Only one entry should exist — toMap must have exactly one key
        Assertions.assertEquals(1, state.toMap().size());
        Assertions.assertEquals(Boolean.FALSE, state.getPropertyValue(prop));
    }

    // setPropertyValue — primitive/boxed type matching

    /// Setting a property by name with a boxed Boolean on an entry backed by a
    /// BooleanBlockProperty (getType() == boolean.class) must be accepted.
    @Test
    void setPropertyByName_boxedBooleanOnPrimitiveProperty_accepted() {
        BooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);
        BlockStateImpl state = new BlockStateImpl();
        state.setPropertyValue(prop, false);

        state.setPropertyValue("lit", Boolean.TRUE);

        Assertions.assertEquals(Boolean.TRUE, state.getPropertyValue(prop));
    }

    // fromString — quoted values

    @Test
    void fromString_missingOpenBracket_throwsIllegalArgument() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> BlockState.fromString(null, "minecraft:gold_block"));
    }

    @Test
    void fromString_missingCloseBracket_throwsIllegalArgument() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> BlockState.fromString(null, "minecraft:gold_block[foo=bar"));
    }

    @Test
    void fromString_quotedValueWithComma_parsedCorrectly() {
        BlockStateImpl expected = new BlockStateImpl();
        expected.setBlock(Blocks.gold_block);
        expected.setPropertyValue("desc", "hello,world");
        expected.setPropertyValue("other", "foo");

        BlockState parsed = BlockState.fromString(null, "minecraft:gold_block[desc=\"hello,world\",other=foo]");

        Assertions.assertEquals(expected, parsed);
    }

    @Test
    void fromString_quotedValueWithEquals_parsedCorrectly() {
        BlockStateImpl expected = new BlockStateImpl();
        expected.setBlock(Blocks.gold_block);
        expected.setPropertyValue("expr", "a=b");

        BlockState parsed = BlockState.fromString(null, "minecraft:gold_block[expr=\"a=b\"]");

        Assertions.assertEquals(expected, parsed);
    }

    @Test
    void fromString_quotedValueWithEscapedQuote_parsedCorrectly() {
        BlockStateImpl expected = new BlockStateImpl();
        expected.setBlock(Blocks.gold_block);
        expected.setPropertyValue("msg", "say \"hi\"");

        BlockState parsed = BlockState.fromString(null, "minecraft:gold_block[msg=\"say \\\"hi\\\"\"]");

        Assertions.assertEquals(expected, parsed);
    }
}
