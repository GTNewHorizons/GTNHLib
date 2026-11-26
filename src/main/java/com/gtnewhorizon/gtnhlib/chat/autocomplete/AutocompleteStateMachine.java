package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.command.ICommandSender;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/// Tracks the autocomplete popup for a single chat screen.
///
/// The machine has three effective states, which are derived from its fields:
/// - Hidden: there are no suggestions to show.
/// - Fresh: the shown suggestions were computed for the current text.
/// - Stale: suggestions are shown, but a newer request is still in flight. The old suggestions stay visible until the
/// new ones arrive so that the popup doesn't flicker while the player types or deletes characters.
///
/// Requirement: [#update(ICommandSender, String, int)] must be called with the current text before any other method,
/// so that the machine knows which text its suggestions apply to.
@Log4j2
public class AutocompleteStateMachine {

    @Nullable
    private String requestedText;
    private int requestedCaret;

    @Nullable
    private ChatAutocompleteFuture pending;

    /// The suggestions to show, or null when the popup is hidden. Never contains an empty replacement list.
    @Nullable
    @Getter
    private AutocompleteSuggestions suggestions;
    /// The in-line continuation text. This is decoupled from the suggestion because it may show up when there are no
    /// suggestions.
    @Nullable
    @Getter
    private String continuation;
    /// The index of the selected replacement. Only meaningful while the popup is visible.
    @Getter
    private int selectedIndex;

    /// Requests new suggestions if the text or caret changed since the last request, then applies the result of the
    /// in-flight request if it has finished.
    public void update(ICommandSender sender, @NotNull String text, int caret) {
        if (!text.equals(requestedText) || caret != requestedCaret) {
            request(sender, text, caret);
            autoselect();
        }

        poll();
    }

    private void request(ICommandSender sender, String text, int caret) {
        requestedText = text;
        requestedCaret = caret;

        pending = ChatAutocompleteRegistry.getSuggestions(sender, text, caret);

        if (pending.hasPreliminaryResults()) {
            show(pending.getPreliminaryResults());
        }
    }

    /// Re-selects a matching option when the player types something, if one matches.
    private void autoselect() {
        // Don't use hasVisibleSuggestions because that includes the 'token is complete' check, which would interfere
        // here
        if (suggestions == null) return;

        String withinSnip = getTokenPrior();

        var match = suggestions.replacements().stream().filter(s -> s.startsWith(withinSnip)).findFirst();

        if (match.isPresent()) {
            selectedIndex = suggestions.replacements().indexOf(match.get());
        }
    }

    private String safeSubstring(String text, int lo, int hi) {
        if (text == null || text.isEmpty()) return "";

        if (lo < 0) lo = 0;
        if (hi < 0) hi = 0;
        if (hi < lo) hi = lo;
        if (lo >= text.length()) lo = text.length();
        if (hi >= text.length()) hi = text.length();

        return lo == hi ? "" : text.substring(lo, hi);
    }

    /// Gets the text within the current token/autocomplete suggestion, prior to the caret. This returns an empty string
    /// for invalid states.
    private @NotNull String getTokenPrior() {
        if (suggestions == null || requestedText == null) return "";

        return safeSubstring(requestedText, suggestions.snipStart(), Math.min(suggestions.snipEnd(), requestedCaret));
    }

    private void poll() {
        if (pending == null || !pending.isDone()) return;

        AutocompleteSuggestions result = getResult(pending);
        pending = null;

        show(result);
    }

    @Nullable
    private static AutocompleteSuggestions getResult(Future<@Nullable AutocompleteSuggestions> future) {
        try {
            return future.get();
        } catch (CancellationException e) {
            return null;
        } catch (ExecutionException e) {
            log.error("Failed to get autocomplete suggestions", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /// Replaces the shown suggestions. The selected replacement is kept if it's still present, so that the selection
    /// doesn't jump around while the player types.
    private void show(@Nullable AutocompleteSuggestions result) {
        String previouslySelected = getSelected();

        continuation = result != null ? result.continuation() : null;

        if (result == null || result.replacements().isEmpty()) {
            suggestions = null;
            selectedIndex = 0;
            return;
        }

        suggestions = result;
        selectedIndex = previouslySelected == null ? 0 : Math.max(0, result.replacements().indexOf(previouslySelected));
        autoselect();
    }

    /// True when the popup should be drawn: there are suggestions, the popup is enabled, and the player hasn't already
    /// typed the selected suggestion in full.
    public boolean hasVisibleSuggestions() {
        return suggestions != null && AutocompleteConfig.showPopup
                && !Objects.equals(getTokenPrior(), suggestions.replacements().get(selectedIndex));
    }

    /// Gets the continuation text for the selected autocomplete suggestion. This trims the front of the token and only
    /// returns the tail if the suggestion is half-typed. This returns nothing if the caret is not at the end of the
    /// input text.
    @Nullable
    public String getSuggestionContinuation() {
        if (!hasVisibleSuggestions()) return null;

        if (requestedCaret < requestedText.length()) return null;

        String selected = suggestions.replacements().get(selectedIndex);

        String tokenPrior = getTokenPrior();
        if (tokenPrior.isEmpty() || !selected.startsWith(tokenPrior)) return null;

        return safeSubstring(selected, requestedCaret - suggestions.snipStart(), Integer.MAX_VALUE);
    }

    /// True when the shown suggestions were computed for an older version of the text.
    public boolean isStale() {
        return pending != null;
    }

    @Nullable
    public String getSelected() {
        return suggestions == null ? null : suggestions.replacements().get(selectedIndex);
    }

    /// Moves the selection by the given amount, clamped to the replacement list.
    ///
    /// @return False when the popup is hidden or disabled, in which case nothing happened.
    public boolean moveSelection(int delta) {
        if (suggestions == null || !AutocompleteConfig.showPopup) return false;

        int last = suggestions.replacements().size() - 1;
        selectedIndex = Math.max(0, Math.min(last, selectedIndex + delta));

        return true;
    }

    /// The start of the region that the selected replacement will overwrite, clamped to the given text.
    ///
    /// Assumption: the popup is visible.
    public int getSnipStart(String text) {
        return Math.min(suggestions.snipStart(), text.length());
    }

    /// The end of the region that the selected replacement will overwrite, clamped to the given text.
    ///
    /// Stale suggestions were computed for older text, so their snip end may lag behind what the player has typed
    /// since.
    /// The region is extended to the caret in that case, otherwise the characters typed after the request would be left
    /// dangling after the replacement.
    ///
    /// Assumption: the popup is visible.
    public int getSnipEnd(String text, int caret) {
        int end = isStale() ? Math.max(suggestions.snipEnd(), caret) : suggestions.snipEnd();

        return Math.max(getSnipStart(text), Math.min(end, text.length()));
    }

    /// Computes the text that results from inserting the selected replacement.
    ///
    /// @return The new text and caret, or null when the popup is hidden or disabled.
    @Nullable
    @Contract(pure = true)
    public Completion complete(String text, int caret) {
        String replacement = getSelected();

        if (replacement == null || !AutocompleteConfig.showPopup) return null;

        int start = getSnipStart(text);
        int end = getSnipEnd(text, caret);

        String completed = text.substring(0, start) + replacement + text.substring(end);

        return new Completion(completed, start + replacement.length());
    }

    @Desugar
    public record Completion(String text, int caret) {}
}
