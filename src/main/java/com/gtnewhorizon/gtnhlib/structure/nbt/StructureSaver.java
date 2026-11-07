package com.gtnewhorizon.gtnhlib.structure.nbt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

/**
 * Writes an area of the world in the structure template format {@link NBTStructure} reads. Each palette entry has the
 * block's registry name, its GTNHLib blockstate properties and its exact metadata ({@code Meta}).
 */
public final class StructureSaver {

    public static final String STRUCTURE_BLOCK = "minecraft:structure_block";

    private StructureSaver() {}

    public static NBTTagCompound save(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);

        Map<String, Integer> paletteIndices = new HashMap<>();
        NBTTagList palette = new NBTTagList();
        NBTTagList blocks = new NBTTagList();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    TileEntity tile = world.getTileEntity(x, y, z);
                    boolean marker = tile instanceof TileEntityStructureMarker;
                    NBTTagCompound paletteEntry = marker ? createMarkerPaletteEntry()
                            : createPaletteEntry(world, x, y, z);
                    String key = paletteEntry.toString();
                    Integer index = paletteIndices.get(key);
                    if (index == null) {
                        index = palette.tagCount();
                        paletteIndices.put(key, index);
                        palette.appendTag(paletteEntry);
                    }

                    NBTTagCompound blockEntry = new NBTTagCompound();
                    blockEntry.setTag("pos", intList(x - minX, y - minY, z - minZ));
                    blockEntry.setInteger("state", index);
                    if (marker) {
                        blockEntry.setTag("nbt", createMarkerNbt(((TileEntityStructureMarker) tile).getData()));
                    } else if (tile != null) {
                        NBTTagCompound tileNbt = new NBTTagCompound();
                        tile.writeToNBT(tileNbt);
                        tileNbt.removeTag("x");
                        tileNbt.removeTag("y");
                        tileNbt.removeTag("z");
                        blockEntry.setTag("nbt", tileNbt);
                    }
                    blocks.appendTag(blockEntry);
                }
            }
        }

        NBTTagCompound structure = new NBTTagCompound();
        structure.setTag("size", intList(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1));
        structure.setTag("palette", palette);
        structure.setTag("blocks", blocks);
        structure.setTag("entities", new NBTTagList());
        return structure;
    }

    public static void write(NBTTagCompound structure, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        CompressedStreamTools.writeCompressed(structure, new FileOutputStream(file));
    }

    private static NBTTagCompound createMarkerPaletteEntry() {
        NBTTagCompound properties = new NBTTagCompound();
        properties.setString("mode", "data");
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("Name", STRUCTURE_BLOCK);
        entry.setTag("Properties", properties);
        return entry;
    }

    private static NBTTagCompound createMarkerNbt(String data) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", STRUCTURE_BLOCK);
        nbt.setString("mode", "DATA");
        nbt.setString(TileEntityStructureMarker.TAG_DATA, data);
        return nbt;
    }

    private static NBTTagCompound createPaletteEntry(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("Name", Block.blockRegistry.getNameForObject(block));
        entry.setInteger("Meta", world.getBlockMetadata(x, y, z));

        BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z);
        Map<String, String> properties = state.toMap();
        if (!properties.isEmpty()) {
            NBTTagCompound propertiesTag = new NBTTagCompound();
            properties.forEach(propertiesTag::setString);
            entry.setTag("Properties", propertiesTag);
        }
        return entry;
    }

    private static NBTTagList intList(int a, int b, int c) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagInt(a));
        list.appendTag(new NBTTagInt(b));
        list.appendTag(new NBTTagInt(c));
        return list;
    }
}
