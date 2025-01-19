package com.gtnewhorizon.gtnhlib.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {

    /**
     * The mod id that this configuration is associated with.
     */
    String modid();

    /**
     * Root element category, defaults to "general".
     */
    String category() default "general";

    /**
     * The subdirectory of the config directory to use. Defaults to none (config/). If you want to use a subdirectory,
     * you must specify it as a relative path (e.g. "myMod").
     */
    String configSubDirectory() default "";

    /**
     * The name of the configuration file. Defaults to the modid. The file extension (.cfg) is added automatically.
     */
    String filename() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @interface LangKey {

        String value();
    }

    /**
     * Defines a pattern for generating lang keys for fields and categories in the annotated class.
     * <p>
     * Placeholders: <br>
     * {@code %mod} - mod id <br>
     * {@code %file} - file name <br>
     * {@code %cat} - category name <br>
     * {@code %field} - field name <b>(required)</b> <br>
     * </p>
     * Default pattern: {@code %mod.%cat.%field}. Categories use the pattern without {@code %field}. Can be overridden
     * for fields with {@link Config.LangKey}. <br>
     * The generated keys can be printed to log by setting the {@code -Dgtnhlib.printkeys=true} JVM flag or dumped to a
     * file in the base minecraft directory by setting the {@code -Dgtnhlib.dumpkeys=true} JVM flag.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface LangKeyPattern {

        String pattern() default "%mod.%cat.%field";

        /**
         * Whether subcategories should use their fully qualified name.<br>
         * Fully qualified: {@code category.category1.category2} <br>
         * Normal: {@code category2}
         */
        boolean fullyQualified() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @interface Comment {

        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Ignore {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultBoolean {

        boolean value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface RangeInt {

        int min() default Integer.MIN_VALUE;

        int max() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultInt {

        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultIntList {

        int[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface RangeFloat {

        float min() default -Float.MAX_VALUE;

        float max() default Float.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultFloat {

        float value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface RangeDouble {

        double min() default -Double.MAX_VALUE;

        double max() default Double.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultDouble {

        double value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultDoubleList {

        double[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultString {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Pattern {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultEnum {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface DefaultStringList {

        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Name {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @interface RequiresMcRestart {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @interface RequiresWorldRestart {}

    /**
     * Set a default value if the listed coremod or modID is found. Coremod will take precedence value will be parsed to
     * target field's type
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface ModDetectedDefault {

        String coremod() default "";

        String modID() default "";

        String value() default "";

        /**
         * Can be used instead of value() for array fields
         */
        String[] values() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface ModDetectedDefaultList {

        ModDetectedDefault[] values() default {};
    }

    /**
     * Excludes this class from the auto config GUI, only applicable to a {@link Config} annotated class. Has no effect
     * if a gui factory is registered for the mod.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface ExcludeFromAutoGui {}

    /**
     * Fields or classes annotated with this will automatically be synced from server -> client. If applied to a class,
     * all fields (including subcategories) in the class will be synced. All fields are restored to their original value
     * when the player disconnects.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @interface Sync {

        /**
         * Can be used to overwrite the sync behavior for fields in classes annotated with {@link Sync}.
         * 
         * @return Whether the field should be synced. Defaults to true.
         */
        boolean value() default true;
    }
}
