package com.gtnewhorizon.gtnhlib.client.model.loading;

import java.util.Map;

import net.minecraft.client.resources.FallbackResourceManager;

public interface GlobalResourceManager {

    Map<String, FallbackResourceManager> nhlib$getDomainResourceManagers();
}
