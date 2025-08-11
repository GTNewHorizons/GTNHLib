package com.gtnewhorizon.gtnhlib.core.shared;

import com.gtnewhorizon.gtnhlib.asm.ASMUtil;
import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;

public class GTNHLibClassDump {

    private static final boolean DUMP_CLASS = Boolean.getBoolean("gtnhlib.dumpClass");

    public static void dumpClass(String className, byte[] originalBytes, byte[] transformedBytes, Object transformer) {
        if (DUMP_CLASS) {
            ASMUtil.saveAsRawClassFile(originalBytes, className + "_PRE", transformer);
            ASMUtil.saveAsRawClassFile(transformedBytes, className + "_POST", transformer);
        }
    }

    public static void dumpRFBClass(String className, ClassNodeHandle classNode, Object transformer) {
        if (DUMP_CLASS) {
            final byte[] originalBytes = classNode.getOriginalBytes();
            final byte[] transformedBytes = classNode.computeBytes();
            ASMUtil.saveAsRawClassFile(originalBytes, className + "_PRE", transformer);
            ASMUtil.saveAsRawClassFile(transformedBytes, className + "_POST", transformer);
        }
    }
}
