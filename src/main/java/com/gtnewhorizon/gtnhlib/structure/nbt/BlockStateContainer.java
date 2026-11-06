package com.gtnewhorizon.gtnhlib.structure.nbt;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;

// Adapted from Et Futurum Requiem (LGPL-3.0)
/**
 * Used to store the results of an unflattening an ID and meta, to easily store it in a map after reading a structure
 * NBT's palette.
 */
public class BlockStateContainer {

    private final Object theObject;
    private NBTTagCompound theCompound;
    private final BlockStateType type;

    private int theMeta;
    private ChestGenHooks lootTable;

    public BlockStateContainer(Block block, int meta, ChestGenHooks info) {
        this(block, meta);
        setLootTable(info);
    }

    public BlockStateContainer(Block block, int meta) {
        theObject = block;
        theMeta = meta;
        type = block instanceof ITileEntityProvider ? BlockStateType.BLOCK_ENTITY : BlockStateType.BLOCK;
    }

    public BlockStateContainer(Block block) {
        this(block, 0);
    }

    public BlockStateContainer(Class<? extends Entity> entity, NBTTagCompound compound) {
        theObject = entity;
        theCompound = compound;
        type = BlockStateType.ENTITY;
    }

    public BlockStateContainer(Class<? extends Entity> entity) {
        this(entity, new NBTTagCompound());
    }

    public Block getBlock() {
        if (type == BlockStateType.ENTITY) {
            throw new IllegalArgumentException("Tried to get block instance from ENTITY in a BlockState object!");
        }
        return (Block) theObject;
    }

    public int getMeta() {
        if (type == BlockStateType.ENTITY) {
            throw new IllegalArgumentException("Tried to get metadata from ENTITY in a BlockState object!");
        }
        return theMeta;
    }

    public Entity createNewEntity(World world) {
        if (type != BlockStateType.ENTITY) {
            throw new IllegalArgumentException("Tried to get entity instance from a block in a BlockState object!");
        }
        return EntityList.createEntityByName(EntityList.classToStringMapping.get(theObject), world);
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
        if (type != BlockStateType.BLOCK_ENTITY
                && (type != BlockStateType.ENTITY || !theObject.getClass().isInstance(IInventory.class))) {
            throw new IllegalArgumentException("Tried to get a loot table from a non-entity block!");
        }
        return lootTable;
    }

    public void setLootTable(ChestGenHooks info) {
        // Dead warning; if it's not BLOCK_ENTITY it can be ENTITY or BLOCK, so IntelliJ's warning is wrong here.
        if (type != BlockStateType.BLOCK_ENTITY
                && (type != BlockStateType.ENTITY || !theObject.getClass().isInstance(IInventory.class))) {
            throw new IllegalArgumentException("Tried to set a loot table from a non-entity block!");
        }
        lootTable = info;
    }

    public enum BlockStateType {
        BLOCK,
        BLOCK_ENTITY,
        ENTITY
    }
}
