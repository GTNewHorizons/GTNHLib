package com.gtnewhorizon.gtnhlib.rfb;

import java.util.jar.Manifest;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.core.transformer.TessellatorRedirectorTransformer;
import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.ExtensibleClassLoader;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

/** RFB wrapper for {@link TessellatorRedirectorTransformer} */
public class TessellatorRedirectorTransformerWrapper implements RfbClassTransformer {

    private final TessellatorRedirectorTransformer inner = new TessellatorRedirectorTransformer();

    @Pattern("[a-z0-9-]+")
    @Override
    public @NotNull String id() {
        return "redirector";
    }

    @Override
    public @NotNull String @Nullable [] sortAfter() {
        return new String[] { "*", "mixin:mixin" };
    }

    @Override
    public @NotNull String @Nullable [] sortBefore() {
        return new String[] { "lwjgl3ify:redirect" };
    }

    @Override
    public @NotNull String @Nullable [] additionalExclusions() {
        return TessellatorRedirectorTransformer.getTransformerExclusions();
    }

    @Override
    public boolean shouldTransformClass(@NotNull ExtensibleClassLoader classLoader,
            @NotNull RfbClassTransformer.Context context, @Nullable Manifest manifest, @NotNull String className,
            @NotNull ClassNodeHandle classNode) {
        if (!classNode.isPresent()) {
            return false;
        }
        if (!classNode.isOriginal()) {
            // If a class is already a transformed ClassNode, conservatively continue processing.
            return true;
        }
        return TessellatorRedirectorTransformer.shouldRfbTransform(classNode.getOriginalBytes());
    }

    @Override
    public void transformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull RfbClassTransformer.Context context,
            @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        final boolean changed = inner.transformClassNode(className, classNode.getNode());
        if (changed) {
            classNode.computeMaxs();
        }
    }
}
