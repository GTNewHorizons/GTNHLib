package com.gtnewhorizon.gtnhlib.util.font;

import net.minecraft.client.gui.GuiTextField;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * Handles whether a text field is supposed to treat ampersands as part of formatting codes (e.g. &a or &#RRGGBB) or if
 * it's part of its syntax and should be ignored (e.g. search fields, filters, etc.).
 */
public class TextFieldFormatting {

    private TextFieldFormatting() {}

    static boolean loggedNonMixinedField = false;

    public static void setAmpCodeAware(GuiTextField field, boolean enabled) {
        if (!(field instanceof IAmpCodeAwareField)) {
            // Shouldn't happen because of the mixin
            logNoMixinWarning(field);
            return;
        }

        ((IAmpCodeAwareField) field).gtnhlib$setAmpCodeAware(enabled);
    }

    public static boolean isAmpCodeAware(GuiTextField field) {
        if (!(field instanceof IAmpCodeAwareField)) {
            // Shouldn't happen because of the mixin, keep the previous behavior if it does anyhow
            logNoMixinWarning(field);
            return true;
        }

        return ((IAmpCodeAwareField) field).gtnhlib$isAmpCodeAware();
    }

    private static void logNoMixinWarning(GuiTextField field) {
        if (loggedNonMixinedField) {
            return;
        }

        GTNHLib.LOG.warn(
                "Text field {} doesn't implement IAmpCodeAware! Is the GuiTextField mixin applied?",
                field.getClass().getName());
        loggedNonMixinedField = true;
    }
}
