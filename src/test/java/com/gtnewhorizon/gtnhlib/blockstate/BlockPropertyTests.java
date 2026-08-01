package com.gtnewhorizon.gtnhlib.blockstate;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty.BitPackedIntegerMetaBlockProperty;

class BlockPropertyTests {

    // BooleanBlockProperty.FlagBooleanBlockProperty

    @Test
    void flagBooleanGetValue_flagBitSet_returnsTrue() {
        BooleanBlockProperty.FlagBooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 0b0100);

        Assertions.assertTrue(prop.getValue(0b0100));
        Assertions.assertTrue(prop.getValue(0b1111));
    }

    @Test
    void flagBooleanGetValue_flagBitUnset_returnsFalse() {
        BooleanBlockProperty.FlagBooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 0b0100);

        Assertions.assertFalse(prop.getValue(0b0000));
        Assertions.assertFalse(prop.getValue(0b1011));
    }

    @Test
    void flagBooleanGetMeta_true_setsFlagBit() {
        BooleanBlockProperty.FlagBooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 0b0010);

        Assertions.assertEquals(0b0010, prop.getMeta(true, 0b0000));
    }

    @Test
    void flagBooleanGetMeta_false_clearsFlagBit() {
        BooleanBlockProperty.FlagBooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 0b0010);

        Assertions.assertEquals(0b1101, prop.getMeta(false, 0b1111));
    }

    @Test
    void flagBooleanGetMeta_preservesUnrelatedBits() {
        BooleanBlockProperty.FlagBooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 0b0100);

        Assertions.assertEquals(0b1001, prop.getMeta(false, 0b1001));
        Assertions.assertEquals(0b0111, prop.getMeta(true, 0b0011));
    }

    @Test
    void flagBooleanName_returnsConstructorName() {
        Assertions.assertEquals("powered", BooleanBlockProperty.flag("powered", 1).getName());
    }

    @Test
    void flagBooleanHasTraits() {
        BooleanBlockProperty.FlagBooleanBlockProperty prop = BooleanBlockProperty.flag("lit", 1);

        Assertions.assertTrue(prop.hasTrait(BlockPropertyTrait.SupportsWorld));
        Assertions.assertTrue(prop.hasTrait(BlockPropertyTrait.SupportsStacks));
        Assertions.assertTrue(prop.hasTrait(BlockPropertyTrait.OnlyNeedsMeta));
        Assertions.assertTrue(prop.hasTrait(BlockPropertyTrait.WorldMutable));
        Assertions.assertTrue(prop.hasTrait(BlockPropertyTrait.StackMutable));
        Assertions.assertFalse(prop.hasTrait(BlockPropertyTrait.Transformable));
        Assertions.assertFalse(prop.hasTrait(BlockPropertyTrait.VectorTransformable));
        Assertions.assertFalse(prop.hasTrait(BlockPropertyTrait.Config));
    }

    // IntegerBlockProperty.MetaIntegerBlockProperty

    @Test
    void metaIntegerGetValue_extractsMaskedShiftedBits() {
        // Bits 2-3 (mask=0b1100, shift=2)
        BitPackedIntegerMetaBlockProperty prop = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("facing", 0b1100, 2);

        Assertions.assertEquals(0, (int) prop.getValue(0b0000));
        Assertions.assertEquals(1, (int) prop.getValue(0b0100));
        Assertions.assertEquals(2, (int) prop.getValue(0b1000));
        Assertions.assertEquals(3, (int) prop.getValue(0b1100));
    }

    @Test
    void metaIntegerGetMeta_encodesValueAndPreservesOtherBits() {
        BitPackedIntegerMetaBlockProperty prop = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("facing", 0b1100, 2);

        // Set value=2 into bits 2-3, preserving bit 0
        Assertions.assertEquals(0b1001, prop.getMeta(2, 0b0001));

        // Set value=0 clears the masked bits, preserving other bits
        Assertions.assertEquals(0b0001, prop.getMeta(0, 0b1101));
    }

    @Test
    void metaIntegerNeedsExisting_fullMaskReturnsFalse() {
        BitPackedIntegerMetaBlockProperty prop = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("value", -1, 0);

        Assertions.assertFalse(prop.needsExisting());
    }

    @Test
    void metaIntegerNeedsExisting_partialMaskReturnsTrue() {
        BitPackedIntegerMetaBlockProperty prop = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("value", 0b1111, 0);

        Assertions.assertTrue(prop.needsExisting());
    }

    // IntegerBlockProperty.MappedBlockProperty

    @Test
    void mappedGetValue_returnsStringForIndex() {
        BitPackedIntegerMetaBlockProperty base = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("shape", 0b0011, 0);
        IntegerBlockProperty.MappedBlockProperty mapped = (IntegerBlockProperty.MappedBlockProperty) base
                .map(Arrays.asList("north", "south", "east", "west"));

        Assertions.assertEquals("north", mapped.getValue(0b0000));
        Assertions.assertEquals("south", mapped.getValue(0b0001));
        Assertions.assertEquals("east", mapped.getValue(0b0010));
        Assertions.assertEquals("west", mapped.getValue(0b0011));
    }

    @Test
    void mappedGetMeta_returnsIndexForString() {
        BitPackedIntegerMetaBlockProperty base = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("shape", 0b0011, 0);
        IntegerBlockProperty.MappedBlockProperty mapped = (IntegerBlockProperty.MappedBlockProperty) base
                .map(Arrays.asList("north", "south", "east", "west"));

        Assertions.assertEquals(0, mapped.getMeta("north", 0));
        Assertions.assertEquals(1, mapped.getMeta("south", 0));
        Assertions.assertEquals(2, mapped.getMeta("east", 0));
        Assertions.assertEquals(3, mapped.getMeta("west", 0));
    }

    @Test
    void mappedGetMeta_preservesUnrelatedBits() {
        // Bits 0-1 hold the value; bit 2 is unrelated
        BitPackedIntegerMetaBlockProperty base = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("shape", 0b0011, 0);
        IntegerBlockProperty.MappedBlockProperty mapped = (IntegerBlockProperty.MappedBlockProperty) base
                .map(Arrays.asList("north", "south", "east", "west"));

        Assertions.assertEquals(0b0101, mapped.getMeta("south", 0b0100));
    }

    @Test
    void mappedGetValue_outOfRangeIndex_returnsNull() {
        BitPackedIntegerMetaBlockProperty base = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("shape", 0b0011, 0);
        IntegerBlockProperty.MappedBlockProperty mapped = (IntegerBlockProperty.MappedBlockProperty) base
                .map(Arrays.asList("north", "south"));

        // base decodes meta=0b0010 as index 2, which is out of range for a 2-element list
        Assertions.assertNull(mapped.getValue(0b0010));
    }

    @Test
    void mappedInheritsBaseTraits() {
        BitPackedIntegerMetaBlockProperty base = (BitPackedIntegerMetaBlockProperty) IntegerBlockProperty
                .meta("shape", 0b0011, 0);
        IntegerBlockProperty.MappedBlockProperty mapped = (IntegerBlockProperty.MappedBlockProperty) base
                .map(Arrays.asList("north", "south"));

        Assertions.assertTrue(mapped.hasTrait(BlockPropertyTrait.SupportsWorld));
        Assertions.assertFalse(mapped.hasTrait(BlockPropertyTrait.Transformable));
    }
}
