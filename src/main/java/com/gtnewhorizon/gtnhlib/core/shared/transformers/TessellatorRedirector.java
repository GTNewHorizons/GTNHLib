package com.gtnewhorizon.gtnhlib.core.shared.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

import java.util.ListIterator;

public final class TessellatorRedirector {

    private static final String TessellatorClass = "net/minecraft/client/renderer/Tessellator";

    private final ClassConstantPoolParser cstPoolParser;
    private final String Tessellator$instance;

    public TessellatorRedirector(boolean isObf) {
        cstPoolParser = new ClassConstantPoolParser(TessellatorClass);
        Tessellator$instance = isObf ? "field_78398_a" : "instance";
    }

    public String[] getTransformerExclusions() {
        return new String[] { "org.lwjgl", "com.gtnewhorizons.angelica.glsm.", "com.gtnewhorizons.angelica.loading",
                "me.eigenraven.lwjgl3ify", "com.gtnewhorizon.gtnhlib", "net.minecraft.client.renderer.Tessellator" };
    }

    public boolean shouldTransform(byte[] basicClass) {
        return cstPoolParser.find(basicClass);
    }

    /**
     * @return Was the class changed?
     */
    public boolean transformClassNode(String className, ClassNode cn) {
        if (cn == null) {
            return false;
        }
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            ListIterator<AbstractInsnNode> iter = mn.instructions.iterator();
            while (iter.hasNext()) {
                if (iter.next() instanceof FieldInsnNode node && node.getOpcode() == Opcodes.GETSTATIC) {
                    if (TessellatorClass.equals(node.owner) && node.name.equals(Tessellator$instance)) {
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
