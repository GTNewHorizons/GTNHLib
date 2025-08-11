package com.gtnewhorizon.gtnhlib.core.transformer;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;
import com.gtnewhorizon.gtnhlib.core.shared.TessellatorRedirector;

public class TessellatorRedirectorTransformer implements IClassTransformer {

    private final String[] exclusions;

    public TessellatorRedirectorTransformer() {
        exclusions = TessellatorRedirector.getTransformerExclusions();
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        for (String exclusion : exclusions) {
            if (transformedName.startsWith(exclusion)) return basicClass;
        }

        if (!TessellatorRedirector.shouldTransform(basicClass)) {
            return basicClass;
        }

        // Keep in sync with com.gtnewhorizons.angelica.loading.rfb.RedirectorTransformerWrapper
        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        final boolean changed = TessellatorRedirector.transformClassNode(transformedName, cn);
        if (changed) {
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            final byte[] transformedBytes = cw.toByteArray();
            GTNHLibClassDump.dumpClass(transformedName, basicClass, transformedBytes, this);
            return transformedBytes;
        }
        return basicClass;
    }
}
