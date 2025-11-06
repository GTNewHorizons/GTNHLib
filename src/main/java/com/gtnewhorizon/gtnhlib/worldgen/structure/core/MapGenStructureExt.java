package com.gtnewhorizon.gtnhlib.worldgen.structure.core;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureStart;

public interface MapGenStructureExt {

    StructureStart loadStructureStart(NBTTagCompound tag, World world);


    default void loadDataIfNeeded(World world) { }

    default void addStructureToChunk(int chunkX, int chunkZ, StructureStart start) { }

    default MapGenStructureData getStructureData() {
        return null;
    }
}
