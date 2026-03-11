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
    private static final String GUI_SCREEN = "net.minecraft.client.gui.GuiScreen";
    private static final String GUI_NEW_CHAT = "net.minecraft.client.gui.GuiNewChat";
    private static final String FONT_RENDERER = "net.minecraft.client.gui.FontRenderer";
    private static final String RENDER_ITEM = "net.minecraft.client.renderer.entity.RenderItem";
    private static final String DARKMODE_CONTROLLER = "com/gtnewhorizon/gtnhlib/client/ResourcePackDarkModeFix/DarkModeFixController";
    private static final String COLOR_PROCESSOR = "com/gtnewhorizon/gtnhlib/client/ResourcePackDarkModeFix/DarkModeFixColorProcessor";

    private static final String[] DRAW_SCREEN_NAMES = { "drawScreen", "func_73863_a" };
    private static final String[] FOREGROUND_NAMES = { "drawGuiContainerForegroundLayer", "func_146979_b" };
    private static final String[] DRAW_HOVERING_NAMES = { "drawHoveringText", "func_146283_a" };
    private static final String[] RENDER_TOOLTIP_NAMES = { "renderToolTip", "func_146285_a" };
    private static final String[] RENDER_ITEM_OVERLAY_NAMES = { "renderItemOverlayIntoGUI", "func_94148_a" };
    private static final String[] DRAW_CHAT_NAMES = { "drawChat", "func_146230_a" };
    private static final String[] RENDER_STRING_AT_POS_NAMES = { "renderStringAtPos", "func_78255_a" };
    private static final String[] RENDER_STRING_NAMES = { "renderString", "func_78258_a" };
    private static final String[] TEXT_COLOR_FIELD_NAMES = { "textColor", "field_78304_r" };

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
        boolean isGuiScreen = GUI_SCREEN.equals(transformedName) || GUI_SCREEN.equals(name);
        boolean isGuiNewChat = GUI_NEW_CHAT.equals(transformedName) || GUI_NEW_CHAT.equals(name);
        boolean isFontRenderer = FONT_RENDERER.equals(transformedName) || FONT_RENDERER.equals(name)
                || endsWithFontRenderer(transformedName)
                || endsWithFontRenderer(name);
        boolean isRenderItem = RENDER_ITEM.equals(transformedName) || RENDER_ITEM.equals(name);
        boolean isHeuristicFontRenderer = !isFontRenderer && FONT_METHOD_MATCHER.find(basicClass)
                && FONT_FIELD_MATCHER.find(basicClass);

        if (!isGuiContainer && !isGuiScreen
                && !isGuiNewChat
                && !isFontRenderer
                && !isRenderItem
                && !isHeuristicFontRenderer) {
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
        cr.accept(cn, 0);

        boolean changed;
        if (isGuiContainer) {
            changed = transformGuiContainer(cn);
        } else if (isGuiScreen) {
            changed = transformGuiScreen(cn);
        } else if (isGuiNewChat) {
            changed = transformGuiNewChat(cn);
        } else if (isRenderItem) {
            changed = transformRenderItem(cn);
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

            // Treat container screens as general GUI screens too.
            wrapWithFlag(mn, "setInGuiScreen");

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

    private boolean transformGuiScreen(ClassNode cn) {
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            if (matches(mn.name, DRAW_SCREEN_NAMES) && "(IIF)V".equals(mn.desc)) {
                changed |= wrapWithFlag(mn, "setInGuiScreen");
            } else if (matches(mn.name, DRAW_HOVERING_NAMES)
                    && "(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V".equals(mn.desc)) {
                        changed |= wrapWithFlag(mn, "setInTooltip");
                    } else
                if (matches(mn.name, RENDER_TOOLTIP_NAMES) && "(Lnet/minecraft/item/ItemStack;II)V".equals(mn.desc)) {
                    changed |= wrapWithFlag(mn, "setInTooltip");
                }
        }
        return changed;
    }

    private boolean transformRenderItem(ClassNode cn) {
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            if (matches(mn.name, RENDER_ITEM_OVERLAY_NAMES)
                    && "(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"
                            .equals(mn.desc)) {
                changed |= wrapWithFlag(mn, "setInItemOverlay");
            } else if (matches(mn.name, RENDER_ITEM_OVERLAY_NAMES)
                    && "(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;II)V"
                            .equals(mn.desc)) {
                                changed |= wrapWithFlag(mn, "setInItemOverlay");
                            }
        }
        return changed;
    }

    private boolean transformGuiNewChat(ClassNode cn) {
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            if (matches(mn.name, DRAW_CHAT_NAMES) && "(I)V".equals(mn.desc)) {
                changed |= wrapWithFlag(mn, "setInChat");
            }
        }
        return changed;
    }

    private boolean wrapWithFlag(MethodNode mn, String setterName) {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        LabelNode after = new LabelNode();

        InsnList before = new InsnList();
        before.add(start);
        before.add(new InsnNode(Opcodes.ICONST_1));
        before.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, setterName, "(Z)V", false));
        mn.instructions.insert(before);

        InsnList afterCall = new InsnList();
        afterCall.add(end);
        afterCall.add(new InsnNode(Opcodes.ICONST_0));
        afterCall.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, setterName, "(Z)V", false));
        afterCall.add(new JumpInsnNode(Opcodes.GOTO, after));
        afterCall.add(handler);
        afterCall.add(new InsnNode(Opcodes.ICONST_0));
        afterCall.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DARKMODE_CONTROLLER, setterName, "(Z)V", false));
        afterCall.add(new InsnNode(Opcodes.ATHROW));
        afterCall.add(after);
        mn.instructions.add(afterCall);

        mn.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
        return true;
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
            mn.instructions.insert(inject);
            // Also adjust color codes applied inside renderStringAtPos (safe local-variable rewrite).
            transformRenderStringAtPosColorCode(cn);
            return true;
        }
        LOGGER.warn("FontRenderer renderString method not found in {}", cn.name);
        return false;
    }

    private void transformRenderStringAtPosColorCode(ClassNode cn) {
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

    private String findTextColorFieldName(ClassNode cn) {
        if (cn.fields == null) {
            return null;
        }
        for (var field : cn.fields) {
            if ("I".equals(field.desc) && matches(field.name, TEXT_COLOR_FIELD_NAMES)) {
                return field.name;
            }
        }
        return null;
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
