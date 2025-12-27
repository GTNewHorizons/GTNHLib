package com.gtnewhorizon.gtnhlib.client.model.loading;

import java.util.Map;

import net.minecraft.client.resources.IResourceManager;

public interface GlobalResourceManager {

    Map<String, IResourceManager> nhlib$getDomainResourceManagers();
}
