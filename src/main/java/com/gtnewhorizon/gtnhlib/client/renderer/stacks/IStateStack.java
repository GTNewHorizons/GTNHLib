package com.gtnewhorizon.gtnhlib.client.renderer.stacks;

public interface IStateStack<T> {

    T push();

    T pop();

    boolean isEmpty();

}
