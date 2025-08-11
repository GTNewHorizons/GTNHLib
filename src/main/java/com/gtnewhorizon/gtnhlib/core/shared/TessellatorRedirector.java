package com.gtnewhorizon.gtnhlib.core.shared;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class TessellatorRedirector {

    private static final String TessellatorClass = "net/minecraft/client/renderer/Tessellator";
    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(TessellatorClass);
    private static final String[] exclusions = { "org.lwjgl", "com.gtnewhorizons.angelica.glsm.",
            "com.gtnewhorizons.angelica.transform", "me.eigenraven.lwjgl3ify", "com.gtnewhorizon.gtnhlib",
            "net.minecraft.client.renderer.Tessellator" };

    public static String[] getTransformerExclusions() {
        return exclusions.clone();
    }

    public static boolean shouldTransform(byte[] basicClass) {
        return cstPoolParser.find(basicClass, true);
    }

    /**
     * @return Was the class changed?
     */
    public static boolean transformClassNode(String transformedName, ClassNode cn) {
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
