package com.gtnewhorizon.gtnhlib.structure.nbt;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * A data marker for structure templates, registered when {@code enableStructureMarker} is on. {@link StructureSaver}
 * writes it as a modern structure block in data mode with the marker's text, which
 * {@link NBTStructure#setStructureBlockAction} receives on load. Set the text with {@code /structure marker};
 * right-click shows it.
 */
public class BlockStructureMarker extends BlockContainer {

    public static BlockStructureMarker INSTANCE;

    public BlockStructureMarker() {
        super(Material.iron);
        this.setBlockName("gtnhlib.structure_marker");
        this.setBlockTextureName("command_block");
        this.setBlockUnbreakable();
        this.setResistance(6000000.0F);
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    public static void register() {
        INSTANCE = new BlockStructureMarker();
        GameRegistry.registerBlock(INSTANCE, "structure_marker");
        GameRegistry.registerTileEntity(TileEntityStructureMarker.class, "gtnhlib:structure_marker");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityStructureMarker();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
            float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileEntityStructureMarker marker) {
            player.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.structure.marker.data", marker.getData()));
        }
        return true;
    }
}
