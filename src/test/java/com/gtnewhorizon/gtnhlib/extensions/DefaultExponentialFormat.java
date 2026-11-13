package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.gtnewhorizon.gtnhlib.util.numberformatting.ExponentialFormat;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DefaultExponentialFormat.Extension.class)
public @interface DefaultExponentialFormat {

    ExponentialFormat value();

    class Extension extends StateExtension<DefaultExponentialFormat> {

        private static final String OLD_STATE = NumberFormatConfig.formatPattern;

        protected Extension() {
            super(DefaultExponentialFormat.class);
        }

        @Override
        public void setup(ExtensionContext context, DefaultExponentialFormat attr) {
            NumberFormatConfig.formatPattern = attr.value().name().toUpperCase();
            NumberFormatUtil.postConfiguration();
        }

        @Override
        public void teardown() {
            NumberFormatConfig.formatPattern = OLD_STATE;
            NumberFormatUtil.postConfiguration();
        }
    }
}
