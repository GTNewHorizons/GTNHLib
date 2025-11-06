package com.gtnewhorizon.gtnhlib.worldgen.structure.test;

import java.io.IOException;

import net.minecraft.world.biome.BiomeGenBase;

import com.gtnewhorizon.gtnhlib.worldgen.structure.core.StructurePiece;
import com.gtnewhorizon.gtnhlib.worldgen.structure.core.StructurePieceGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

public class TestStructurePieceGenerator extends StructurePieceGenerator {

    public TestStructurePieceGenerator() throws IOException {
        super("Test");

        registerStart("tower", StructurePiece.load("structures/dungeon/stone-brick/tower.json"));
        registerPiece("stairs", StructurePiece.load("structures/dungeon/stone-brick/stairs1.json"));
        registerPiece("stairs-bottom", StructurePiece.load("structures/dungeon/stone-brick/stairs1-bottom.json"));

        registerPiece("tunnel-z", StructurePiece.load("structures/dungeon/stone-brick/tunnel-z.json"));

        registerPiece("cap-north", StructurePiece.load("structures/dungeon/stone-brick/cap-north.json"));
        registerPiece("cap-south", StructurePiece.load("structures/dungeon/stone-brick/cap-south.json"));

        this.setRarity(10);
        GameRegistry.registerWorldGenerator(this, 50);
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        if (!super.canSpawnStructureAtCoords(chunkX, chunkZ)) return false;

        BiomeGenBase[] biomes = this.worldObj.getWorldChunkManager().getBiomeGenAt(null, (chunkX << 4) + 8, (chunkZ << 4) + 8, 1, 1, true);

        return biomes[0].getTempCategory() == BiomeGenBase.TempCategory.MEDIUM;
    }
}
