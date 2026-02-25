package com.gtnewhorizon.gtnhlib.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigurationManagerTest {

    @Test
    void configNodeIgnoresSyntheticOuterReferenceFields() {
        ConfigurationManager.ConfigNode root = assertDoesNotThrow(
                () -> new ConfigurationManager.ConfigNode(Outer.class));

        assertTrue(root.children.containsKey("nested"));
    }

    private static class Outer {

        Nested nested = new Nested();

        private class Nested {

            boolean enabled = true;
        }
    }
}
