package com.gtnewhorizon.gtnhlib.rfb;

import java.util.jar.Manifest;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.core.fml.transformers.TessellatorTransformer;
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
        return TessellatorTransformer.getTransformerExclusions().toArray(new String[0]);
    }

    @Override
    public boolean shouldTransformClass(@NotNull ExtensibleClassLoader classLoader,
            @NotNull RfbClassTransformer.Context context, @Nullable Manifest manifest, @NotNull String className,
            @NotNull ClassNodeHandle classNode) {
        // Only transform the Tessellator class itself
        if (!"net.minecraft.client.renderer.Tessellator".equals(className)) {
            return false;
        }
        if (!classNode.isPresent()) {
            return false;
        }
        if (!classNode.isOriginal()) {
            // If a class is already a transformed ClassNode, conservatively continue processing.
            return true;
        }
        return inner.shouldRfbTransform(classNode.getOriginalBytes());
    }

    @Override
    public void transformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull RfbClassTransformer.Context context,
            @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        // Double-check we're only transforming Tessellator
        if (!"net.minecraft.client.renderer.Tessellator".equals(className)) {
            return;
        }
        final boolean changed = inner.transformClassNode(classNode.getNode());
        if (changed) {
            classNode.computeFrames();
        }
    }
}
