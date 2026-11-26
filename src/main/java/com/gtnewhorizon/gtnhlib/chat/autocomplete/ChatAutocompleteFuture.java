package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.concurrent.Future;

import org.jetbrains.annotations.Nullable;

public interface ChatAutocompleteFuture extends Future<@Nullable AutocompleteSuggestions> {

    boolean hasPreliminaryResults();

    @Nullable
    AutocompleteSuggestions getPreliminaryResults();

}
