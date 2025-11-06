package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.worldgen.structure.core.MapGenStructureExt;

@Mixin(MapGenStructure.class)
public abstract class MixinMapGenStructure implements MapGenStructureExt {

    @Shadow
    protected abstract void func_143027_a(World p_143027_1_);

    @Shadow
    protected abstract void func_143026_a(int p_143026_1_, int p_143026_2_, StructureStart p_143026_3_);

    @Shadow
    private MapGenStructureData field_143029_e;

    @Redirect(method = "func_143027_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/structure/MapGenStructureIO;func_143035_a(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/World;)Lnet/minecraft/world/gen/structure/StructureStart;"))
    private StructureStart interceptLoadStart(NBTTagCompound tag, World world) {
        return loadStructureStart(tag, world);
    }

    @Override
    public StructureStart loadStructureStart(NBTTagCompound tag, World world) {
        return MapGenStructureIO.func_143035_a(tag, world);
    }

    @Override
    public void loadDataIfNeeded(World world) {
        this.func_143027_a(world);
    }

    @Override
    public void addStructureToChunk(int chunkX, int chunkZ, StructureStart start) {
        this.func_143026_a(chunkX, chunkZ, start);
    }

    @Override
    public MapGenStructureData getStructureData() {
        return this.field_143029_e;
    }
}
