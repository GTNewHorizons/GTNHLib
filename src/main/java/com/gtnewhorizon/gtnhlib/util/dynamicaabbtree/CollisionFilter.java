package com.gtnewhorizon.gtnhlib.util.dynamicaabbtree;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface CollisionFilter<T extends Boundable & Identifiable> extends BiPredicate<T, T>
{
}
