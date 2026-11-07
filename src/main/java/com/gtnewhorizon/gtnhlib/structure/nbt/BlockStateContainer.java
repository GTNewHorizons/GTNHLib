package com.gtnewhorizon.gtnhlib.structure.nbt;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ChestGenHooks;

// Adapted from Et Futurum Requiem (LGPL-3.0)
/**
 * Used to store the results of an unflattening an ID and meta, to easily store it in a map after reading a structure
 * NBT's palette.
 */
public class BlockStateContainer {

    private final Block theBlock;
    private NBTTagCompound theCompound;
    private final BlockStateType type;

    private int theMeta;
    private ChestGenHooks lootTable;

    public BlockStateContainer(Block block, int meta, ChestGenHooks info) {
        this(block, meta);
        setLootTable(info);
    }

    public BlockStateContainer(Block block, int meta) {
        theBlock = block;
        theMeta = meta;
        type = block instanceof ITileEntityProvider ? BlockStateType.BLOCK_ENTITY : BlockStateType.BLOCK;
    }

    public BlockStateContainer(Block block) {
        this(block, 0);
    }

    public BlockStateContainer copy() {
        BlockStateContainer copy = new BlockStateContainer(theBlock, theMeta);
        copy.theCompound = theCompound == null ? null : (NBTTagCompound) theCompound.copy();
        copy.lootTable = lootTable;
        return copy;
    }

    public Block getBlock() {
        return theBlock;
    }

    public int getMeta() {
        return theMeta;
    }

    public BlockStateType getType() {
        return type;
    }

    public NBTTagCompound getCompound() {
        return theCompound;
    }

    public void setCompound(NBTTagCompound blockTags) {
        theCompound = blockTags;
    }

    /**
     * Gets the loot table meant for stuff like chests, dispensers etc Only works on IInventory block entities.
     */
    public ChestGenHooks getLootTable() {
        if (type != BlockStateType.BLOCK_ENTITY) {
            throw new IllegalArgumentException("Tried to get a loot table from a non-entity block!");
        }
        return lootTable;
    }

    public void setLootTable(ChestGenHooks info) {
        if (type != BlockStateType.BLOCK_ENTITY) {
            throw new IllegalArgumentException("Tried to set a loot table from a non-entity block!");
        }
        lootTable = info;
    }

    public enum BlockStateType {
        BLOCK,
        BLOCK_ENTITY
    }
}
