package com.gtnewhorizon.gtnhlib.util;

public interface IObjectPool<T> {

    T getInstance();

    void releaseInstance(T instance);
}
