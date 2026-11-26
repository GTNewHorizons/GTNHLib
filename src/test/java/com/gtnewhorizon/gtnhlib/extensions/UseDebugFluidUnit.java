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
@ExtendWith(UseDebugFluidUnit.Extension.class)
public @interface UseDebugFluidUnit {

    boolean value();

    class Extension extends StateExtension<UseDebugFluidUnit> {

        private static final boolean OLD_STATE = NumberFormatConfig.debugFluidUnit;

        protected Extension() {
            super(UseDebugFluidUnit.class);
        }

        @Override
        public void setup(ExtensionContext context, UseDebugFluidUnit attr) {
            NumberFormatConfig.debugFluidUnit = attr.value();
            NumberFormatUtil.postConfiguration();
        }

        @Override
        public void teardown() {
            NumberFormatConfig.debugFluidUnit = OLD_STATE;
            NumberFormatUtil.postConfiguration();
        }
    }
}
