package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.junit.jupiter.api.extension.ExtendWith;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ForceFormatLocale.Extension.class)
public @interface ForceFormatLocale {

    String value();

    class Extension extends StateExtension<ForceFormatLocale> {

        private static final Locale OLD_STATE = Locale.getDefault(Locale.Category.FORMAT);

        protected Extension() {
            super(ForceFormatLocale.class);
        }

        @Override
        public void setup(ForceFormatLocale attr) {
            Locale codeLocal = Locale.forLanguageTag(attr.value());
            if (codeLocal == null)
                throw new IllegalArgumentException("local code \"" + attr.value() + "\" is invalid.");
            Locale.setDefault(Locale.Category.FORMAT, codeLocal);
        }

        @Override
        public void teardown() {
            Locale.setDefault(Locale.Category.FORMAT, OLD_STATE);
        }
    }
}
