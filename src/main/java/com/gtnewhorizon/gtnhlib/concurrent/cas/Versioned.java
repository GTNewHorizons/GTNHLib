package com.gtnewhorizon.gtnhlib.concurrent.cas;

import org.jetbrains.annotations.NotNull;

import com.github.bsideup.jabel.Desugar;

/** A snapshot value paired with the version stamp at the time it was read. */
@Desugar
public record Versioned<T> (@NotNull T value, long version) {}
