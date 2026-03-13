package com.gtnewhorizon.gtnhlib.core.fml.transformers;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;

/**
 * Transformer the Tessellator and inject draw() interception.
 */
public class TessellatorTransformer implements IClassTransformer {

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
        final boolean changed = transformClassNode(cn);
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

    /**
     * Injects intercept check at the start of Tessellator.draw() to handle display list compilation.
     *
     * @param cn The Tessellator ClassNode
     * @return true if the class was modified
     */
    public boolean transformClassNode(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            // Handle both deobfuscated and obfuscated names
            if (mn.desc.equals("()I") && (mn.name.equals("draw") || mn.name.equals("func_78381_a"))) {
                InsnList inject = new InsnList();
                LabelNode skipLabel = new LabelNode();

                // if (TessellatorManager.shouldInterceptDraw(this))
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                inject.add(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "com/gtnewhorizon/gtnhlib/client/renderer/TessellatorManager",
                                "shouldInterceptDraw",
                                "(Lnet/minecraft/client/renderer/Tessellator;)Z",
                                false));
                inject.add(new JumpInsnNode(Opcodes.IFEQ, skipLabel)); // if false, skip

                // return TessellatorManager.interceptDraw(this);
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                inject.add(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "com/gtnewhorizon/gtnhlib/client/renderer/TessellatorManager",
                                "interceptDraw",
                                "(Lnet/minecraft/client/renderer/Tessellator;)I",
                                false));
                inject.add(new InsnNode(Opcodes.IRETURN));
                inject.add(skipLabel);

                // Insert at the start of the method
                mn.instructions.insert(inject);
                return true;
            }
        }
        return false;
    }
}
