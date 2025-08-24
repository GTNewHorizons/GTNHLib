package com.gtnewhorizon.gtnhlib.client.model;

import com.github.bsideup.jabel.Desugar;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Random;

@Desugar
public record Weighted<T>(T thing, int weight) {
    public static <T> T selectOne(ObjectList<Weighted<T>> heavyThings, Random rand) {
        var weight = 0;
        for (var v : heavyThings) {
            weight += v.weight();
        }

        final var selector = rand.nextInt(weight);
        weight = 0;
        for (var v : heavyThings) {
            if (selector <= weight) return v.thing();
            weight += v.weight();
        }

        throw new IllegalStateException("Randomly selected beyond the list!");
    }
}
