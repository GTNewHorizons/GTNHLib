package com.gtnewhorizon.gtnhlib.core.fml.transformers;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;
import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;

public class DarkModeFixTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLib|DarkModeFixTransformer");

    private static final String GUI_CONTAINER = "net.minecraft.client.gui.inventory.GuiContainer";
    private static final String FONT_RENDERER = "net.minecraft.client.gui.FontRenderer";
    private static final String DARKMODE_CONTROLLER = "com/gtnewhorizon/gtnhlib/client/ResourcePackDarkModeFix/DarkModeFixController";
    private static final String COLOR_PROCESSOR = "com/gtnewhorizon/gtnhlib/client/ResourcePackDarkModeFix/DarkModeFixColorProcessor";
    private static final String GUI_FOREGROUND_METHOD = "drawGuiContainerForegroundLayer";
    private static final String GUI_FOREGROUND_METHOD_OBF = "func_146979_b";
    private static final String GUI_DRAW_SCREEN_METHOD = "drawScreen";
    private static final String GUI_DRAW_SCREEN_METHOD_OBF = "func_73863_a";

    private static final ClassConstantPoolParser FONT_METHOD_MATCHER = new ClassConstantPoolParser(
            "renderStringAtPos",
            "func_78255_a");
    private static final ClassConstantPoolParser FONT_FIELD_MATCHER = new ClassConstantPoolParser(
            "textColor",
            "field_78304_r");

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        boolean isGuiContainer = GUI_CONTAINER.equals(transformedName) || GUI_CONTAINER.equals(name);

        boolean isFontRenderer = FONT_RENDERER.equals(transformedName) || FONT_RENDERER.equals(name)
                || endsWithFontRenderer(transformedName)
                || endsWithFontRenderer(name);

        boolean isHeuristicFontRenderer = !isFontRenderer && FONT_METHOD_MATCHER.find(basicClass)
                && FONT_FIELD_MATCHER.find(basicClass);

        if (!isGuiContainer && !isFontRenderer && !isHeuristicFontRenderer) {
            return basicClass;
        }

        if (isHeuristicFontRenderer) {
            LOGGER.info("Transforming FontRenderer via heuristic match: {}", transformedName);
        } else if (isFontRenderer && !FONT_RENDERER.equals(transformedName)) {
            LOGGER.info("Transforming FontRenderer via name match: {} (transformed={})", name, transformedName);
        } else {
            LOGGER.info("Transforming {}", transformedName);
        }

        ClassReader cr = new ClassReader(basicClass);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        boolean changed;
        if (isGuiContainer) {
            changed = transformGuiContainer(cn);
        } else {
            changed = transformFontRenderer(cn);
        }

        if (!changed) {
            LOGGER.warn("No DarkModeFix injection applied for {}", transformedName);
            return basicClass;
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        byte[] transformedBytes = cw.toByteArray();
        GTNHLibClassDump.dumpClass(transformedName, basicClass, transformedBytes, this);
        LOGGER.info("Applied DarkModeFix injection to {}", transformedName);
        return transformedBytes;
    }

    private boolean transformGuiContainer(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (!"(IIF)V".equals(mn.desc)) {
                continue;
            }
            if (!GUI_DRAW_SCREEN_METHOD.equals(mn.name) && !GUI_DRAW_SCREEN_METHOD_OBF.equals(mn.name)) {
                continue;
            }

            MethodInsnNode foregroundCall = findForegroundCall(mn, cn.name);
            if (foregroundCall == null) {
                continue;
            }

            InsnList before = new InsnList();
            before.add(new InsnNode(Opcodes.ICONST_1));
            before.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, "setInContainerGui", "(Z)V", false));

            InsnList after = new InsnList();
            after.add(new InsnNode(Opcodes.ICONST_0));
            after.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, "setInContainerGui", "(Z)V", false));

            mn.instructions.insertBefore(foregroundCall, before);
            mn.instructions.insert(foregroundCall, after);

            LOGGER.info("Wrapped GuiContainer foreground call in {}{}", mn.name, mn.desc);
            return true;
        }

        return false;
    }

    private MethodInsnNode findForegroundCall(MethodNode mn, String ownerName) {
        String guiContainerOwner = GUI_CONTAINER.replace('.', '/');

        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }

            MethodInsnNode method = (MethodInsnNode) insn;

            if (method.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                continue;
            }

            if (!"(II)V".equals(method.desc)) {
                continue;
            }

            boolean ownerMatch = ownerName.equals(method.owner) || guiContainerOwner.equals(method.owner);
            if (!ownerMatch) {
                continue;
            }

            boolean nameMatch = GUI_FOREGROUND_METHOD.equals(method.name)
                    || GUI_FOREGROUND_METHOD_OBF.equals(method.name);
            if (nameMatch) {
                return method;
            }
        }

        return null;
    }

    private boolean transformFontRenderer(ClassNode cn) {
        boolean changed = false;

        for (MethodNode mn : cn.methods) {
            int colorParamIndex = -1;

            // FontRenderer.drawString(String text, int x, int y, int color)
            if ("(Ljava/lang/String;III)I".equals(mn.desc)) {
                colorParamIndex = 4;
            }
            // FontRenderer.drawString(String text, int x, int y, int color, boolean dropShadow)
            else if ("(Ljava/lang/String;IIIZ)I".equals(mn.desc)) {
                colorParamIndex = 4;
            }
            // Some font renderers may use float x/y
            else if ("(Ljava/lang/String;FFIZ)I".equals(mn.desc)) {
                colorParamIndex = 4;
            }

            if (colorParamIndex == -1) {
                continue;
            }

            AbstractInsnNode first = mn.instructions.getFirst();
            if (first == null) {
                continue;
            }

            InsnList inject = new InsnList();
            inject.add(new VarInsnNode(Opcodes.ILOAD, colorParamIndex));
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COLOR_PROCESSOR, "adjustColorOpaque", "(I)I", false));
            inject.add(new VarInsnNode(Opcodes.ISTORE, colorParamIndex));

            mn.instructions.insertBefore(first, inject);
            changed = true;

            LOGGER.info("Patched FontRenderer-like method: {}{}", mn.name, mn.desc);
        }

        if (!changed) {
            LOGGER.warn("No drawString-like FontRenderer method found in {}", cn.name);
        }

        return changed;
    }

    private boolean endsWithFontRenderer(String name) {
        return name != null && name.endsWith("FontRenderer");
    }
}
