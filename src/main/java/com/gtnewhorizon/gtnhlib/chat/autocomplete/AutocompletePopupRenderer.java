package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Draws the autocomplete popup above the chat bar.
@SideOnly(Side.CLIENT)
public final class AutocompletePopupRenderer {

    private static final int LINE_HEIGHT = 12;
    private static final int CHAT_BAR_HEIGHT = 16;
    private static final int TEXT_LEFT = 2;
    private static final int PADDING = 2;

    /// GuiChat places its input field at x = 4 and disables its background, so the text is drawn at x = 4 with no
    /// extra inset. Anything drawn inline with the input text must be offset by this.
    private static final int INPUT_TEXT_LEFT = 4;
    private static final int INPUT_TEXT_BOTTOM_OFFSET = 12;

    private static final int BACKGROUND_COLOUR = 0xFF000000;
    private static final int SELECTED_COLOUR = 0xedecda;
    private static final int UNSELECTED_COLOUR = 0x9d9c8e;

    private AutocompletePopupRenderer() {}

    /// Draws the popup for the given state. Does nothing when the popup is hidden.
    ///
    /// Assumption: the state has been updated with the same text and caret.
    public static void draw(AutocompleteStateMachine state, String text, int caret, int screenHeight) {
        AutocompleteSuggestions suggestions = state.getSuggestions();

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        String selectionContinuation = null;

        if (suggestions != null && AutocompleteConfig.showHint) {
            selectionContinuation = state.getSuggestionContinuation();

            if (selectionContinuation != null) {
                font.drawString(
                        selectionContinuation,
                        INPUT_TEXT_LEFT + font.getStringWidth(text),
                        screenHeight - INPUT_TEXT_BOTTOM_OFFSET,
                        0xFF666666);
            }
        }

        if (state.getContinuation() != null && AutocompleteConfig.showHint) {
            // The selection continuation is only drawn when the caret is at the end, so it always directly follows text
            String prefix = selectionContinuation != null ? text + selectionContinuation : text;

            int x = INPUT_TEXT_LEFT + font.getStringWidth(prefix);

            font.drawString(state.getContinuation(), x, screenHeight - INPUT_TEXT_BOTTOM_OFFSET, 0xFF666666);
        }

        if (!state.hasVisibleSuggestions()) return;

        assert suggestions != null;

        List<String> replacements = suggestions.replacements();
        int selected = state.getSelectedIndex();
        int firstVisible = getFirstVisible(selected, replacements.size(), AutocompleteConfig.popupRows);
        List<String> visible = replacements
                .subList(firstVisible, Math.min(replacements.size(), firstVisible + AutocompleteConfig.popupRows));

        String prefix = text.substring(0, state.getSnipStart(text));
        String replaced = text.substring(0, state.getSnipEnd(text, caret));

        int left = TEXT_LEFT + font.getStringWidth(prefix);
        int right = TEXT_LEFT + getContentWidth(font, prefix, replaced, visible) + PADDING * 2;
        int bottom = screenHeight - CHAT_BAR_HEIGHT;
        int top = bottom - visible.size() * LINE_HEIGHT;

        Gui.drawRect(left, top, right, bottom, BACKGROUND_COLOUR);

        for (int i = 0; i < visible.size(); i++) {
            int colour = firstVisible + i == selected ? SELECTED_COLOUR : UNSELECTED_COLOUR;

            font.drawString(visible.get(i), left + PADDING, top + i * LINE_HEIGHT + 1, colour);
        }
    }

    /// Keeps the selection in the middle of the popup, except near the ends of the list, where the popup stays full
    /// instead of shrinking.
    private static int getFirstVisible(int selected, int count, int rows) {
        int centred = selected - rows / 2;

        return Math.max(0, Math.min(centred, count - rows));
    }

    /// The width from the start of the text to the right edge of the popup's contents. The popup must cover both the
    /// widest replacement and the text that will be replaced.
    private static int getContentWidth(FontRenderer font, String prefix, String replaced, List<String> visible) {
        int width = font.getStringWidth(replaced);

        for (String replacement : visible) {
            width = Math.max(width, font.getStringWidth(prefix + replacement));
        }

        return width;
    }
}
