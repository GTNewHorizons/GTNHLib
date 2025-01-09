package com.gtnewhorizon.gtnhlib.util.data;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

/**
 * A supplier that provides an ItemStack. This is its own type because superclasses save their generics, allowing the
 * JVM to differentiate between functional interfaces at runtime. Without this interface, two methods overloads that
 * accept a Supplier with different generics but an otherwise identical method signature would cause a compilation
 * error.
 */
@FunctionalInterface
public interface ItemStackSupplier extends Supplier<ItemStack> {

}
