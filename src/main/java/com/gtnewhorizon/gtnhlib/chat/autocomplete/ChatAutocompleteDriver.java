package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/// An autocomplete driver is an adapter that can provide autocomplete suggestions for pending chat text. This is
/// primarily used for commands, but it could be used for anything. When an autocomplete request starts, the
/// autocomplete system will scan all registered drivers. The first driver that returns true from
/// [#canDrive(ICommandSender, String, int)] gains authority over what autocomplete suggestions are displayed. Further
/// drivers are not checked. [#canDrive(ICommandSender, String, int)] is executed for each autocomplete request. There
/// is no 'active driver', so typing a single letter or changing the caret position could change which driver has
/// authority over the text immediately.
public interface ChatAutocompleteDriver {

    /// Gets the ID for this driver. This cannot change after the driver has been registered.
    ResourceLocation getDriverID();

    /// Checks if this driver can complete the given text. The first driver that can drive the text gains authority over
    /// the suggestions.
    ///
    /// @param sender The player that wrote the text.
    /// @param pendingText The whole in-progress text string, including any characters trailing after the caret.
    /// @param caretPosition The position of the caret within the pending text. May be positioned at the end of the
    /// text.
    /// @return True when this driver can provide suggestions for the pending text.
    boolean canDrive(ICommandSender sender, String pendingText, int caretPosition);

    /// Controls the side that [#getSuggestions(ICommandSender, String, int)] is called on, and how it's called.
    AutocompleteSyncMode getSyncMode();

    /// Merges the client and server suggestions. Only called when [#getSyncMode()] returns
    /// [AutocompleteSyncMode#MERGE].
    default AutocompleteSuggestions merge(AutocompleteSuggestions client, AutocompleteSuggestions server) {
        throw new UnsupportedOperationException();
    }

    /// Scans the pending text for suggestions at the current caret.
    ///
    /// @param sender The player that wrote the text.
    /// @param pendingText The whole in-progress text string, including any characters trailing after the caret.
    /// @param caretPosition The position of the caret within the pending text. May be positioned at the end of the
    /// text.
    /// @return The list of suggestions. Null means no suggestions are available for the current position.
    @Nullable
    AutocompleteSuggestions getSuggestions(ICommandSender sender, String pendingText, int caretPosition);
}
