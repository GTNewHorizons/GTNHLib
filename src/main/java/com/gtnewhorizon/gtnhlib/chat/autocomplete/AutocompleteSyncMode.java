package com.gtnewhorizon.gtnhlib.chat.autocomplete;

public enum AutocompleteSyncMode {
    /// Autocomplete runs entirely on the client with no server sync.
    NONE,
    /// Autocomplete runs entirely on the server with no client polling.
    FULL,
    /// Autocomplete runs on the client as a best-effort guess, then updates later with the real results.
    BEST_EFFORT,
    /// Autocomplete runs on client and server, and the results are later merged by
    /// [ChatAutocompleteDriver#merge(AutocompleteSuggestions, AutocompleteSuggestions)].
    MERGE
}
