package com.gtnewhorizon.gtnhlib.mixins.early.models;

import com.gtnewhorizon.gtnhlib.client.model.loading.ducks.GlobalResourceManager;
import java.util.Map;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("UnusedMixin")
@Mixin(SimpleReloadableResourceManager.class)
public interface SRRMAccessor extends GlobalResourceManager {

    @Override
    @Accessor("domainResourceManagers")
    Map<String, FallbackResourceManager> nhlib$getDomainResourceManagers();
}
