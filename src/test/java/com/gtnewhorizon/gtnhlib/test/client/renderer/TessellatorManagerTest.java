package com.gtnewhorizon.gtnhlib.test.client.renderer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;

/**
 * Unit tests for TessellatorManager compiling mode and capturing mode mutual exclusion.
 */
public class TessellatorManagerTest {

    @Test
    void testSetCompilingWithNullCallback() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TessellatorManager.setCompiling(null),
                "setCompiling(null) should throw IllegalArgumentException");
    }

    @Test
    void testSetCompilingTwice() {
        TessellatorManager.setCompiling(quads -> {});
        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> TessellatorManager.setCompiling(quads -> {}),
                    "setCompiling() when already compiling should throw IllegalStateException");
        } finally {
            TessellatorManager.stopCompiling();
        }
    }

    @Test
    void testStopCompilingWhenNotCompiling() {
        assertThrows(
                IllegalStateException.class,
                () -> TessellatorManager.stopCompiling(),
                "stopCompiling() when not compiling should throw IllegalStateException");
    }

    @Test
    void testSetCompilingWhenCapturing() {
        TessellatorManager.startCapturing();
        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> TessellatorManager.setCompiling(quads -> {}),
                    "setCompiling() when already capturing should throw IllegalStateException");
        } finally {
            // Clean up capturing mode
            TessellatorManager.stopCapturingToPooledQuads();
            TessellatorManager.cleanup();
        }
    }

    @Test
    void testStartCapturingWhenCompiling() {
        TessellatorManager.setCompiling(quads -> {});
        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> TessellatorManager.startCapturing(),
                    "startCapturing() when already compiling should throw IllegalStateException");
        } finally {
            TessellatorManager.stopCompiling();
        }
    }

    @Test
    void testSetCompilingAndStop() {
        // Basic lifecycle test - should not throw
        TessellatorManager.setCompiling(quads -> {});
        TessellatorManager.stopCompiling();
    }

    @Test
    void testCleanupClearsCompilingState() {
        TessellatorManager.setCompiling(quads -> {});
        TessellatorManager.cleanup();

        // After cleanup, compiling callback should be cleared
        // This means we should be able to set compiling again without error
        assertDoesNotThrow(() -> TessellatorManager.setCompiling(quads -> {}));
        TessellatorManager.stopCompiling();
    }
}
