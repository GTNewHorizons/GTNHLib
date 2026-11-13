package com.gtnewhorizon.gtnhlib.structure.nbt.gen;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import com.gtnewhorizon.gtnhlib.structure.nbt.NBTStructure;
import com.gtnewhorizon.gtnhlib.structure.nbt.StructureOrientation;

// Adapted from Et Futurum Requiem (LGPL-3.0)
public abstract class StructureNBTComponent extends StructureComponent {

    private final NBTStructure structure;
    public final String pieceName;

    public StructureNBTComponent(NBTStructure structure, String name) {
        this.structure = structure;
        this.pieceName = name;
        this.coordBaseMode = 0;
    }

    protected void generatePiece(World world, Random rand, float integrity, StructureOrientation orientation,
            StructureBoundingBox box) {
        this.generatePiece(world, rand, integrity, 0, orientation, box);
    }

    protected void generatePiece(World world, Random rand, float integrity, int palette,
            StructureOrientation orientation, StructureBoundingBox box) {
        if (coordBaseMode != 0) {
            throw new UnsupportedOperationException(
                    "coordBaseMode (rotation) should only be 0; pass the desired rotation into the NBTStructure instead.");
        }
        int sizeX = structure.getSize(orientation).getX();
        int sizeY = structure.getSize(orientation).getY();
        int sizeZ = structure.getSize(orientation).getZ();
        int x1 = this.getXWithOffset(sizeX, sizeZ);
        int y1 = this.getYWithOffset(sizeY);
        int z1 = this.getZWithOffset(sizeX, sizeZ);
        structure.placeStructure(world, rand, x1, y1, z1, orientation, integrity, palette, box);
    }
}
