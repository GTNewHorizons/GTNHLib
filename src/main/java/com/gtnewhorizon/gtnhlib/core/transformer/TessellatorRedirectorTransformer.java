package com.gtnewhorizon.gtnhlib.core.transformer;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

// This transformer can be instantiated both by RFB or FML,
// since RFB uses a different ClassLoader, this transformer
// should depend on a minimal amount of classes to avoid
// cascading classloading on the wrong ClassLoader which can
// created un-intended behaviors and make debugging harder.
public final class TessellatorRedirectorTransformer implements IClassTransformer {

    private static final String TessellatorClass = "net/minecraft/client/renderer/Tessellator";
    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(TessellatorClass);
    private static final String[] exclusions = { "org.lwjgl", "com.gtnewhorizons.angelica.glsm.",
            "com.gtnewhorizons.angelica.transform", "me.eigenraven.lwjgl3ify", "com.gtnewhorizon.gtnhlib",
            "net.minecraft.client.renderer.Tessellator" };

    public static String[] getTransformerExclusions() {
        return exclusions.clone();
    }

    public static boolean shouldRfbTransform(byte[] basicClass) {
        return cstPoolParser.find(basicClass, true);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        for (String exclusion : exclusions) {
            if (transformedName.startsWith(exclusion)) return basicClass;
        }

        if (!cstPoolParser.find(basicClass, true)) {
            return basicClass;
        }

        // Keep in sync with com.gtnewhorizons.angelica.loading.rfb.RedirectorTransformerWrapper
        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        final boolean changed = transformClassNode(transformedName, cn);
        if (changed) {
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        }
        return basicClass;
    }

    /**
     * @return Was the class changed?
     */
    public boolean transformClassNode(String transformedName, ClassNode cn) {
        if (cn == null) {
            return false;
        }
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            if (transformedName.equals("net.minecraft.client.renderer.OpenGlHelper")
                    && (mn.name.equals("glBlendFunc") || mn.name.equals("func_148821_a"))) {
                continue;
            }
            for (AbstractInsnNode node : mn.instructions.toArray()) {
                if (node.getOpcode() == Opcodes.GETSTATIC && node instanceof FieldInsnNode fNode) {
                    if ((fNode.name.equals("field_78398_a") || fNode.name.equals("instance"))
                            && fNode.owner.equals(TessellatorClass)) {
                        // package com.gtnewhorizon.gtnhlib.client.renderer;
                        mn.instructions.set(
                                node,
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "com/gtnewhorizon/gtnhlib/client/renderer/TessellatorManager",
                                        "get",
                                        "()Lnet/minecraft/client/renderer/Tessellator;",
                                        false));
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }
}
