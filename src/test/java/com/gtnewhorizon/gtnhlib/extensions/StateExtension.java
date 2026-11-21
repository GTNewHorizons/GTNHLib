package com.gtnewhorizon.gtnhlib.extensions;

import java.io.IOException;
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
        if (context.getTestMethod().isPresent()) {
            T attr = context.getTestMethod().get().getDeclaredAnnotation(attrClazz);
            if (attr != null) {
                this.setup(context, attr);
                return;
            }
        }
        if (context.getTestClass().isPresent()) {
            T attr = context.getTestClass().get().getDeclaredAnnotation(attrClazz);
            if (attr != null) {
                this.setup(context, attr);
                return;
            }
        }
        List<Class<?>> enclosingClasses = context.getEnclosingTestClasses();
        for (int i = enclosingClasses.size() - 1; i >= 0; i--) {
            Class<?> enclosing = enclosingClasses.get(i);
            T attr = enclosing.getDeclaredAnnotation(attrClazz);
            if (attr == null) continue;
            this.setup(context, attr);
            return;
        }

        // can't find annotation, someone probably used the extension directly by accident
        throw new IllegalStateException(
                "The " + attrClazz.getName() + " extension lass isn't meant to be used standalone.");

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        this.teardown();
    }

    public abstract void setup(ExtensionContext context, T attr) throws IOException;

    public abstract void teardown();
}
