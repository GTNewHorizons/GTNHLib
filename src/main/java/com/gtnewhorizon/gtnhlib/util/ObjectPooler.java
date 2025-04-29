package com.gtnewhorizon.gtnhlib.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ObjectPooler<T> {

    private final Supplier<T> instanceSupplier;
    private final ObjectArrayList<T> availableInstances;

    public ObjectPooler(Supplier<T> instanceSupplier) {
        this.instanceSupplier = instanceSupplier;
        this.availableInstances = new ObjectArrayList<>();
    }

    public T getInstance() {
        if (this.availableInstances.isEmpty()) {
            return this.instanceSupplier.get();
        }
        return this.availableInstances.remove(this.availableInstances.size() - 1);
    }

    public void releaseInstance(T instance) {
        this.availableInstances.add(instance);
    }

    public void releaseInstances(Collection<T> instances) {
        instances.forEach(i -> {
            if (i != null) {
                releaseInstance(i);
            }
        });
        instances.clear();
    }

    /**
     * Uses arraycopy instead of a loop. Faster, but doesn't check that the input is nonnull. Use with care!
     */
    public void releaseInstances(@NotNull T[] instances) {
        this.availableInstances.addElements(availableInstances.size(), instances);
        Arrays.fill(instances, null);
    }
}
