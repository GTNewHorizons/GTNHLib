package com.gtnewhorizon.gtnhlib.worldgen.structure.core;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureComponent;

public interface StructureStartExt {

    StructureComponent loadStructureComponent(NBTTagCompound tag, World world);
}
