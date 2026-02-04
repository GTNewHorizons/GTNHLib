package com.gtnewhorizon.gtnhlib.client.opengl;

import java.nio.IntBuffer;

import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VaoFunctions;

/**
 * Universal methods for handling Vertex Array Objects in the OpenGL versions supported by Minecraft. Uses the GL 3.0
 * VAO methods if available, and falls back to GL_APPLE_vertex_array_object or ARB_vertex_array_object if needed. This
 * should cover pretty much any GL 2.1-capable hardware.
 *
 * @author eigenraven
 */
public final class UniversalVAO {

    /**
     * Returns the implementation based on the ContextCapabilities.
     */
    public static VaoFunctions getImplementation(ContextCapabilities caps) {
        if (caps.OpenGL30) {
            return new VaoGL3();
        } else if (caps.GL_APPLE_vertex_array_object) {
            return new VaoApple();
        } else if (caps.GL_ARB_vertex_array_object) {
            return new VaoGL3();
        } else {
            return null;
        }
    }

    /**
     * Resets the cached VAO implementation for this thread, any further method calls will query the GL context
     * capabilities again on first call.
     */
    public static void reinitializeGlContext() {
        FUNCTIONS.remove();
    }

    /**
     * Equivalent to {@code glGetInteger(GL_VERTEX_ARRAY_BINDING)} for the currently used VAO extension.
     *
     * @return The identifier of the currently bound VAO, or 0 if none is bound.
     */
    public static int getVertexArrayBinding() {
        return FUNCTIONS.get().getCurrentBinding();
    }

    /**
     * Generates a single vertex array object name.
     *
     * @see #genVertexArrays(IntBuffer)
     */
    public static int genVertexArrays() {
        return FUNCTIONS.get().glGenVertexArrays();
    }

    /**
     * {@link #genVertexArrays(IntBuffer)} returns n vertex array object names in arrays. There is no guarantee that the
     * names form a contiguous set of integers; however, it is guaranteed that none of the returned names was in use
     * immediately before the call to {@link #genVertexArrays(IntBuffer)}.
     * <p>
     * Vertex array object names returned by a call to {@link #genVertexArrays(IntBuffer)} are not returned by
     * subsequent calls, unless they are first deleted with {@link #deleteVertexArrays(IntBuffer)}.
     * <p>
     * The names returned in arrays are marked as used, for the purposes of {@link #genVertexArrays(IntBuffer)} only,
     * but they acquire state and type only when they are first bound.
     * <h1>Errors</h1> {@link GL11#GL_INVALID_VALUE} is generated if n is negative.
     */
    public static void genVertexArrays(IntBuffer output) {
        FUNCTIONS.get().glGenVertexArrays(output);
    }

    /**
     * Frees a single vertex array object name.
     *
     * @param id The name to free
     * @see #deleteVertexArrays(IntBuffer)
     */
    public static void deleteVertexArrays(int id) {
        FUNCTIONS.get().glDeleteVertexArrays(id);
    }

    /**
     * {@link #deleteVertexArrays(IntBuffer)} deletes n vertex array objects whose names are stored in the array
     * addressed by arrays. Once a vertex array object is deleted it has no contents and its name is again unused. If a
     * vertex array object that is currently bound is deleted, the binding for that object reverts to zero and the
     * default vertex array becomes current. Unused names in arrays are silently ignored, as is the value zero.
     *
     * <h1>Errors</h1>
     *
     * {@link GL11#GL_INVALID_VALUE} is generated if n is negative.
     */
    public static void deleteVertexArrays(IntBuffer ids) {
        FUNCTIONS.get().glDeleteVertexArrays(ids);
    }

    /**
     * {@link #isVertexArray} returns {@code true} if array is currently the name of a vertex array object object. If
     * the array is zero, or if array is not the name of a vertex array object, or if an error occurs,
     * {@link #isVertexArray} returns {@code false}. If array is a name returned by {@link #genVertexArrays(IntBuffer)},
     * by that has not yet been bound through a call to {@link #bindVertexArray}, then the name is not a vertex array
     * object and glIsVertexArray returns {@code false}.
     */
    public static boolean isVertexArray(int array) {
        return FUNCTIONS.get().glIsVertexArray(array);
    }

    /**
     * {@link #bindVertexArray(int)} binds the vertex array object with name array. array is the name of a vertex array
     * object previously returned from a call to {@link #genVertexArrays(IntBuffer)}, or zero to break the existing
     * vertex array object binding.
     * <p>
     * If no vertex array object with name array exists, one is created when array is first bound. If the bind is
     * successful no change is made to the state of the vertex array object, and any previous vertex array object
     * binding is broken.
     * <h1>Errors</h1>
     *
     * {@link GL11#GL_INVALID_OPERATION} is generated if array is not zero or the name of a vertex array object
     * previously returned from a call to {@link #genVertexArrays(IntBuffer)}.
     */
    public static void bindVertexArray(int id) {
        FUNCTIONS.get().glBindVertexArray(id);
    }

    private static final ThreadLocal<VaoFunctions> FUNCTIONS = ThreadLocal
            .withInitial(() -> getImplementation(GLContext.getCapabilities()));

    private static final class VaoGL3 implements VaoFunctions {

        @Override
        public int getCurrentBinding() {
            return GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        }

        @Override
        public int glGenVertexArrays() {
            return GL30.glGenVertexArrays();
        }

        @Override
        public void glGenVertexArrays(IntBuffer output) {
            GL30.glGenVertexArrays(output);
        }

        @Override
        public void glDeleteVertexArrays(int id) {
            GL30.glDeleteVertexArrays(id);
        }

        @Override
        public void glDeleteVertexArrays(IntBuffer ids) {
            GL30.glDeleteVertexArrays(ids);
        }

        @Override
        public boolean glIsVertexArray(int id) {
            return GL30.glIsVertexArray(id);
        }

        @Override
        public void glBindVertexArray(int id) {
            GL30.glBindVertexArray(id);
        }
    }

    private static final class VaoApple implements VaoFunctions {

        @Override
        public int getCurrentBinding() {
            return GL11.glGetInteger(APPLEVertexArrayObject.GL_VERTEX_ARRAY_BINDING_APPLE);
        }

        @Override
        public int glGenVertexArrays() {
            return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
        }

        @Override
        public void glGenVertexArrays(IntBuffer output) {
            APPLEVertexArrayObject.glGenVertexArraysAPPLE(output);
        }

        @Override
        public void glDeleteVertexArrays(int id) {
            APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(id);
        }

        @Override
        public void glDeleteVertexArrays(IntBuffer ids) {
            APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(ids);
        }

        @Override
        public boolean glIsVertexArray(int id) {
            return APPLEVertexArrayObject.glIsVertexArrayAPPLE(id);
        }

        @Override
        public void glBindVertexArray(int id) {
            APPLEVertexArrayObject.glBindVertexArrayAPPLE(id);
        }
    }
}
