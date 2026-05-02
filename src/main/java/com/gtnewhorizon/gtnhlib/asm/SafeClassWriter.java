package com.gtnewhorizon.gtnhlib.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

/**
 * Copied from ClassWriter which resolves common superclasses using Mixin's metadata instead of calling Class.forName
 */
public final class SafeClassWriter extends ClassWriter {

    public SafeClassWriter(int flags) {
        super(flags);
    }

    public SafeClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }

    /*
     * (non-Javadoc)
     * @see org.objectweb.asm.ClassWriter#getCommonSuperClass(java.lang.String, java.lang.String)
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        return ClassInfo.getCommonSuperClass(type1, type2).getName();
    }
}
