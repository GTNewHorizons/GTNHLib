package com.gtnewhorizon.gtnhlib.extensions;

import java.io.ByteArrayInputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

/**
 * Tests using this annotation must include a String parameter, it will be filled with the a unique generated id for the
 * test's keys.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(InjectLocalisation.Extension.class)
@Repeatable(InjectLocalisation.Repeatable.class)
public @interface InjectLocalisation {

    String key();

    String text();

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ExtendWith(InjectLocalisation.Extension.class)
    @interface Repeatable {

        InjectLocalisation[] value();
    }

    class Extension extends RepeatableStateExtension<InjectLocalisation, InjectLocalisation.Repeatable> {

        private String toRestore = null;

        protected Extension() {
            super(InjectLocalisation.class, Repeatable.class);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            super.beforeEach(context);
            NumberFormatUtil.LOC_CACHE.onResourceManagerReload();
        }

        @Override
        public void setup(ExtensionContext context, InjectLocalisation attr) {
            String toInject = attr.key() + "=" + attr.text();
            toRestore = attr.key() + "=" + StatCollector.translateToLocal(attr.key());
            byte[] bytes = toInject.getBytes(StandardCharsets.UTF_8);
            StringTranslate.inject(new ByteArrayInputStream(bytes));
        }

        @Override
        public void setupRepeat(ExtensionContext context, Repeatable attrs) {
            String toInject = Arrays.stream(attrs.value()).map(attr -> attr.key() + "=" + attr.text())
                    .collect(Collectors.joining(System.lineSeparator()));
            toRestore = Arrays.stream(attrs.value())
                    .map(attr -> attr.key() + "=" + StatCollector.translateToLocal(attr.key()))
                    .collect(Collectors.joining(System.lineSeparator()));
            byte[] bytes = toInject.getBytes(StandardCharsets.UTF_8);
            StringTranslate.inject(new ByteArrayInputStream(bytes));
        }

        @Override
        public void teardown() {
            // thread safety jic
            String toRestore = this.toRestore;
            if (toRestore != null) {
                byte[] bytes = toRestore.getBytes(StandardCharsets.UTF_8);
                StringTranslate.inject(new ByteArrayInputStream(bytes));
            }
            NumberFormatUtil.LOC_CACHE.resetToDefaults();
            NumberFormatUtil.LOC_CACHE.onResourceManagerReload();
        }
    }
}
