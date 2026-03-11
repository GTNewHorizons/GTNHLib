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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;
import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;

public class DarkModeFixTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLib|DarkModeFixTransformer");

    private static final String GUI_CONTAINER = "net.minecraft.client.gui.inventory.GuiContainer";
    private static final String FONT_RENDERER = "net.minecraft.client.gui.FontRenderer";
    private static final String DARKMODE_CONTROLLER = "com/gtnewhorizon/gtnhlib/client/ResourcePackDarkModeFix/DarkModeFixController";
    private static final String COLOR_PROCESSOR = "com/gtnewhorizon/gtnhlib/client/ResourcePackDarkModeFix/DarkModeFixColorProcessor";

    private static final String[] DRAW_SCREEN_NAMES = { "drawScreen", "func_73863_a" };
    private static final String[] FOREGROUND_NAMES = { "drawGuiContainerForegroundLayer", "func_146979_b" };
    private static final String[] RENDER_STRING_AT_POS_NAMES = { "renderStringAtPos", "func_78255_a" };
    private static final String[] RENDER_STRING_NAMES = { "renderString", "func_78258_a" };

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
            if (!matches(mn.name, DRAW_SCREEN_NAMES) || !"(IIF)V".equals(mn.desc)) {
                continue;
            }

            MethodInsnNode target = findForegroundCall(mn);
            if (target == null) {
                continue;
            }

            LabelNode start = new LabelNode();
            LabelNode end = new LabelNode();
            LabelNode handler = new LabelNode();
            LabelNode after = new LabelNode();

            // Set inContainerGui = true right before drawGuiContainerForegroundLayer.
            InsnList before = new InsnList();
            before.add(start);
            before.add(new InsnNode(Opcodes.ICONST_1));
            before.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, "setInContainerGui", "(Z)V", false));
            mn.instructions.insertBefore(target, before);

            // Reset the flag in normal flow and in the exception handler (try/finally style).
            InsnList afterCall = new InsnList();
            afterCall.add(end);
            afterCall.add(new InsnNode(Opcodes.ICONST_0));
            afterCall.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, "setInContainerGui", "(Z)V", false));
            afterCall.add(new JumpInsnNode(Opcodes.GOTO, after));
            afterCall.add(handler);
            afterCall.add(new InsnNode(Opcodes.ICONST_0));
            afterCall.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, "setInContainerGui", "(Z)V", false));
            afterCall.add(new InsnNode(Opcodes.ATHROW));
            afterCall.add(after);
            mn.instructions.insert(target, afterCall);

            mn.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
            return true;
        }
        return false;
    }

    private MethodInsnNode findForegroundCall(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode method = (MethodInsnNode) insn;
            if (matches(method.name, FOREGROUND_NAMES) && "(II)V".equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private boolean transformFontRenderer(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (!matches(mn.name, RENDER_STRING_NAMES) || !"(Ljava/lang/String;IIIZ)I".equals(mn.desc)) {
                continue;
            }

            // Adjust the incoming color parameter before alpha/shadow handling and GL color math.
            InsnList inject = new InsnList();
            inject.add(new VarInsnNode(Opcodes.ILOAD, 4));
            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COLOR_PROCESSOR, "adjustColorOpaque", "(I)I", false));
            inject.add(new VarInsnNode(Opcodes.ISTORE, 4));
            AbstractInsnNode first = mn.instructions.getFirst();
            if (first == null) {
                continue;
            }
            mn.instructions.insertBefore(first, inject);
            // Also adjust color codes applied inside renderStringAtPos (safe local-variable rewrite).
            transformRenderStringAtPosColorCode(cn);
            return true;
        }
        LOGGER.warn("FontRenderer renderString method not found in {}", cn.name);
        return false;
    }

    private void transformRenderStringAtPosColorCode(ClassNode cn) { // TODO Check variable index or surrounding
                                                                     // instructions
        for (MethodNode mn : cn.methods) {
            if (!matches(mn.name, RENDER_STRING_AT_POS_NAMES)) {
                continue;
            }
            boolean changed = false;
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() != Opcodes.IALOAD) {
                    continue;
                }
                AbstractInsnNode next = insn.getNext();
                if (next == null || next.getOpcode() != Opcodes.ISTORE) {
                    continue;
                }
                VarInsnNode store = (VarInsnNode) next;
                InsnList inject = new InsnList();
                inject.add(new VarInsnNode(Opcodes.ILOAD, store.var));
                inject.add(
                        new MethodInsnNode(Opcodes.INVOKESTATIC, COLOR_PROCESSOR, "adjustColorOpaque", "(I)I", false));
                inject.add(new VarInsnNode(Opcodes.ISTORE, store.var));
                mn.instructions.insert(store, inject);
                changed = true;
            }
            if (changed) {
                return;
            }
        }
    }

    private boolean matches(String name, String[] options) {
        for (String option : options) {
            if (option.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean endsWithFontRenderer(String name) {
        return name != null && name.endsWith("FontRenderer");
    }
}
