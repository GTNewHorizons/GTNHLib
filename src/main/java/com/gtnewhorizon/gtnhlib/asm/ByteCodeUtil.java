package com.gtnewhorizon.gtnhlib.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public final class ByteCodeUtil implements Opcodes {

    private ByteCodeUtil() {}

    /**
     * Adds a getter method for the specified field in the ClassNode
     */
    public static void addGetterMethod(ClassNode classNode, String methodName, String owner, String fieldName,
            Type fieldType, String methodSignature) {
        final String fieldDesc = fieldType.getDescriptor();
        final MethodVisitor mv = classNode.visitMethod(ACC_PUBLIC, methodName, "()" + fieldDesc, methodSignature, null);
        mv.visitCode();
        final Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, fieldName, fieldDesc);
        mv.visitInsn(fieldType.getOpcode(IRETURN));
        final Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + owner + ";", null, l0, l1, 0);
        mv.visitMaxs(fieldType.getSize(), 1);
        mv.visitEnd();
    }

    /**
     * Adds a setter method for the specified field in the ClassNode
     */
    public static void addSetterMethod(ClassNode classNode, String methodName, String owner, String fieldName,
            Type fieldType, String methodSignature) {
        final String fieldDesc = fieldType.getDescriptor();
        final MethodVisitor mv = classNode
                .visitMethod(ACC_PUBLIC, methodName, "(" + fieldDesc + ")V", methodSignature, null);
        mv.visitCode();
        final Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(fieldType.getOpcode(ILOAD), 1);
        mv.visitFieldInsn(PUTFIELD, owner, fieldName, fieldDesc);
        final Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(RETURN);
        final Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + owner + ";", null, l0, l2, 0);
        mv.visitLocalVariable(fieldName + "In", fieldDesc, null, l0, l2, 1);
        mv.visitMaxs(1 + fieldType.getSize(), 1 + fieldType.getSize());
        mv.visitEnd();
    }
}
