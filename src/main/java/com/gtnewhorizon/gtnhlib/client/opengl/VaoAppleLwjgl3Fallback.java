package com.gtnewhorizon.gtnhlib.client.opengl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.IntBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VaoFunctions;

/**
 * LWJGL3-specific Apple VAO fallback implementation.
 */
public final class VaoAppleLwjgl3Fallback implements VaoFunctions {

    private static final Logger LOGGER = LogManager.getLogger("VaoAppleLwjgl3Fallback");

    private final long glGenVertexArraysAPPLE;
    private final long glDeleteVertexArraysAPPLE;
    private final long glBindVertexArrayAPPLE;
    private final long glIsVertexArrayAPPLE;

    private static final class Lwjgl3Handles {

        static final MethodHandle STACK_PUSH; // () -> MemoryStack
        static final MethodHandle STACK_CALLOC_INT; // (MemoryStack, int) -> IntBuffer
        static final MethodHandle STACK_INTS; // (MemoryStack, int) -> IntBuffer
        static final MethodHandle STACK_CLOSE; // (MemoryStack) -> void
        static final MethodHandle MEM_ADDRESS; // (Buffer) -> long
        static final MethodHandle JNI_CALL_PV; // (int, long, long) -> void
        static final MethodHandle JNI_CALL_V; // (int, long) -> void
        static final MethodHandle JNI_CALL_Z; // (int, long) -> boolean
        static final MethodHandle GET_FUNCTION_ADDRESS; // (String) -> long, bound to GL.getFunctionProvider()
        static final boolean INITIALIZED;

        static {
            boolean success = false;
            MethodHandle stackPush = null;
            MethodHandle stackCallocInt = null;
            MethodHandle stackInts = null;
            MethodHandle stackClose = null;
            MethodHandle memAddress = null;
            MethodHandle jniCallPV = null;
            MethodHandle jniCallV = null;
            MethodHandle jniCallZ = null;
            MethodHandle getFunctionAddress = null;

            try {
                final MethodHandles.Lookup lookup = MethodHandles.lookup();

                final Class<?> memoryStackClass = Class.forName("org.lwjgl.system.MemoryStack");
                final Class<?> memoryUtilClass = Class.forName("org.lwjgl.system.MemoryUtil");
                final Class<?> jniClass = Class.forName("org.lwjgl.system.JNI");

                stackPush = lookup.findStatic(memoryStackClass, "stackPush", MethodType.methodType(memoryStackClass));
                stackCallocInt = lookup
                        .findVirtual(memoryStackClass, "callocInt", MethodType.methodType(IntBuffer.class, int.class));
                stackInts = lookup
                        .findVirtual(memoryStackClass, "ints", MethodType.methodType(IntBuffer.class, int.class));
                stackClose = lookup.findVirtual(memoryStackClass, "close", MethodType.methodType(void.class));
                memAddress = lookup
                        .findStatic(memoryUtilClass, "memAddress", MethodType.methodType(long.class, Buffer.class));
                jniCallPV = lookup.findStatic(
                        jniClass,
                        "callPV",
                        MethodType.methodType(void.class, int.class, long.class, long.class));
                jniCallV = lookup
                        .findStatic(jniClass, "callV", MethodType.methodType(void.class, int.class, long.class));
                jniCallZ = lookup
                        .findStatic(jniClass, "callZ", MethodType.methodType(boolean.class, int.class, long.class));

                final Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
                final Method getFunctionProviderMethod = glClass.getMethod("getFunctionProvider");
                final Object provider = getFunctionProviderMethod.invoke(null);
                if (provider != null) {
                    final Method getFunctionAddressMethod = provider.getClass()
                            .getMethod("getFunctionAddress", CharSequence.class);
                    getFunctionAddress = lookup.unreflect(getFunctionAddressMethod).bindTo(provider);
                }

                success = provider != null;
            } catch (Exception e) {
                LOGGER.debug("LWJGL3 handles not available: {}", e.toString());
            }

            STACK_PUSH = stackPush;
            STACK_CALLOC_INT = stackCallocInt;
            STACK_INTS = stackInts;
            STACK_CLOSE = stackClose;
            MEM_ADDRESS = memAddress;
            JNI_CALL_PV = jniCallPV;
            JNI_CALL_V = jniCallV;
            JNI_CALL_Z = jniCallZ;
            GET_FUNCTION_ADDRESS = getFunctionAddress;
            INITIALIZED = success;
        }

        static long memAddress(IntBuffer buf) throws Throwable {
            return (long) MEM_ADDRESS.invokeExact((Buffer) buf);
        }

        static long getFunctionAddress(String name) throws Throwable {
            return (long) GET_FUNCTION_ADDRESS.invoke(name);
        }
    }

    private static final class StackFrame implements AutoCloseable {

        private final Object stack;

        StackFrame() throws Throwable {
            this.stack = Lwjgl3Handles.STACK_PUSH.invoke();
        }

        IntBuffer callocInt(int size) throws Throwable {
            return (IntBuffer) Lwjgl3Handles.STACK_CALLOC_INT.invoke(stack, size);
        }

        IntBuffer ints(int value) throws Throwable {
            return (IntBuffer) Lwjgl3Handles.STACK_INTS.invoke(stack, value);
        }

        @Override
        public void close() {
            try {
                Lwjgl3Handles.STACK_CLOSE.invoke(stack);
            } catch (Throwable ignored) {}
        }
    }

    private VaoAppleLwjgl3Fallback(long genAddr, long deleteAddr, long bindAddr, long isArrayAddr) {
        this.glGenVertexArraysAPPLE = genAddr;
        this.glDeleteVertexArraysAPPLE = deleteAddr;
        this.glBindVertexArrayAPPLE = bindAddr;
        this.glIsVertexArrayAPPLE = isArrayAddr;
    }

    public static VaoFunctions tryCreate() {
        if (!Lwjgl3Handles.INITIALIZED) {
            return null;
        }

        try {
            long genAddr = Lwjgl3Handles.getFunctionAddress("glGenVertexArraysAPPLE");
            long deleteAddr = Lwjgl3Handles.getFunctionAddress("glDeleteVertexArraysAPPLE");
            long bindAddr = Lwjgl3Handles.getFunctionAddress("glBindVertexArrayAPPLE");
            long isArrayAddr = Lwjgl3Handles.getFunctionAddress("glIsVertexArrayAPPLE");

            if (genAddr == 0 || deleteAddr == 0 || bindAddr == 0) {
                return null;
            }

            LOGGER.info("Using LWJGL3 Apple VAO fallback");
            return new VaoAppleLwjgl3Fallback(genAddr, deleteAddr, bindAddr, isArrayAddr);
        } catch (Throwable e) {
            LOGGER.debug("Failed to probe Apple VAO functions", e);
            return null;
        }
    }

    @Override
    public int getCurrentBinding() {
        return GL11.glGetInteger(APPLEVertexArrayObject.GL_VERTEX_ARRAY_BINDING_APPLE);
    }

    @Override
    public int glGenVertexArrays() {
        try (StackFrame stack = new StackFrame()) {
            final IntBuffer buf = stack.callocInt(1);
            final long addr = Lwjgl3Handles.memAddress(buf);
            Lwjgl3Handles.JNI_CALL_PV.invokeExact(1, addr, glGenVertexArraysAPPLE);
            return buf.get(0);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call glGenVertexArraysAPPLE", e);
        }
    }

    @Override
    public void glGenVertexArrays(IntBuffer output) {
        try {
            final long addr = Lwjgl3Handles.memAddress(output);
            Lwjgl3Handles.JNI_CALL_PV.invokeExact(output.remaining(), addr, glGenVertexArraysAPPLE);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call glGenVertexArraysAPPLE", e);
        }
    }

    @Override
    public void glDeleteVertexArrays(int id) {
        try (StackFrame stack = new StackFrame()) {
            final IntBuffer buf = stack.ints(id);
            final long addr = Lwjgl3Handles.memAddress(buf);
            Lwjgl3Handles.JNI_CALL_PV.invokeExact(1, addr, glDeleteVertexArraysAPPLE);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call glDeleteVertexArraysAPPLE", e);
        }
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer ids) {
        try {
            final long addr = Lwjgl3Handles.memAddress(ids);
            Lwjgl3Handles.JNI_CALL_PV.invokeExact(ids.remaining(), addr, glDeleteVertexArraysAPPLE);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call glDeleteVertexArraysAPPLE", e);
        }
    }

    @Override
    public boolean glIsVertexArray(int id) {
        if (glIsVertexArrayAPPLE == 0) {
            return false;
        }
        try {
            return (boolean) Lwjgl3Handles.JNI_CALL_Z.invokeExact(id, glIsVertexArrayAPPLE);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call glIsVertexArrayAPPLE", e);
        }
    }

    @Override
    public void glBindVertexArray(int id) {
        try {
            Lwjgl3Handles.JNI_CALL_V.invokeExact(id, glBindVertexArrayAPPLE);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call glBindVertexArrayAPPLE", e);
        }
    }
}
