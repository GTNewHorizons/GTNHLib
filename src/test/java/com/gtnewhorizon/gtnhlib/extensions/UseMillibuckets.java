package com.gtnewhorizon.gtnhlib.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(UseMillibuckets.Extension.class)
public @interface UseMillibuckets {

    boolean value();

    class Extension extends StateExtension<UseMillibuckets> {

        private static final boolean OLD_STATE = NumberFormatConfig.useForgeFluidMillibuckets;

        protected Extension() {
            super(UseMillibuckets.class);
        }

        @Override
        public void setup(ExtensionContext context, UseMillibuckets attr) {
            NumberFormatConfig.useForgeFluidMillibuckets = attr.value();
            NumberFormatUtil.postConfiguration();
        }

        @Override
        public void teardown() {
            NumberFormatConfig.useForgeFluidMillibuckets = OLD_STATE;
            NumberFormatUtil.postConfiguration();
        }
    }
}
