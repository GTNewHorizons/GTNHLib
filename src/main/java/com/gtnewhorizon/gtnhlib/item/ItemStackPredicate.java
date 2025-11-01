package com.gtnewhorizon.gtnhlib.item;

import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;

/**
 * A predicate for ItemStacks.
 */
@FunctionalInterface
public interface ItemStackPredicate extends Predicate<ItemStack> {

    @Override
    boolean test(ItemStack stack);

    default boolean test(ImmutableItemStack stack) {
        return test(stack.toStack());
    }

    default @NotNull ItemStackPredicate and(ItemStackPredicate other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default @NotNull ItemStackPredicate negate() {
        return (t) -> !test(t);
    }

    default @NotNull ItemStackPredicate or(ItemStackPredicate other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    static @NotNull ItemStackPredicate not(ItemStackPredicate target) {
        Objects.requireNonNull(target);
        return target.negate();
    }

    static @NotNull ItemStackPredicate and(ItemStackPredicate a, ItemStackPredicate b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return a.and(b);
    }

    static ItemStackPredicate matches(ItemStack other) {
        return stack -> ItemUtil.areStacksEqual(stack, other);
    }

    static ItemStackPredicate stackSizeRange(int lower, int upper) {
        return stack -> stack.stackSize >= lower && stack.stackSize < upper;
    }
}
