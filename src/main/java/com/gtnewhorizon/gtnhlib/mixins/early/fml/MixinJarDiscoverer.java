package com.gtnewhorizon.gtnhlib.mixins.early.fml;

import java.util.zip.ZipEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = cpw.mods.fml.common.discovery.JarDiscoverer.class, remap = false)
public class MixinJarDiscoverer {

    @Redirect(
            method = "discover(Lcpw/mods/fml/common/discovery/ModCandidate;Lcpw/mods/fml/common/discovery/ASMDataTable;)Ljava/util/List;",
            at = @At(ordinal = 1, value = "INVOKE", target = "Ljava/util/zip/ZipEntry;getName()Ljava/lang/String;"),
            require = 1)
    private String gtnhlib$skipMultiReleaseEntries(ZipEntry ze) {
        String name = ze.getName();
        if (name != null && name.startsWith("META-INF/versions/")) {
            return "__MACOSX_ignoreme";
        }
        return name;
    }
}
