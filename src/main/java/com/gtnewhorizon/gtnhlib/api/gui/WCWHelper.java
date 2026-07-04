package com.gtnewhorizon.gtnhlib.api.gui;

import org.jetbrains.annotations.ApiStatus;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.StartupQuery;

public class WCWHelper {

    @ApiStatus.Internal
    public static void fireWarnings() {
        WorldConversionWarningManager.WARNINGS.forEach((id, wcw) -> {
            if (wcw.shouldShow()) {
                boolean confirmed = StartupQuery
                        .confirm(FMLCommonHandler.instance().getSide().isServer() ? wcw.getServerMessage() : id);
                if (!confirmed) StartupQuery.abort();
            }
        });
    }
}
