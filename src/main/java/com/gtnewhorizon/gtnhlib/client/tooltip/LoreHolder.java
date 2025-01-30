package com.gtnewhorizon.gtnhlib.client.tooltip;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify fields which should be updated on a resource refresh. Annotated fields must be static and
 * of type {@link String}. To use this, register the declaring class via {@link LoreHandler#registerLoreHolder(Class)}.
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
