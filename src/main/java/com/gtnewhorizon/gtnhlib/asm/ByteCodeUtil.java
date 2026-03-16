package com.gtnewhorizon.gtnhlib.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public final class ByteCodeUtil implements Opcodes {

    private ByteCodeUtil() {}

    /**
     * Adds a getter method for the specified field in the classnode
     */
    public static void addGetterMethod(ClassNode classNode, String methodName, String owner, String fieldName,
            String fieldDesc, String methodSignature) {
        final MethodVisitor mv = classNode.visitMethod(ACC_PUBLIC, methodName, "()" + fieldDesc, methodSignature, null);
        mv.visitCode();
        final Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, fieldName, fieldDesc);
        mv.visitInsn(getReturnOpcode(fieldDesc));
        final Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + owner + ";", null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Returns the appropriate RETURN Opcode depending on the descriptor
     */
    public static int getReturnOpcode(String fieldDesc) {
        return switch (fieldDesc) {
            case "D" -> DRETURN;
            case "F" -> FRETURN;
            case "C", "I", "Z" -> IRETURN;
            case "L" -> LRETURN;
            default -> ARETURN;
        };
    }

    /**
     * Adds a setter method for the specified field in the classnode
     */
    public static void addSetterMethod(ClassNode classNode, String methodName, String owner, String fieldName,
            String fieldDesc, String methodSignature) {
        final MethodVisitor mv = classNode
                .visitMethod(ACC_PUBLIC, methodName, "(" + fieldDesc + ")V", methodSignature, null);
        mv.visitCode();
        final Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(getLoadOpcode(fieldDesc), 1);
        mv.visitFieldInsn(PUTFIELD, owner, fieldName, fieldDesc);
        final Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(RETURN);
        final Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + owner + ";", null, l0, l2, 0);
        mv.visitLocalVariable(fieldName + "In", fieldDesc, null, l0, l2, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * Returns the appropriate LOAD Opcode depending on the descriptor
     */
    public static int getLoadOpcode(String fieldDesc) {
        return switch (fieldDesc) {
            case "D" -> DLOAD;
            case "F" -> FLOAD;
            case "C", "I", "Z" -> ILOAD;
            case "J" -> LLOAD;
            default -> ALOAD;
        };
    }
}
