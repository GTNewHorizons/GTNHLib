package com.gtnewhorizon.gtnhlib.core.shared;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public final class TessellatorRedirector {

    private static final String TessellatorClass = "net/minecraft/client/renderer/Tessellator";

    private final ClassConstantPoolParser cstPoolParser;
    private final String OpenGlHelper$glBlendFunc;
    private final String Tessellator$instance;

    public TessellatorRedirector(boolean isObf) {
        cstPoolParser = new ClassConstantPoolParser(TessellatorClass);
        OpenGlHelper$glBlendFunc = isObf ? "func_148821_a" : "glBlendFunc";
        Tessellator$instance = isObf ? "field_78398_a" : "instance";
    }

    public String[] getTransformerExclusions() {
        return new String[] { "org.lwjgl", "com.gtnewhorizons.angelica.glsm.", "com.gtnewhorizons.angelica.transform",
                "me.eigenraven.lwjgl3ify", "com.gtnewhorizon.gtnhlib", "net.minecraft.client.renderer.Tessellator" };
    }

    public boolean shouldTransform(byte[] basicClass) {
        return cstPoolParser.find(basicClass, true);
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
            if ("net.minecraft.client.renderer.OpenGlHelper".equals(className)
                    && mn.name.equals(OpenGlHelper$glBlendFunc)) {
                continue;
            }
            for (AbstractInsnNode node : mn.instructions.toArray()) {
                if (node.getOpcode() == Opcodes.GETSTATIC && node instanceof FieldInsnNode fNode) {
                    if (TessellatorClass.equals(fNode.owner) && fNode.name.equals(Tessellator$instance)) {
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
