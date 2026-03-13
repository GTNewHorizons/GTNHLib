package com.gtnewhorizon.gtnhlib.core.rfb.transformers;

import java.util.jar.Manifest;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.core.fml.transformers.TessellatorTransformer;
import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;
import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.ExtensibleClassLoader;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

/** RFB wrapper for {@link TessellatorTransformer} */
public class TessellatorTransformerWrapper implements RfbClassTransformer {

    public final TessellatorTransformer inner = new TessellatorTransformer();

    @Pattern("[a-z0-9-]+")
    @Override
    public @NotNull String id() {
        return "tessellator";
    }

    @Override
    public @NotNull String @Nullable [] sortAfter() {
        // Must run after mixins to ensure Tessellator class is fully prepared
        return new String[] { "mixin:mixin" };
    }

    @Override
    public @NotNull String @Nullable [] sortBefore() {
        // Must run before the redirector that modifies calls TO Tessellator
        return new String[] { "redirector" };
    }

    @Override
    public @NotNull String @Nullable [] additionalExclusions() {
        return new String[0];
    }

    @Override
    public boolean shouldTransformClass(@NotNull ExtensibleClassLoader classLoader,
            @NotNull RfbClassTransformer.Context context, @Nullable Manifest manifest, @NotNull String className,
            @NotNull ClassNodeHandle classNode) {
        return classNode.isPresent() && "net.minecraft.client.renderer.Tessellator".equals(className);
    }

    @Override
    public void transformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull RfbClassTransformer.Context context,
            @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        final boolean changed = inner.transformClassNode(classNode.getNode());
        if (changed) {
            classNode.computeFrames();
            GTNHLibClassDump.dumpRFBClass(className, classNode, this);
        }
    }
}
