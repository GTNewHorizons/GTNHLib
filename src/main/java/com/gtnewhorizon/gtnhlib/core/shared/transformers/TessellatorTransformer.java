package com.gtnewhorizon.gtnhlib.core.shared.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class TessellatorTransformer implements Opcodes {

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
                inject.add(new VarInsnNode(ALOAD, 0)); // this
                inject.add(
                        new MethodInsnNode(
                                INVOKESTATIC,
                                "com/gtnewhorizon/gtnhlib/client/renderer/TessellatorManager",
                                "shouldInterceptDraw",
                                "(Lnet/minecraft/client/renderer/Tessellator;)Z",
                                false));
                inject.add(new JumpInsnNode(IFEQ, skipLabel)); // if false, skip

                // return TessellatorManager.interceptDraw(this);
                inject.add(new VarInsnNode(ALOAD, 0)); // this
                inject.add(
                        new MethodInsnNode(
                                INVOKESTATIC,
                                "com/gtnewhorizon/gtnhlib/client/renderer/TessellatorManager",
                                "interceptDraw",
                                "(Lnet/minecraft/client/renderer/Tessellator;)I",
                                false));
                inject.add(new InsnNode(IRETURN));
                inject.add(skipLabel);

                // Insert at the start of the method
                mn.instructions.insert(inject);
                return true;
            }
        }
        return false;
    }
}
