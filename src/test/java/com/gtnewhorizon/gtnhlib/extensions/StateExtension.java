package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class StateExtension<T extends Annotation> implements BeforeEachCallback, AfterEachCallback {

    private final Class<T> attrClazz;

    protected StateExtension(Class<T> attrClazz) {
        this.attrClazz = attrClazz;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        T attr = null;
        if (context.getTestMethod().isPresent()) {
            attr = context.getTestMethod().get().getDeclaredAnnotation(attrClazz);
        }
        if (attr == null && context.getTestClass().isPresent()) {
            attr = context.getTestClass().get().getDeclaredAnnotation(attrClazz);
        }
        if (attr == null) {
            List<Class<?>> enclosingClasses = context.getEnclosingTestClasses();
            for (int i = enclosingClasses.size() - 1; i >= 0; i--) {
                attr = context.getTestClass().get().getDeclaredAnnotation(attrClazz);
                if (attr != null) return;
            }
        }

        // can't find annotation, someone probably used the extension directly by accident
        if (attr == null) {
            throw new IllegalStateException(
                    "The " + attrClazz.getName() + " extension lass isn't meant to be used standalone.");
        }

        this.setup(attr);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        this.teardown();
    }

    public abstract void setup(T attr);

    public abstract void teardown();
}
