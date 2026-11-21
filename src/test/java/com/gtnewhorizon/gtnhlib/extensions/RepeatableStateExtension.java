package com.gtnewhorizon.gtnhlib.extensions;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class RepeatableStateExtension<T extends Annotation, R extends Annotation>
        implements BeforeEachCallback, AfterEachCallback {

    private final Class<T> attrClazz;
    private final Class<R> repeatableAttrClazz;

    protected RepeatableStateExtension(Class<T> attrClazz, Class<R> repeatableAttrClazz) {
        this.attrClazz = attrClazz;
        this.repeatableAttrClazz = repeatableAttrClazz;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (context.getTestMethod().isPresent()) {
            T attr = context.getTestMethod().get().getDeclaredAnnotation(this.attrClazz);
            if (attr != null) {
                setup(context, attr);
                return;
            }
            R repeatAttr = context.getTestMethod().get().getDeclaredAnnotation(this.repeatableAttrClazz);
            if (repeatAttr != null) {
                setupRepeat(context, repeatAttr);
                return;
            }
        }
        if (context.getTestClass().isPresent()) {
            T attr = context.getTestClass().get().getDeclaredAnnotation(this.attrClazz);
            if (attr != null) {
                setup(context, attr);
                return;
            }
            R repeatAttr = context.getTestClass().get().getDeclaredAnnotation(this.repeatableAttrClazz);
            if (repeatAttr != null) {
                setupRepeat(context, repeatAttr);
                return;
            }
        }
        List<Class<?>> enclosingClasses = context.getEnclosingTestClasses();
        for (int i = enclosingClasses.size() - 1; i >= 0; i--) {
            Class<?> enclosing = enclosingClasses.get(i);
            T attr = enclosing.getDeclaredAnnotation(this.attrClazz);
            if (attr != null) {
                setup(context, attr);
                return;
            }
            R repeatAttr = enclosing.getDeclaredAnnotation(this.repeatableAttrClazz);
            if (repeatAttr != null) {
                setupRepeat(context, repeatAttr);
                return;
            }
            attr = enclosing.getDeclaredAnnotation(attrClazz);
            if (attr != null) return;
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

    public abstract void setupRepeat(ExtensionContext context, R attrs) throws IOException;

    public abstract void teardown();
}
