package com.gtnewhorizon.gtnhlib.core.fml.transformers;

import static com.gtnewhorizon.gtnhlib.core.fml.transformers.BlockIconTransformer.Blockness.BLOCK;
import static com.gtnewhorizon.gtnhlib.core.fml.transformers.BlockIconTransformer.Blockness.NOT_A_BLOCK;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;

import java.util.HashSet;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.gtnewhorizon.gtnhlib.asm.ByteCodeUtil;
import com.gtnewhorizon.gtnhlib.asm.SafeClassWriter;
import com.gtnewhorizon.gtnhlib.core.shared.GTNHLibClassDump;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class BlockIconTransformer implements IClassTransformer {

    private static final String FIELD_NAME = "nhlib$isModeled";
    private static final String BOOL_DESC = BOOLEAN_TYPE.getDescriptor();
    private static final String BLOCK_CLASS = "net/minecraft/block/Block";
    private static final String BLOCK_MODEL_INFO = "com/gtnewhorizon/gtnhlib/api/BlockModelInfo";
    private static final String ISBRH_CLASS = "com/gtnewhorizon/gtnhlib/client/model/ModelISBRH";
    private static final String ISBRH_DESC = "L" + ISBRH_CLASS + ";";
    private static final String NEW_WORLD_DESC = "(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/util/IIcon;";
    private static final String MISSINGNO_DESC = "()Lnet/minecraft/util/IIcon;";

    // These are the targeted calls - a variety of getIcons declared by the Block class
    private static final String GI_WORLD = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;";
    private static final String GI_SIDE_META = "getIcon(II)Lnet/minecraft/util/IIcon;";
    private static final String OBF_SIDE_META = "func_149735_b(II)Lnet/minecraft/util/IIcon;";
    private static final String GI_SIDE = "getBlockTextureFromSide(I)Lnet/minecraft/util/IIcon;";
    private static final HashSet<String> GETICON_SIGS = new HashSet<>();
    static {
        GETICON_SIGS.add(GI_WORLD);
        GETICON_SIGS.add(GI_SIDE_META);
        GETICON_SIGS.add(OBF_SIDE_META);
        GETICON_SIGS.add(GI_SIDE);
    }

    private final ObjectOpenHashSet<String> blockFamily = new ObjectOpenHashSet<>();

    public BlockIconTransformer() {
        blockFamily.add(BLOCK_CLASS);
    }

    /// Implements the {@link com.gtnewhorizon.gtnhlib.api.BlockModelInfo} interface on the Block class,
    /// adds the boolean nhlib$isModeled field and adds associated getter/setter for it.
    /// <p>
    /// This is what our hook is injecting for the IBlockAccess variant:
    /// ```java
    /// if (this.nhlib$isModeled) return ModelISBRH.INSTANCE.getParticleIcon(world, x, y, z);
    /// ```
    /// <p>
    /// This is what our hook is injecting for the side:meta and just side variants:
    /// ```java
    /// if (this.nhlib$isModeled) return ModelISBRH.INSTANCE.getMissingIcon();
    /// ```
    /// This is because modeled blocks *need* world access to properly get their particle icons. Returning missingno
    /// fails loudly, allowing us to find and patch out the incorrect usages.
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        final var cr = new ClassReader(basicClass);
        final var blockness = checkForBlockChild(cr.getClassName(), cr.getSuperName());
        if (blockness == NOT_A_BLOCK) {
            return basicClass;
        }

        // Transform the class. We hit:
        // - getIcon and overloads (func_149735_b, getBlockTextureFromSide), inserting an early return if needed
        // TODO determine if the Block#blockIcon field needs to be redirected
        final var cn = new ClassNode();
        cr.accept(cn, 0);
        boolean transformed = false;
        if (blockness == BLOCK) {
            cn.fields.add(new FieldNode(ACC_PROTECTED, FIELD_NAME, BOOL_DESC, null, 0));
            cn.interfaces.add(BLOCK_MODEL_INFO);
            ByteCodeUtil.addGetterMethod(cn, "nhlib$isModeled", BLOCK_CLASS, FIELD_NAME, BOOL_DESC, null);
            ByteCodeUtil.addSetterMethod(cn, "nhlib$setModeled", BLOCK_CLASS, FIELD_NAME, BOOL_DESC, null);
            transformed = true;
        }
        for (var methodNode : cn.methods) {
            transformed |= hookMethod(cn, methodNode);
        }

        if (transformed) {
            final var cw = new SafeClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            final byte[] transformedBytes = cw.toByteArray();
            GTNHLibClassDump.dumpClass(transformedName, basicClass, transformedBytes, this);
            return transformedBytes;
        }
        return basicClass;
    }

    private boolean hookMethod(ClassNode cn, MethodNode mn) {
        final var signature = mn.name + mn.desc;
        if (!GETICON_SIGS.contains(signature)) return false;

        final var injectedHook = new InsnList();
        final var endLabel = new LabelNode();
        injectedHook.add(new VarInsnNode(ALOAD, 0));
        injectedHook.add(new FieldInsnNode(GETFIELD, cn.name, FIELD_NAME, BOOL_DESC));
        injectedHook.add(new JumpInsnNode(IFEQ, endLabel));
        injectedHook.add(new FieldInsnNode(GETSTATIC, ISBRH_CLASS, "INSTANCE", ISBRH_DESC));

        // This call varies based on the node we injected to.
        switch (signature) {
            case GI_WORLD -> {
                injectedHook.add(new VarInsnNode(ALOAD, 1));
                injectedHook.add(new VarInsnNode(ILOAD, 2));
                injectedHook.add(new VarInsnNode(ILOAD, 3));
                injectedHook.add(new VarInsnNode(ILOAD, 4));
                injectedHook
                        .add(new MethodInsnNode(INVOKEVIRTUAL, ISBRH_CLASS, "getParticleIcon", NEW_WORLD_DESC, false));
            }
            case GI_SIDE_META, OBF_SIDE_META, GI_SIDE -> injectedHook
                    .add(new MethodInsnNode(INVOKEVIRTUAL, ISBRH_CLASS, "getMissingIcon", MISSINGNO_DESC, false));
            default -> throw new RuntimeException("Attempted to hook non-icon method!");
        }

        injectedHook.add(new InsnNode(ARETURN));
        injectedHook.add(endLabel);

        mn.instructions.insert(injectedHook);

        return true;
    }

    private Blockness checkForBlockChild(String className, String superName) {
        if (BLOCK_CLASS.equals(className)) return BLOCK;
        if (blockFamily.contains(superName)) {
            blockFamily.add(className);
            return Blockness.BLOCK_SUBCLASS;
        }
        return NOT_A_BLOCK;
    }

    enum Blockness {
        BLOCK,
        BLOCK_SUBCLASS,
        NOT_A_BLOCK
    }
}
