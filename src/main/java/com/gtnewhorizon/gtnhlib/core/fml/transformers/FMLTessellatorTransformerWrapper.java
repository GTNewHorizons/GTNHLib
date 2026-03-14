package com.gtnewhorizon.gtnhlib.core.fml.transformers;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;
import com.gtnewhorizon.gtnhlib.core.shared.transformers.TessellatorTransformer;

/** FML IClassTransformer wrapper for {@link TessellatorTransformer} */
public class FMLTessellatorTransformerWrapper implements IClassTransformer {

    private final TessellatorTransformer inner = new TessellatorTransformer();

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        // Only transform the Tessellator class itself
        if (!transformedName.equals("net.minecraft.client.renderer.Tessellator")) {
            return basicClass;
        }

        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        final boolean changed = inner.transformClassNode(cn);
        if (changed) {
            // Use COMPUTE_FRAMES since we're modifying control flow with jumps
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            final byte[] transformedBytes = cw.toByteArray();
            GTNHLibClassDump.dumpClass(transformedName, basicClass, transformedBytes, this);
            return transformedBytes;
        }
        return basicClass;
    }
}
