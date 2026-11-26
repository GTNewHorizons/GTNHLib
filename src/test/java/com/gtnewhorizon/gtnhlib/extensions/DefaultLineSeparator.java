package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.gtnewhorizon.gtnhlib.util.TextFormatConfig;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DefaultLineSeparator.Extension.class)
public @interface DefaultLineSeparator {

    TextFormatUtil.LineSeparatorStyle style();

    int length();

    class Extension extends StateExtension<DefaultLineSeparator> {

        private static final TextFormatUtil.LineSeparatorStyle OLD_STYLE = TextFormatConfig.lineSeparatorStyle;
        private static final int OLD_LENGTH = TextFormatConfig.lineSeparatorDefaultLength;

        protected Extension() {
            super(DefaultLineSeparator.class);
        }

        @Override
        public void setup(ExtensionContext context, DefaultLineSeparator attr) {
            TextFormatConfig.lineSeparatorStyle = attr.style();
            TextFormatConfig.lineSeparatorDefaultLength = attr.length();
        }

        @Override
        public void teardown() {
            TextFormatConfig.lineSeparatorStyle = OLD_STYLE;
            TextFormatConfig.lineSeparatorDefaultLength = OLD_LENGTH;
        }
    }
}
