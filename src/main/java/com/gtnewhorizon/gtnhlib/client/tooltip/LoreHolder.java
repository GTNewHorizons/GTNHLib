package com.gtnewhorizon.gtnhlib.client.tooltip;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify fields which should be updated on a resource refresh. Annotated fields must be static and
 * of type {@link String}. When the resources are reloaded, the field(s) are updated with a random translation. The
 * possible lines are defined via lang files, using the translation key defined by {@link #value()}, appended by an
 * index (starting with 0). Blank translations are ignored. The translations may be weighted by using {@code <weight>:}
 * as prefix, {@code <weight>} being a non-negative integer. If no weight is specified, a default value of 1 is used. To
 * prevent ':' being used as delimiter, escape it using '\'.
 *
 * @since 0.5.21
 * @author glowredman
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LoreHolder {

    /**
     * The localization key
     */
    String value();
}
