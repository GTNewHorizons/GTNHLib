package com.gtnewhorizon.gtnhlib.client.model;

import java.util.Random;

import it.unimi.dsi.fastutil.objects.ObjectList;

/// A simple wrapper for any object, adding a weight. Also provides a helper for selecting from a weighted list.
public final class Weighted<T> {

    private final T thing;
    private final int weight;

    public Weighted(T thing, int weight) {
        this.thing = thing;
        this.weight = weight;
    }

    public static <T> T selectOne(ObjectList<Weighted<T>> heavyThings, Random rand) {
        var maxWeight = 0;
        for (var v : heavyThings) {
            maxWeight += v.weight();
        }

        final var selector = rand.nextInt(maxWeight) + 1;
        var weight = 0;
        for (var v : heavyThings) {
            weight += v.weight();
            if (selector <= weight) return v.thing();
        }

        throw new IllegalStateException("Randomly selected beyond the list!");
    }

    public T thing() {
        return thing;
    }

    public int weight() {
        return weight;
    }
}
