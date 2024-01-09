/*
 * GTNHLib Fields helper - A Java 8-19 compatible reflection helper for unsafe accesses to fields. Written in 2023 by
 * Raven Szewczyk To the extent possible under law, the author(s) have dedicated all copyright and related and
 * neighboring rights to this software to the public domain worldwide. This software is distributed without any
 * warranty. See <http://creativecommons.org/publicdomain/zero/1.0> for the detailed license.
 */
package com.gtnewhorizon.gtnhlib.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Throwables;

import sun.misc.Unsafe;

/**
 * Utilities for {@link java.lang.reflect.Field} reflection, compatible with Java 8-19. Can read and write to final
 * static fields unlike the regular reflection API.
 */
public class Fields {

    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup mhLookup = MethodHandles.lookup();

    private Fields() {}

    static {
        try {
            java.lang.reflect.Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * How to look up a field in the class?
     */
    public enum LookupType {

        /** Look at the public API (using {@link Class#getField(String)}) */
        PUBLIC,
        /** Look at the fields declared in the exact class specified (using {@link Class#getDeclaredField(String)}) */
        DECLARED,
        /** Like {@link LookupType#DECLARED}, but also look in superclasses */
        DECLARED_IN_HIERARCHY;

        /**
         * Executes the lookup strategy.
         */
        public java.lang.reflect.Field lookup(Class<?> klass, String name) {
            switch (this) {
                case PUBLIC:
                    try {
                        return klass.getField(name);
                    } catch (NoSuchFieldException e) {
                        return null;
                    }
                case DECLARED:
                    try {
                        return klass.getDeclaredField(name);
                    } catch (NoSuchFieldException e) {
                        return null;
                    }
                case DECLARED_IN_HIERARCHY:
                    Class<?> currentClass = klass;
                    while (currentClass != null && currentClass != Object.class) {
                        try {
                            return currentClass.getDeclaredField(name);
                        } catch (NoSuchFieldException e) {
                            currentClass = currentClass.getSuperclass();
                        }
                    }
                    break;
            }
            return null;
        }
    }

    /**
     * Creates a type-safe fields accessor for the given class.
     */
    @Nonnull
    public static <T> ClassFields<T> ofClass(@Nonnull Class<T> klass) {
        return new ClassFields<>(Objects.requireNonNull(klass));
    }

    public static class ClassFields<C> {

        public final Class<C> klass;

        private ClassFields(Class<C> klass) {
            this.klass = klass;
        }

        private java.lang.reflect.Field getCheckedFieldImpl(@Nonnull LookupType strategy, @Nonnull String name,
                @Nullable Class<?> expectedType) {
            java.lang.reflect.Field javaField = strategy.lookup(klass, name);
            if (javaField == null) {
                return null;
            }
            if (expectedType != null) {
                Class<?> realType = javaField.getType();
                if (!realType.equals(expectedType)) {
                    throw new ClassCastException(
                            String.format(
                                    "Trying to access field %s#%s of type %s as type %s",
                                    klass.getName(),
                                    name,
                                    realType.getName(),
                                    expectedType.getName()));
                }
            }
            return javaField;
        }

        /**
         * Looks up a field in the class using the reflection API and provides non-type-safe accessors to it.
         *
         * @param strategy The lookup strategy, see {@link LookupType}
         * @param name     Name of the field we are searching for
         * @return A field wrapper for the found field, or null if not found.
         */
        public Field<?> getUntypedField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    null);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Object.class);
        }

        /**
         * Looks up a field in the class using the reflection API and provides type-safe accessors to it.
         *
         * @param strategy     The lookup strategy, see {@link LookupType}
         * @param name         Name of the field we are searching for
         * @param expectedType The type we want to access the field as
         * @return A field wrapper for the found field, or null if not found.
         * @throws ClassCastException If the field exists under the given name, but is not of the given type.
         */
        public <F> Field<F> getField(@Nonnull LookupType strategy, @Nonnull String name,
                @Nonnull Class<F> expectedType) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    Objects.requireNonNull(expectedType));
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, expectedType);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Boolean> getBooleanField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    boolean.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Boolean.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Byte> getByteField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    byte.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Byte.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Short> getShortField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    short.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Short.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Integer> getIntField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    int.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Integer.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Long> getLongField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    long.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Long.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Character> getCharField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    char.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Character.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Float> getFloatField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    float.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Float.class);
        }

        /**
         * Looks up a primitive field in the class.
         *
         * @see ClassFields#getField(LookupType, String, Class)
         */
        public Field<Double> getDoubleField(@Nonnull LookupType strategy, @Nonnull String name) {
            java.lang.reflect.Field javaField = getCheckedFieldImpl(
                    Objects.requireNonNull(strategy),
                    Objects.requireNonNull(name),
                    double.class);
            if (javaField == null) {
                return null;
            }
            return new Field<>(javaField, Double.class);
        }

        public class Field<F> {

            public final java.lang.reflect.Field javaField;
            public final Class<F> accessType;
            public final boolean isPrimitive, isStatic, isFinal;
            private final Function<C, F> getAccessor;
            private final BiConsumer<C, F> setAccessor;

            private Field(java.lang.reflect.Field javaField, Class<F> accessType) {
                javaField.setAccessible(true);
                this.javaField = javaField;
                this.accessType = accessType;
                this.isPrimitive = javaField.getType().isPrimitive();
                this.isStatic = Modifier.isStatic(javaField.getModifiers());
                this.isFinal = Modifier.isFinal(javaField.getModifiers());
                try {
                    // Generate getter
                    final MethodHandle getterHandle = mhLookup.unreflectGetter(this.javaField);
                    if (this.isStatic) {
                        final MethodHandle exactGetterHandle = getterHandle.asType(MethodType.methodType(Object.class));
                        this.getAccessor = obj -> {
                            try {
                                return (F) exactGetterHandle.invokeExact();
                            } catch (Throwable e) {
                                throw Throwables.propagate(e);
                            }
                        };
                    } else {
                        final MethodHandle exactGetterHandle = getterHandle
                                .asType(MethodType.methodType(Object.class, Object.class));
                        this.getAccessor = obj -> {
                            try {
                                return (F) exactGetterHandle.invokeExact((Object) obj);
                            } catch (Throwable e) {
                                throw Throwables.propagate(e);
                            }
                        };
                    }
                    // Generate setter
                    BiConsumer<C, F> setAccessor = null;
                    try { // Try the MethodHandle way first, fall back to unsafe is not possible (e.g. static final
                          // fields)
                        final MethodHandle setterHandle = mhLookup.unreflectSetter(javaField);
                        if (this.isStatic) {
                            final MethodHandle exactSetterHandle = setterHandle
                                    .asType(MethodType.methodType(void.class, Object.class));
                            setAccessor = (obj, val) -> {
                                try {
                                    exactSetterHandle.invokeExact((Object) val);
                                } catch (Throwable e) {
                                    throw Throwables.propagate(e);
                                }
                            };
                        } else {
                            final MethodHandle exactSetterHandle = setterHandle
                                    .asType(MethodType.methodType(void.class, Object.class, Object.class));
                            setAccessor = (obj, val) -> {
                                try {
                                    exactSetterHandle.invokeExact((Object) obj, (Object) val);
                                } catch (Throwable e) {
                                    throw Throwables.propagate(e);
                                }
                            };
                        }
                    } catch (IllegalAccessException e) {
                        // Unsafe fallback
                        final Object staticBase = isStatic ? UNSAFE.staticFieldBase(javaField) : null;
                        final long fieldOffset = isStatic ? UNSAFE.staticFieldOffset(javaField)
                                : UNSAFE.objectFieldOffset(javaField);
                        if (!isPrimitive) {
                            setAccessor = (obj, val) -> UNSAFE
                                    .putObject(unsafeBaseHelper(staticBase, obj, val), fieldOffset, val);
                        } else {
                            final Class<?> fieldType = javaField.getType();
                            if (fieldType.equals(boolean.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putBoolean(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Boolean) val);
                            } else if (fieldType.equals(byte.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putByte(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Byte) val);
                            } else if (fieldType.equals(short.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putShort(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Short) val);
                            } else if (fieldType.equals(int.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putInt(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Integer) val);
                            } else if (fieldType.equals(long.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putLong(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Long) val);
                            } else if (fieldType.equals(char.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putChar(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Character) val);
                            } else if (fieldType.equals(float.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putFloat(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Float) val);
                            } else if (fieldType.equals(double.class)) {
                                setAccessor = (obj, val) -> UNSAFE
                                        .putDouble(unsafeBaseHelper(staticBase, obj, val), fieldOffset, (Double) val);
                            } else {
                                throw new IllegalStateException();
                            }
                        }
                    }
                    this.setAccessor = Objects.requireNonNull(setAccessor);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            "Couldn't create a Field accessor for " + klass.getName() + "#" + javaField.getName(),
                            e);
                }
            }

            private Object unsafeBaseHelper(Object staticBase, Object obj, Object value) {
                if (!isStatic && !klass.isInstance(obj)) {
                    throw new ClassCastException(
                            "Illegal reflective access to field of " + klass
                                    + " using object of type "
                                    + obj.getClass());
                }
                if (!accessType.isAssignableFrom(value.getClass())) {
                    throw new ClassCastException(
                            "Illegal reflective set of value of type " + value.getClass()
                                    + " to field of type "
                                    + javaField.getType());
                }
                return isStatic ? staticBase : obj;
            }

            /**
             * @param object Instance of the class, or null if accessing a static field
             * @return The value of the field
             */
            public F getValue(C object) {
                return this.getAccessor.apply(object);
            }

            /**
             * @param object Instance of the class, or null if accessing a static field
             * @param value  New value to set in the field
             */
            public void setValue(C object, F value) {
                this.setAccessor.accept(object, value);
            }
        }
    }
}
