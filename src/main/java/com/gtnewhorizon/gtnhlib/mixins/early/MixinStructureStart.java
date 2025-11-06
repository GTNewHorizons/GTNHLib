package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.worldgen.structure.core.StructureStartExt;

@Mixin(StructureStart.class)
public class MixinStructureStart implements StructureStartExt {

    @Redirect(method = "func_143020_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/structure/MapGenStructureIO;func_143032_b(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/World;)Lnet/minecraft/world/gen/structure/StructureComponent;"))
    private StructureComponent interceptLoadComponent(NBTTagCompound tag, World world) {
        return loadStructureComponent(tag, world);
    }

    @Override
    public StructureComponent loadStructureComponent(NBTTagCompound tag, World world) {
        return MapGenStructureIO.func_143032_b(tag, world);
    }
}
