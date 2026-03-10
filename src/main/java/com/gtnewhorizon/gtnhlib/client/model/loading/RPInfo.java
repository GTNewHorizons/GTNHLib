package com.gtnewhorizon.gtnhlib.client.model.loading;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/// Tiny holder for data coming from the resourcepack scanning
public record RPInfo(@NotNull List<String> textureNames, @NotNull List<String> modeledBlocks) {}
