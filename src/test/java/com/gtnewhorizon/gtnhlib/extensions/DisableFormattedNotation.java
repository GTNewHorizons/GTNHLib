package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableFormattedNotation.Extension.class)
public @interface DisableFormattedNotation {

    boolean value();

    class Extension extends StateExtension<DisableFormattedNotation> {

        private static final boolean OLD_STATE = NumberFormatConfig.disableFormattedNotation;

        protected Extension() {
            super(DisableFormattedNotation.class);
        }

        @Override
        public void setup(ExtensionContext context, DisableFormattedNotation attr) {
            NumberFormatConfig.disableFormattedNotation = attr.value();
        }

        @Override
        public void teardown() {
            NumberFormatConfig.disableFormattedNotation = OLD_STATE;
        }
    }
}
