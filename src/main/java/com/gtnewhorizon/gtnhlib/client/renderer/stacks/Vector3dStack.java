package com.gtnewhorizon.gtnhlib.client.renderer.stacks;

import org.joml.Vector3d;

public class Vector3dStack extends Vector3d implements IStateStack<Vector3dStack> {

    public static final int MAX_STACK_DEPTH = 16;
    protected final Vector3d[] stack;

    protected int pointer;

    public Vector3dStack() {
        stack = new Vector3d[MAX_STACK_DEPTH];
        for (int i = 0; i < MAX_STACK_DEPTH; i++) {
            stack[i] = new Vector3d();
        }
    }

    @Override
    public Vector3dStack push() {
        if (pointer == stack.length) {
            throw new IllegalStateException("Stack overflow size " + (pointer + 1) + " reached");
        }
        stack[pointer++].set(this);
        return this;
    }

    @Override
    public Vector3dStack pop() {
        if (pointer == 0) {
            throw new IllegalStateException("Stack underflow");
        }
        set(stack[--pointer]);
        return this;
    }

    public boolean isEmpty() {
        return pointer == 0;
    }
}
