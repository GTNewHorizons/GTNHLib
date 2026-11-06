package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(GlobalDisableExponentialNotation.Extension.class)
public @interface GlobalDisableExponentialNotation {

    boolean value();

    class Extension extends StateExtension<GlobalDisableExponentialNotation> {

        private static final boolean OLD_STATE = NumberFormatConfig.globalDisableExponentialNotation;

        protected Extension() {
            super(GlobalDisableExponentialNotation.class);
        }

        @Override
        public void setup(GlobalDisableExponentialNotation attr) {
            NumberFormatConfig.globalDisableExponentialNotation = attr.value();
        }

        @Override
        public void teardown() {
            NumberFormatConfig.globalDisableExponentialNotation = OLD_STATE;
        }
    }
}
