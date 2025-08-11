package com.gtnewhorizon.gtnhlib.rfb;

import net.minecraft.launchwrapper.Launch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizons.retrofuturabootstrap.api.PluginContext;
import com.gtnewhorizons.retrofuturabootstrap.api.RetroFuturaBootstrap;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbPlugin;

public class GTNHLibRfbPlugin implements RfbPlugin {

    @Override
    public void onConstruction(@NotNull PluginContext ctx) {
        Launch.blackboard.put("gtnhlib.rfbPluginLoaded", Boolean.TRUE);
    }

    @Override
    public @NotNull RfbClassTransformer @Nullable [] makeTransformers() {
        final boolean isServer = (null
                == RetroFuturaBootstrap.API.launchClassLoader().findClassMetadata("net.minecraft.client.main.Main"));
        if (isServer) {
            return null;
        }
        return new RfbClassTransformer[] { new RFBTessellatorRedirector() };
    }
}
