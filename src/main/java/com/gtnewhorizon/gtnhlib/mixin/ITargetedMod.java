package com.gtnewhorizon.gtnhlib.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.lib.tree.ClassNode;

public interface ITargetedMod {

    /**
     * The "modid" of the targeted mod, found in the @Mod(modid=) annotation.
     */
    default String modId() {
        return null;
    }

    /**
     * Fully qualified name of the class that implements the IFMLLoadingPlugin interface in this targeted mod. For
     * example : "com.gtnewhorizons.angelica.loading.AngelicaTweaker"
     */
    default String coreModClassName() {
        return null;
    }

    /**
     * Fully qualified name of a class of your choice contained in the targeted mod. Typically, its main mod class. For
     * example : "com.gtnewhorizons.angelica.AngelicaMod"
     */
    default String anyClassName() {
        return null;
    }

    /**
     * A conditional check that will test the raw (un-transformed) bytecode of the class specified by
     * {@link ITargetedMod#anyClassName()}. Requires you to implement {@link ITargetedMod#anyClassName()}.
     */
    default Predicate<ClassNode> classNodeTest() {
        return null;
    }

    /**
     * A conditional check that will test the name of the jar files. !!! This mod identification method should only be
     * used as a last resort if you cannot identify your targeted mod with the other methods !!!
     */
    default Predicate<String> jarNameTest() {
        return null;
    }

}
