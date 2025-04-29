package com.gtnewhorizon.gtnhlib.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

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
        this.availableInstances.addAll(instances);
        instances.clear();
    }

    public void releaseInstances(T[] instances) {
        this.availableInstances.addElements(availableInstances.size(), instances);
        Arrays.fill(instances, null);
    }
}
