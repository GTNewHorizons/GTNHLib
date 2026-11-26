package com.gtnewhorizon.gtnhlib.structure.nbt.gen;

import java.util.List;

import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureIO;

import com.google.common.collect.Lists;

// Adapted from Et Futurum Requiem (LGPL-3.0)
public abstract class MapGenNBTStructure extends MapGenStructure {

    protected final List<StructureNBTComponent> structurePieces = Lists.newArrayList();

    public MapGenNBTStructure() {
        registerStructurePieces();
        for (StructureNBTComponent struct : structurePieces) {
            MapGenStructureIO.func_143031_a/* registerStructureComponent */(struct.getClass(), struct.pieceName);
        }
    }

    /**
     * Add stuff to the structurePieces list here
     */
    protected abstract void registerStructurePieces();
}
