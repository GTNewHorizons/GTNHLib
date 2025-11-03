package com.gtnewhorizon.gtnhlib.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

/**
 * A predicate for ItemStacks.
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface ItemStackPredicate extends Predicate<ImmutableItemStack> {

    ItemStackPredicate ALL = stack -> true;

    boolean test(ImmutableItemStack stack);

    default boolean test(ItemStack stack) {
        return test(new FastImmutableItemStack(stack));
    }

    /// Gets any stacks that this predicate will match against, if possible. This is an optimization for inventories
    /// that maintain an Item+meta -> ItemStack index (such as AE). The meta, NBT, or stack size for stacks that are
    /// passed to [#test(ItemStack)] may not match one of the returned stacks if the index does not track that
    /// information.
    /// If this method returns non-null, [#test(ItemStack)] may not scan the inventory - only matches for the returned
    /// stacks will be tested.
    @Nullable
    default Collection<ItemStack> getStacks() {
        return null;
    }

    default @NotNull ItemStackPredicate and(ItemStackPredicate other) {
        Objects.requireNonNull(other);

        return new ItemStackPredicate() {

            private List<ItemStack> stacks;

            @Override
            public boolean test(ImmutableItemStack t) {
                return ItemStackPredicate.this.test(t) && other.test(t);
            }

            @Override
            public @Nullable Collection<ItemStack> getStacks() {
                Collection<ItemStack> a = ItemStackPredicate.this.getStacks();
                Collection<ItemStack> b = other.getStacks();

                if (a == null && b == null) return null;
                if (a == null || b == null) return a == null ? b : a;

                if (this.stacks != null) return this.stacks;

                this.stacks = new ArrayList<>(a.size() + b.size());

                this.stacks.addAll(a);
                this.stacks.addAll(b);

                return this.stacks;
            }
        };
    }

    default @NotNull ItemStackPredicate negate() {
        // There isn't a way to implement getStacks here, so just use a lambda
        return t -> !ItemStackPredicate.this.test(t);
    }

    default @NotNull ItemStackPredicate or(ItemStackPredicate other) {
        Objects.requireNonNull(other);

        return new ItemStackPredicate() {

            @Override
            public boolean test(ImmutableItemStack t) {
                return ItemStackPredicate.this.test(t) || other.test(t);
            }

            @Override
            public @Nullable Collection<ItemStack> getStacks() {
                Collection<ItemStack> a = ItemStackPredicate.this.getStacks();
                Collection<ItemStack> b = other.getStacks();

                if (a == null && b == null) return null;
                if (a == null || b == null) return a == null ? b : a;

                ArrayList<ItemStack> stacks = new ArrayList<>(a.size() + b.size());

                stacks.addAll(a);
                stacks.addAll(b);

                return stacks;
            }
        };
    }

    default @NotNull ItemStackPredicate withStackSize(int size) {
        return withStackSize(size, size);
    }

    default @NotNull ItemStackPredicate withStackSize(int min, int max) {
        return new ItemStackPredicate() {

            @Override
            public boolean test(ImmutableItemStack stack) {
                if (stack.getStackSize() < min || stack.getStackSize() > max) return false;

                return ItemStackPredicate.this.test(stack);
            }

            @Override
            public @Nullable Collection<ItemStack> getStacks() {
                return ItemStackPredicate.this.getStacks();
            }
        };
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

    static ItemStackPredicate matches(ItemStack test) {
        if (test == null) return ALL;

        return new ItemStackPredicate() {

            @Override
            public boolean test(ImmutableItemStack stack) {
                if (stack == null) return false;

                return stack.matches(test);
            }

            private List<ItemStack> list;

            @Override
            public List<ItemStack> getStacks() {
                if (list == null) list = Collections.singletonList(test);

                return list;
            }
        };
    }

    static ItemStackPredicate oredict(String name, boolean checkNBT) {
        List<ItemStack> ores = OreDictionary.getOres(name, false);

        ObjectOpenCustomHashSet<Object> stacks = new ObjectOpenCustomHashSet<>(
                ores,
                checkNBT ? ItemId.GENERIC_ITEM_META_NBT_STRATEGY : ItemId.GENERIC_ITEM_META_STRATEGY);

        return new ItemStackPredicate() {

            @Override
            public boolean test(ImmutableItemStack stack) {
                return stacks.contains(stack);
            }

            @Override
            public Collection<ItemStack> getStacks() {
                return ores;
            }
        };
    }

    static ItemStackPredicate stackSize(int stackSize) {
        return stack -> stack.getStackSize() == stackSize;
    }

    static ItemStackPredicate stackSizeRange(int lower, int upper) {
        return stack -> stack.getStackSize() >= lower && stack.getStackSize() <= upper;
    }
}
