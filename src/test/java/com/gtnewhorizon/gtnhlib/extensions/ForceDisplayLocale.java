package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ForceDisplayLocale.Extension.class)
public @interface ForceDisplayLocale {

    String value();

    class Extension extends StateExtension<ForceDisplayLocale> {

        private static final Locale OLD_STATE = Locale.getDefault(Locale.Category.DISPLAY);

        protected Extension() {
            super(ForceDisplayLocale.class);
        }

        @Override
        public void setup(ForceDisplayLocale attr) {
            Locale codeLocal = Locale.forLanguageTag(attr.value());
            if (codeLocal == null)
                throw new IllegalArgumentException("local code \"" + attr.value() + "\" is invalid.");
            Locale.setDefault(Locale.Category.DISPLAY, codeLocal);
        }

        @Override
        public void teardown() {
            Locale.setDefault(Locale.Category.DISPLAY, OLD_STATE);
        }
    }
}
