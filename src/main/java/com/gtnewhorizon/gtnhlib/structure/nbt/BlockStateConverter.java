package com.gtnewhorizon.gtnhlib.structure.nbt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

import cpw.mods.fml.common.registry.GameRegistry;

// Adapted from Et Futurum Requiem (LGPL-3.0)
public class BlockStateConverter {

    public static final BlockStateConverter DEFAULT_INSTANCE = new BlockStateConverter();

    private static final ForgeDirection[] DOOR_FACINGS = { ForgeDirection.WEST, ForgeDirection.NORTH,
            ForgeDirection.EAST, ForgeDirection.SOUTH };

    public Map<Integer, BlockStateContainer> processPalette(StructureOrientation orientation,
            Set<Pair<Integer, NBTTagCompound>> paletteNBT) {
        Map<Integer, BlockStateContainer> map = new HashMap<>();
        for (Pair<Integer, NBTTagCompound> pair : paletteNBT) {
            String namespace = NBTStructure.getBlockNamespaceFromNBT(pair.getRight());
            BlockStateContainer propertyState = createPropertyBlockStateContainer(
                    namespace,
                    pair.getRight(),
                    orientation);
            if (propertyState != null) {
                map.put(pair.getLeft(), propertyState);
                continue;
            }

            Map<String, String> properties = transformProperties(
                    namespace,
                    NBTStructure.getProperties(pair.getRight()),
                    orientation);
            Block block = getBlockFromNamespace(namespace, properties);
            if (block != null) {
                map.put(pair.getLeft(), createBlockStateContainer(namespace, block, properties));
            }
        }
        return map;
    }

    /**
     * Rotates and mirrors modern block states: {@code facing}, {@code axis}, {@code rotation}, the {@code north},
     * {@code east}, {@code south} and {@code west} connections, rail {@code shape} and door {@code hinge}.
     */
    protected Map<String, String> transformProperties(String blockName, Map<String, String> blockStates,
            StructureOrientation orientation) {
        Map<String, String> transformed = new HashMap<>();
        for (Map.Entry<String, String> entry : blockStates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            ForgeDirection connection = parseHorizontal(key);
            if (connection != null) {
                transformed.put(getName(orientation.apply(connection)), value);
                continue;
            }
            transformed.put(key, switch (key) {
                case "facing" -> transformDirectionName(value, orientation);
                case "axis" -> orientation.swapsAxes() ? swap(value, "x", "z") : value;
                case "rotation" -> Integer.toString(orientation.transformRotation16(Integer.parseInt(value)));
                case "hinge" -> orientation.isMirrored() ? swap(value, "left", "right") : value;
                case "shape" -> blockName.endsWith("rail") ? transformRailShape(value, orientation) : value;
                default -> value;
            });
        }
        return transformed;
    }

    private static String transformRailShape(String shape, StructureOrientation orientation) {
        if (shape.startsWith("ascending_")) {
            return "ascending_" + transformDirectionName(shape.substring("ascending_".length()), orientation);
        }
        int separator = shape.indexOf('_');
        ForgeDirection first = separator < 0 ? null : parseHorizontal(shape.substring(0, separator));
        ForgeDirection second = separator < 0 ? null : parseHorizontal(shape.substring(separator + 1));
        if (first == null || second == null) {
            return shape;
        }
        first = orientation.apply(first);
        second = orientation.apply(second);
        if (first.offsetZ == 0 && second.offsetZ == 0) {
            return "east_west";
        }
        if (first.offsetX == 0 && second.offsetX == 0) {
            return "north_south";
        }
        return first.offsetZ != 0 ? getName(first) + "_" + getName(second) : getName(second) + "_" + getName(first);
    }

    private static String transformDirectionName(String name, StructureOrientation orientation) {
        ForgeDirection dir = parseHorizontal(name);
        return dir == null ? name : getName(orientation.apply(dir));
    }

    @Nullable
    private static ForgeDirection parseHorizontal(String name) {
        return switch (name) {
            case "north" -> ForgeDirection.NORTH;
            case "south" -> ForgeDirection.SOUTH;
            case "west" -> ForgeDirection.WEST;
            case "east" -> ForgeDirection.EAST;
            default -> null;
        };
    }

    private static String getName(ForgeDirection dir) {
        return dir.name().toLowerCase(Locale.ROOT);
    }

    private static String swap(String value, String a, String b) {
        return value.equals(a) ? b : value.equals(b) ? a : value;
    }

    /**
     * Entries saved by {@link StructureSaver} (with {@code Meta}) and modded blocks with blockstate properties, null
     * falls back to the vanilla mappings.
     */
    @Nullable
    protected BlockStateContainer createPropertyBlockStateContainer(String blockName, NBTTagCompound paletteEntry,
            StructureOrientation orientation) {
        boolean savedMeta = paletteEntry.hasKey("Meta", 99);
        int separator = blockName.indexOf(':');
        if (separator < 0 || (!savedMeta && blockName.startsWith("minecraft:"))) {
            return null;
        }
        Block block = GameRegistry.findBlock(blockName.substring(0, separator), blockName.substring(separator + 1));
        if (block == null) {
            return null;
        }
        int baseMeta = savedMeta ? paletteEntry.getInteger("Meta") : 0;
        if (savedMeta && block instanceof BlockDoor) {
            return new BlockStateContainer(block, transformDoorMeta(baseMeta, orientation));
        }
        Map<String, String> blockStates = NBTStructure.getProperties(paletteEntry);
        if (!savedMeta && block instanceof BlockSlab) {
            blockStates = toSlabTop(blockStates);
        }

        Map<String, BlockProperty<?>> validProperties = new HashMap<>();
        BlockPropertyRegistry
                .getPossibleProperties(block, baseMeta, property -> validProperties.put(property.getName(), property));
        BlockState state = BlockPropertyRegistry.getBlockState(block, baseMeta);
        boolean anyApplied = false;
        for (Map.Entry<String, String> entry : blockStates.entrySet()) {
            BlockProperty<?> property = validProperties.get(entry.getKey());
            if (property != null && (!savedMeta || property instanceof MetaBlockProperty<?>)) {
                setParsedValue(state, property, entry.getValue());
                anyApplied = true;
            }
        }
        if (!savedMeta && !anyApplied) {
            return null;
        }
        state.transform(orientation);
        return new BlockStateContainer(block, state.getBlockMeta(baseMeta));
    }

    /**
     * Door halves store different data: the lower half has the facing in its two low bits, the upper half has the hinge
     * side in its lowest bit, which flips in a mirror image.
     */
    protected static int transformDoorMeta(int meta, StructureOrientation orientation) {
        if ((meta & 8) != 0) {
            return orientation.isMirrored() ? meta ^ 1 : meta;
        }
        ForgeDirection facing = orientation.apply(DOOR_FACINGS[meta & 3]);
        for (int i = 0; i < DOOR_FACINGS.length; i++) {
            if (DOOR_FACINGS[i] == facing) {
                return (meta & ~3) | i;
            }
        }
        return meta;
    }

    /** Modern slabs store their half as {@code type}, which 1.7.10 slabs only read from the world. */
    private static Map<String, String> toSlabTop(Map<String, String> blockStates) {
        String type = blockStates.get("type");
        if (!"top".equals(type) && !"bottom".equals(type)) {
            return blockStates;
        }
        Map<String, String> result = new HashMap<>(blockStates);
        result.remove("type");
        result.put("top", Boolean.toString("top".equals(type)));
        return result;
    }

    private static <T> void setParsedValue(BlockState state, BlockProperty<T> property, String value) {
        try {
            state.setPropertyValue(property, property.parse(value));
        } catch (InvalidPropertyTextException e) {
            GTNHLib.LOG.warn(
                    "Could not parse structure block property {}={} for {}",
                    property.getName(),
                    value,
                    state.getBlock(),
                    e);
        }
    }

    /**
     * Note that blockName is the ID of the block in the palette mapping, not the object you're mapping it to.
     */
    public BlockStateContainer createBlockStateContainer(String blockName, Block block,
            Map<String, String> blockStates) {
        BlockStateContainer bsc = new BlockStateContainer(
                block,
                getMetaFromStateWithSubtypeAdditions(blockName, blockStates));
        NBTTagCompound nbt = null;
        if (block instanceof ITileEntityProvider) {
            String truncatedName = blockName.substring(blockName.indexOf(":") + 1);
            if (truncatedName.startsWith("potted")) {
                Block pottedBlock = null;
                int pottedMeta = 0;
                switch (truncatedName.replace("potted_", "")) {
                    case "dandelion" -> pottedBlock = Blocks.yellow_flower;
                    case "poppy" -> pottedBlock = Blocks.red_flower;
                    case "blue_orchid" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 1;
                    }
                    case "allium" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 2;
                    }
                    case "azure_bluet" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 3;
                    }
                    case "oxeye_daisy" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 8;
                    }
                    case "red_tulip" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 4;
                    }
                    case "orange_tulip" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 5;
                    }
                    case "white_tulip" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 6;
                    }
                    case "pink_tulip" -> {
                        pottedBlock = Blocks.red_flower;
                        pottedMeta = 7;
                    }
                    case "fern" -> {
                        pottedBlock = Blocks.tallgrass;
                        pottedMeta = 2;
                    }
                    case "cactus" -> pottedBlock = Blocks.cactus;
                    case "dead_bush" -> pottedBlock = Blocks.deadbush;
                    case "red_mushroom" -> pottedBlock = Blocks.red_mushroom;
                    case "brown_mushroom" -> pottedBlock = Blocks.brown_mushroom;
                    case "oak_sapling" -> pottedBlock = Blocks.brown_mushroom;
                    case "spruce_sapling" -> {
                        pottedBlock = Blocks.brown_mushroom;
                        pottedMeta = 1;
                    }
                    case "birch_sapling" -> {
                        pottedBlock = Blocks.brown_mushroom;
                        pottedMeta = 2;
                    }
                    case "jungle_sapling" -> {
                        pottedBlock = Blocks.brown_mushroom;
                        pottedMeta = 3;
                    }
                    case "acacia_sapling" -> {
                        pottedBlock = Blocks.brown_mushroom;
                        pottedMeta = 4;
                    }
                    case "dark_oak_sapling" -> {
                        pottedBlock = Blocks.brown_mushroom;
                        pottedMeta = 5;
                    }
                    default -> {}
                }
                if (pottedBlock != null) {
                    nbt = new NBTTagCompound();
                    nbt.setInteger("Item", Block.getIdFromBlock(pottedBlock));
                    nbt.setInteger("Data", pottedMeta);
                }
            } else if ((truncatedName.endsWith("skull") || truncatedName.endsWith("head"))) {
                nbt = new NBTTagCompound();
                if (!truncatedName.contains("wall")) {
                    if (blockStates.containsKey("rotation")) {
                        nbt.setByte("Rot", (byte) Integer.parseInt(blockStates.get("rotation")));
                    }
                }
                int type = 0;
                switch (truncatedName.replace("_head", "").replace("_skull", "").replace("_wall", "")) {
                    case "wither_skeleton" -> type = 1;
                    case "zombie" -> type = 2;
                    case "player" -> type = 3;
                    case "creeper" -> type = 4;
                    case "dragon" -> type = 5;
                    default -> {}
                }
                nbt.setByte("SkullType", (byte) (type & 255));
            }
        }
        if (nbt != null) {
            bsc.setCompound(nbt);
        }
        return bsc;
    }

    /**
     * This function is for getting the equivalent meta value for a BlockState value when unflattening an ID in
     * structure NBT. For example an up-down log is meta 0 in unflattened versions, x-facing log is 1 and z is 2.
     * <p>
     * Wall and fence states are completely discarded as all of their connections and states are done directly in the
     * renderer.
     * <p>
     * The states have already been rotated and mirrored by {@link #transformProperties}.
     */
    public int getMetaFromState(String blockName, Map<String, String> blockStates) {
        String truncatedName = blockName.substring(blockName.indexOf(":") + 1);
        // For rotatable pillars like logs
        if (blockStates.containsKey("axis")) {
            String axis = blockStates.get("axis");
            if (truncatedName.equals("quartz_pillar")) {
                switch (axis) { // Because these cheeky bastards just have to be different from other pillars for some
                                // reason, so we'll do this here instead of in the meta additiosn part
                    case "y" -> {
                        return 2;
                    }
                    case "x" -> {
                        return 3;
                    }
                    case "z" -> {
                        return 4;
                    }
                }
            } else {
                switch (axis) {
                    case "y" -> {
                        return 0;
                    }
                    case "x" -> {
                        return 4;
                    }
                    case "z" -> {
                        return 8;
                    }
                }
            }
        } else if (truncatedName.endsWith("torch") && Boolean.parseBoolean(blockStates.get("standing"))) {
            return 5;
            // For blocks that can face in different directions
            // Note not every block that uses this state has the same meta values in 1.7.10, there will be a lot
        } else if ((truncatedName.contains("skull") || truncatedName.contains("head"))
                && !truncatedName.contains("wall")) {
                    return 1;
                } else
            if (blockStates.containsKey("facing")) {
                String facing = blockStates.get("facing");
                if (truncatedName.endsWith("wall_torch") || truncatedName.endsWith("stairs")) {
                    int meta = 0;
                    if (truncatedName.endsWith("wall_torch")) {
                        meta = 1;
                    } else if ("top".equals(blockStates.get("half"))) {
                        // For stairs. Shape is ignored because that's handled by the renderer in this version.
                        // Unfortunately some stair shapes like a square of corner stairs are simply impossible in
                        // 1.7.10.
                        meta = 4;
                    }
                    switch (facing) {
                        case "east" -> {
                            return meta;
                        }
                        case "west" -> {
                            return meta + 1;
                        }
                        case "south" -> {
                            return meta + 2;
                        }
                        case "north" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.equals("end_portal_frame")) {
                    int meta = 0;
                    if (Boolean.parseBoolean(blockStates.get("eye"))) {
                        meta = 4;
                    }
                    switch (facing) {
                        case "south" -> {
                            return meta;
                        }
                        case "west" -> {
                            return meta + 1;
                        }
                        case "north" -> {
                            return meta + 2;
                        }
                        case "east" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("anvil")) {
                    switch (facing) {
                        case "north" -> {
                            return 0;
                        }
                        case "east" -> {
                            return 1;
                        }
                        case "south" -> {
                            return 2;
                        }
                        case "west" -> {
                            return 3;
                        }
                    }
                } else if (truncatedName.endsWith("trapdoor")) {
                    int meta = 0;
                    if ("top".equals(blockStates.get("half"))) {
                        meta += 8;
                    }
                    if (Boolean.parseBoolean(blockStates.get("open"))) {
                        meta += 4;
                    }
                    switch (facing) {
                        case "north" -> {
                            return meta;
                        }
                        case "south" -> {
                            return meta + 1;
                        }
                        case "west" -> {
                            return meta + 2;
                        }
                        case "east" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("tripwire_hook")) {
                    int meta = 0;
                    if (Boolean.parseBoolean(blockStates.get("attached"))) {
                        meta += 4;
                    }
                    if (Boolean.parseBoolean(blockStates.get("powered"))) {
                        meta += 8;
                    }
                    switch (facing) {
                        case "south" -> {
                            return meta;
                        }
                        case "west" -> {
                            return meta + 1;
                        }
                        case "north" -> {
                            return meta + 2;
                        }
                        case "east" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("door")) {
                    int meta = 0;
                    if ("upper".equals(blockStates.get("half"))) {
                        return "right".equals(blockStates.get("hinge")) ? 9 : 8;
                    } else if (Boolean.parseBoolean(blockStates.get("open"))) {
                        meta += 4;
                    }
                    switch (facing) {
                        case "east" -> {
                            return meta;
                        }
                        case "south" -> {
                            return meta + 1;
                        }
                        case "west" -> {
                            return meta + 2;
                        }
                        case "north" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("fence_gate")) {
                    int meta = 0;
                    if (Boolean.parseBoolean(blockStates.get("open"))) {
                        meta += 4;
                    }
                    switch (facing) {
                        case "south" -> {
                            return meta;
                        }
                        case "west" -> {
                            return meta + 1;
                        }
                        case "north" -> {
                            return meta + 2;
                        }
                        case "east" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("bed")) {
                    int meta = 0;
                    if ("head".equals(blockStates.get("part"))) {
                        meta += 8;
                    }
                    switch (facing) {
                        case "south" -> {
                            return meta;
                        }
                        case "west" -> {
                            return meta + 1;
                        }
                        case "north" -> {
                            return meta + 2;
                        }
                        case "east" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("repeater") || truncatedName.endsWith("comparator")) {
                    int meta = 0;
                    if (blockStates.containsKey("delay")) {
                        meta = 4 * (Integer.parseInt(blockStates.get("delay")) - 1);
                    }
                    if (truncatedName.endsWith("comparator")) {
                        if ("subtract".equals(blockStates.get("mode"))) {
                            meta += 4;
                        }
                        if (Boolean.parseBoolean(blockStates.get("powered"))) {
                            meta += 8;
                        }
                    }
                    // "locked" and "powered" state for repeater is ignored because it's not a meta in 1.7.10
                    switch (facing) {
                        case "south" -> {
                            return meta;
                        }
                        case "west" -> {
                            return meta + 1;
                        }
                        case "north" -> {
                            return meta + 2;
                        }
                        case "east" -> {
                            return meta + 3;
                        }
                    }
                } else if (truncatedName.endsWith("button")) {
                    int meta = 0;
                    return switch (facing) {
                        case "north" -> meta + 4;
                        case "south" -> meta + 3;
                        case "west" -> meta + 2;
                        case "east" -> meta + 1;
                        default -> 0;
                    };
                } else if (truncatedName.endsWith("pressure_plate")) {
                    // Weighted pressure plates have the "power" tag so they get processed at the bottom instead.
                    if (Boolean.parseBoolean(blockStates.get("powered"))) {
                        return 1;
                    }
                } else if (truncatedName.endsWith("vine")) {
                    switch (facing) {
                        case "south" -> {
                            return 1;
                        }
                        case "west" -> {
                            return 2;
                        }
                        case "north" -> {
                            return 4;
                        }
                        case "east" -> {
                            return 8;
                        }
                    }
                } else {
                    switch (facing) {
                        case "down" -> {
                            return 0;
                        }
                        case "up" -> {
                            return 1;
                        }
                        case "north" -> {
                            return 2;
                        }
                        case "south" -> {
                            return 3;
                        }
                        case "west" -> {
                            return 4;
                        }
                        case "east" -> {
                            return 5;
                        }
                    }
                }
            } else if (blockStates.containsKey("stage")
                    && (truncatedName.endsWith("sapling") || truncatedName.equals("mangrove_propagule"))) {
                        if (blockStates.get("stage").equals("1")) {
                            return 8;
                        }
                    } else
                if (blockStates.containsKey("persistent")) {
                    if (blockStates.get("persistent").equals("true")) {
                        return 4;
                    }
                } else if (blockStates.containsKey("type") && truncatedName.contains("slab")) {
                    if (blockStates.get("type").equals("top")) {
                        return 8;
                    }
                } else if (blockStates.containsKey("half") && blockStates.get("half").equals("upper")) {
                    if (truncatedName.equals("sunflower") || truncatedName.equals("lilac")
                            || truncatedName.equals("tall_grass")
                            || truncatedName.equals("large_fern")
                            || truncatedName.equals("rose_bush")
                            || truncatedName.equals("peony")) {
                        return 8;
                    }
                } else if (blockStates.containsKey("shape") && truncatedName.endsWith("rail")) {
                    int meta = 0;
                    if (Boolean.parseBoolean(blockStates.get("powered"))) {
                        meta = 8;
                    }
                    switch (blockStates.get("shape")) {
                        case "north_south" -> {
                            return meta;
                        }
                        case "east_west" -> {
                            return meta + 1;
                        }
                        case "ascending_east" -> {
                            return meta + 2;
                        }
                        case "ascending_west" -> {
                            return meta + 3;
                        }
                        case "ascending_north" -> {
                            return meta + 4;
                        }
                        case "ascending_south" -> {
                            return meta + 5;
                        }
                        case "south_east" -> {
                            return 6;
                        }
                        case "south_west" -> {
                            return 7;
                        }
                        case "north_west" -> {
                            return 8;
                        }
                        case "north_east" -> {
                            return 9;
                        }
                    }
                } else if (truncatedName.equals("tripwire")) {
                    int meta = 0;
                    meta |= Boolean.parseBoolean(blockStates.get("powered")) ? 0x1 : 0;
                    // meta |= Boolean.parseBoolean(blockStates.get("")) ? 0x2 : 0; //Suspended bit, not present in
                    // modern
                    meta |= Boolean.parseBoolean(blockStates.get("attached")) ? 0x4 : 0;
                    meta |= Boolean.parseBoolean(blockStates.get("disarmed")) ? 0x8 : 0;
                    return meta;
                } else if (truncatedName.equals("brewing_stand")) {
                    int meta = 0;
                    meta |= Boolean.parseBoolean(blockStates.get("has_bottle_0")) ? 0x1 : 0;
                    meta |= Boolean.parseBoolean(blockStates.get("has_bottle_1")) ? 0x2 : 0;
                    meta |= Boolean.parseBoolean(blockStates.get("has_bottle_2")) ? 0x4 : 0;
                    return meta;
                } else if (truncatedName.endsWith("mushroom_block")) {
                    return getHugeMushroomMetaFromState(blockName, blockStates);
                } else if (blockStates.containsKey("layers")) {
                    return Integer.parseInt(blockStates.get("layers"));
                } else if (blockStates.containsKey("power")) {
                    return Integer.parseInt(blockStates.get("power"));
                } else if (blockStates.containsKey("level")) {
                    return Integer.parseInt(blockStates.get("level"));
                } else if (blockStates.containsKey("age")) {
                    return Integer.parseInt(blockStates.get("age"));
                } else if (blockStates.containsKey("rotation")) {
                    return Integer.parseInt(blockStates.get("rotation"));
                } else if (blockStates.containsKey("moisture")) {
                    return Integer.parseInt(blockStates.get("moisture")) == 7 ? 1 : 0;
                }
        return 0;
    }

    public int getHugeMushroomMetaFromState(String blockName, Map<String, String> blockStates) {
        boolean up = Boolean.parseBoolean(blockStates.get("up"));
        boolean down = Boolean.parseBoolean(blockStates.get("down"));
        boolean north = Boolean.parseBoolean(blockStates.get("north"));
        boolean east = Boolean.parseBoolean(blockStates.get("east"));
        boolean south = Boolean.parseBoolean(blockStates.get("south"));
        boolean west = Boolean.parseBoolean(blockStates.get("west"));
        if (!up && !down && !north && !east && !south && !west) {
            return 0;
        }
        if (up) {
            if (east) {
                if (north) {
                    return 3;
                }
                if (south) {
                    return 9;
                }
                return 6;
            }
            if (west) {
                if (north) {
                    return 1;
                }
                if (south) {
                    return 7;
                }
                return 4;
            }
            if (north) {
                return 2;
            }
            if (south) {
                return 8;
            }
            return 5;
        }
        return 14;
    }

    /**
     * Calls getMetaFromState and adds the metadata for sub-blocks as well (eg minecraft:black_wool adds to 15) This
     * intentionally contains duplicate switch case statements and more individualized math functions just to make it
     * easier to read what it's doing at a glance.
     *
     * @param blockName
     * @param blockStates
     * @return
     */
    public int getMetaFromStateWithSubtypeAdditions(String blockName, Map<String, String> blockStates) {
        int meta = getMetaFromState(blockName, blockStates);
        if (blockName.startsWith("minecraft:")) {
            String truncatedName = blockName.replace("minecraft:", "");
            switch (truncatedName) {
                case "coarse_dirt" -> meta = 1;
                case "podzol" -> meta = 2;
                case "spruce_planks" -> meta = 1;
                case "birch_planks" -> meta = 2;
                case "jungle_planks" -> meta = 3;
                case "acacia_planks" -> meta = 4;
                case "dark_oak_planks" -> meta = 5;
                case "oak_sapling" -> {
                    meta = 0;
                    if (getMetaFromState(truncatedName, blockStates) > 7) {
                        meta += 8;
                    }
                }
                case "spruce_sapling" -> {
                    meta = 1;
                    if (getMetaFromState(truncatedName, blockStates) > 7) {
                        meta += 8;
                    }
                }
                case "birch_sapling" -> {
                    meta = 2;
                    if (getMetaFromState(truncatedName, blockStates) > 7) {
                        meta += 8;
                    }
                }
                case "jungle_sapling" -> {
                    meta = 3;
                    if (getMetaFromState(truncatedName, blockStates) > 7) {
                        meta += 8;
                    }
                }
                case "acacia_sapling" -> {
                    meta = 4;
                    if (getMetaFromState(truncatedName, blockStates) > 7) {
                        meta += 8;
                    }
                }
                case "dark_oak_sapling" -> {
                    meta = 5;
                    if (getMetaFromState(truncatedName, blockStates) > 7) {
                        meta += 8;
                    }
                }
                case "red_sand" -> meta = 1;
                case "spruce_log" -> meta += 1;
                case "birch_log" -> meta += 2;
                case "jungle_log" -> meta += 3;
                case "dark_oak_log" -> meta += 1;
                case "spruce_leaves" -> meta += 1;
                case "birch_leaves" -> meta += 2;
                case "jungle_leaves" -> meta += 3;
                case "dark_oak_leaves" -> meta += 1;
                case "chiseled_sandstone" -> meta = 1;
                case "cut_sandstone" -> meta = 2;
                case "grass" -> meta = 1;
                case "fern" -> meta = 2;
                case "blue_orchid" -> meta = 1;
                case "allium" -> meta = 2;
                case "azure_bluet" -> meta = 3;
                case "red_tulip" -> meta = 4;
                case "orange_tulip" -> meta = 5;
                case "white_tulip" -> meta = 6;
                case "pink_tulip" -> meta = 7;
                case "oxeye_daisy" -> meta = 8;
                case "potted_blue_orchid" -> meta = 1;
                case "potted_allium" -> meta = 2;
                case "potted_azure_bluet" -> meta = 3;
                case "potted_oxeye_daisy" -> meta = 8;
                case "potted_red_tulip" -> meta = 4;
                case "potted_orange_tulip" -> meta = 5;
                case "potted_white_tulip" -> meta = 6;
                case "potted_pink_tulip" -> meta = 7;
                case "potted_fern" -> meta = 2;
                case "potted_spruce_sapling" -> meta = 1;
                case "potted_birch_sapling" -> meta = 2;
                case "potted_jungle_sapling" -> meta = 3;
                case "potted_acacia_sapling" -> meta = 4;
                case "potted_dark_oak_sapling" -> meta = 5;
                case "spruce_slab" -> meta += 1;
                case "birch_slab" -> meta += 2;
                case "jungle_slab" -> meta += 3;
                case "acacia_slab" -> meta += 4;
                case "dark_oak_slab" -> meta += 5;
                case "sandstone_slab" -> meta += 1;
                case "petrified_oak_slab" -> meta += 2;
                case "cobblestone_slab" -> meta += 3;
                case "brick_slab" -> meta += 4;
                case "stone_brick_slab" -> meta += 5;
                case "nether_brick_slab" -> meta += 6;
                case "quartz_slab" -> meta += 7;
                case "smooth_stone" -> meta = 8;
                case "smooth_sandstone" -> meta = 9;
                case "chipped_anvil" -> {
                    if (meta > 1) {
                        meta += 4;
                    }
                }
                case "damaged_anvil" -> {
                    if (meta > 1) {
                        meta += 8;
                    }
                }
                case "attached_pumpkin_stem", "attached_melon_stem" -> meta = 7;
                case "infested_cobblestone" -> meta = 1;
                case "infested_stone_bricks" -> meta = 2;
                case "infested_mossy_stone_bricks" -> meta = 3;
                case "infested_cracked_stone_bricks" -> meta = 4;
                case "infested_chiseled_stone_bricks" -> meta = 5;
                case "mossy_stone_bricks" -> meta = 1;
                case "cracked_stone_bricks" -> meta = 2;
                case "chiseled_stone_bricks" -> meta = 3;
                case "mushroom_stem" -> {
                    boolean up = Boolean.parseBoolean(blockStates.get("up"));
                    boolean down = Boolean.parseBoolean(blockStates.get("down"));
                    boolean north = Boolean.parseBoolean(blockStates.get("north"));
                    boolean east = Boolean.parseBoolean(blockStates.get("east"));
                    boolean south = Boolean.parseBoolean(blockStates.get("south"));
                    boolean west = Boolean.parseBoolean(blockStates.get("west"));
                    if (north && east && south && west) {
                        if (up && down) {
                            return 15;
                        }
                        return 10;
                    }
                }
                case "mossy_cobblestone_wall" -> meta = 1;
                case "chiseled_quartz_block" -> meta = 1;

                // Quartz pillars are handled in getMetaFromState since their rotation metas are different from every
                // other pillar in the game for some reason
                // May as well do it all there at that point.

                case "lilac" -> {
                    if (blockStates.containsKey("half") && blockStates.get("half").equals("upper")) {
                        meta = 1;
                    }
                }
                case "tall_grass" -> {
                    if (blockStates.containsKey("half") && blockStates.get("half").equals("upper")) {
                        meta = 2;
                    }
                }
                case "large_fern" -> {
                    if (blockStates.containsKey("half") && blockStates.get("half").equals("upper")) {
                        meta = 3;
                    }
                }
                case "rose_bush" -> {
                    if (blockStates.containsKey("half") && blockStates.get("half").equals("upper")) {
                        meta = 4;
                    }
                }
                case "peony" -> {
                    if (blockStates.containsKey("half") && blockStates.get("half").equals("upper")) {
                        meta = 5;
                    }
                }
                case "orange_wool", "orange_carpet", "orange_stained_glass", "orange_stained_glass_pane", "orange_terracotta" -> meta = 1;
                case "magenta_wool", "magenta_carpet", "magenta_stained_glass", "magenta_stained_glass_pane", "magenta_terracotta" -> meta = 2;
                case "light_blue_wool", "light_blue_carpet", "light_blue_stained_glass", "light_blue_stained_glass_pane", "light_blue_terracotta" -> meta = 3;
                case "yellow_wool", "yellow_carpet", "yellow_stained_glass", "yellow_stained_glass_pane", "yellow_terracotta" -> meta = 4;
                case "lime_wool", "lime_carpet", "lime_stained_glass", "lime_stained_glass_pane", "lime_terracotta" -> meta = 5;
                case "pink_wool", "pink_carpet", "pink_stained_glass", "pink_stained_glass_pane", "pink_terracotta" -> meta = 6;
                case "gray_wool", "gray_carpet", "gray_stained_glass", "gray_stained_glass_pane", "gray_terracotta" -> meta = 7;
                case "light_gray_wool", "light_gray_carpet", "light_gray_stained_glass", "light_gray_stained_glass_pane", "light_gray_terracotta" -> meta = 8;
                case "cyan_wool", "cyan_carpet", "cyan_stained_glass", "cyan_stained_glass_pane", "cyan_terracotta" -> meta = 9;
                case "purple_wool", "purple_carpet", "purple_stained_glass", "purple_stained_glass_pane", "purple_terracotta" -> meta = 10;
                case "blue_wool", "blue_carpet", "blue_stained_glass", "blue_stained_glass_pane", "blue_terracotta" -> meta = 11;
                case "brown_wool", "brown_carpet", "brown_stained_glass", "brown_stained_glass_pane", "brown_terracotta" -> meta = 12;
                case "green_wool", "green_carpet", "green_stained_glass", "green_stained_glass_pane", "green_terracotta" -> meta = 13;
                case "red_wool", "red_carpet", "red_stained_glass", "red_stained_glass_pane", "red_terracotta" -> meta = 14;
                case "black_wool", "black_carpet", "black_stained_glass", "black_stained_glass_pane", "black_terracotta" -> meta = 15;
                default -> {}
            }
        }
        return meta;
    }

    public Block getBlockFromNamespace(String blockName, Map<String, String> blockStates) {
        String truncatedName = blockName.substring(blockName.indexOf(":") + 1);
        if (blockName.startsWith("minecraft:")) {
            switch (truncatedName) {
                case "grass_block":
                    return Blocks.grass;

                case "coarse_dirt":
                case "podzol":
                    return Blocks.dirt;

                case "oak_planks":
                case "spruce_planks":
                case "birch_planks":
                case "jungle_planks":
                case "acacia_planks":
                case "dark_oak_planks":
                    return Blocks.planks;

                case "red_sand":
                    return Blocks.sand;

                case "oak_log":
                case "spruce_log":
                case "birch_log":
                case "jungle_log":
                    return Blocks.log;

                case "acacia_log":
                case "dark_oak_log":
                    return Blocks.log2;

                case "oak_leaves":
                case "spruce_leaves":
                case "birch_leaves":
                case "jungle_leaves":
                    return Blocks.leaves;

                case "acacia_leaves":
                case "dark_oak_leaves":
                    return Blocks.leaves2;

                case "chiseled_sandstone":
                case "cut_sandstone":
                    return Blocks.sandstone;

                case "note_block":
                    return Blocks.noteblock;

                case "powered_rail":
                    return Blocks.golden_rail;

                case "cobweb":
                    return Blocks.web;

                case "grass":
                case "fern":
                    return Blocks.tallgrass;

                case "dead_bush":
                    return Blocks.deadbush;

                // piston_extension intentionally left out, the data is probably different and structures don't need
                // this as it is for pistons only
                // If it is needed for some reason, you should probably parse the data yourself because the TE data is
                // different from 1.7.10 and a "bare" piston extension is useless.

                case "dandelion":
                    return Blocks.yellow_flower;

                case "poppy":
                case "blue_orchid":
                case "allium":
                case "azure_bluet":
                case "red_tulip":
                case "orange_tulip":
                case "white_tulip":
                case "pink_tulip":
                case "oxeye_daisy":
                    return Blocks.red_flower;

                case "oak_slab":
                case "birch_slab":
                case "spruce_slab":
                case "jungle_slab":
                case "acacia_slab":
                case "dark_oak_slab":
                    if (blockStates.containsKey("type") && blockStates.get("type").equals("double")) {
                        return Blocks.double_wooden_slab;
                    } else {
                        return Blocks.wooden_slab;
                    }

                case "smooth_stone_slab":
                case "sandstone_slab":
                case "petrified_oak_slab":
                case "cobblestone_slab":
                case "brick_slab":
                case "stone_brick_slab":
                case "nether_brick_slab":
                case "quartz_slab":
                    if (blockStates.containsKey("type") && blockStates.get("type").equals("double")) {
                        return Blocks.double_stone_slab;
                    } else {
                        return Blocks.stone_slab;
                    }

                case "smooth_stone":
                case "smooth_sandstone":
                    return Blocks.double_stone_slab;

                case "bricks":
                    return Blocks.brick_block;

                case "spawner":
                    return Blocks.mob_spawner;

                case "furnace":
                    if (Boolean.parseBoolean(blockStates.get("lit"))) {
                        return Blocks.furnace;
                    }
                    break;

                case "cobblestone_stairs":
                    return Blocks.stone_stairs;

                case "redstone_ore":
                    if (Boolean.parseBoolean(blockStates.get("lit"))) {
                        return Blocks.lit_redstone_ore;
                    }

                case "wall_torch":
                    return Blocks.torch;
                case "redstone_wall_torch":
                case "redstone_torch":
                    if (!Boolean.parseBoolean(blockStates.get("lit"))) {
                        return Blocks.unlit_redstone_torch;
                    }
                    return Blocks.redstone_torch;

                case "snow":
                    return Blocks.snow_layer;

                case "snow_block":
                    return Blocks.snow;

                case "carved_pumpkin":
                    return Blocks.pumpkin;
                case "jack_o_lantern":
                    return Blocks.lit_pumpkin;

                case "infested_stone":
                case "infested_cobblestone":
                case "infested_stone_bricks":
                case "infested_mossy_stone_bricks":
                case "infested_cracked_stone_bricks":
                case "infested_chiseled_stone_bricks":
                    return Blocks.monster_egg;

                case "stone_bricks":
                case "mossy_stone_bricks":
                case "cracked_stone_bricks":
                case "chiseled_stone_bricks":
                    return Blocks.stonebrick;

                case "mushroom_stem":
                    return Blocks.red_mushroom_block;

                case "melon":
                    return Blocks.melon_block;

                case "lily_pad":
                    return Blocks.waterlily;

                case "nether_bricks":
                    return Blocks.nether_brick;

                case "redstone_lamp":
                    if (Boolean.parseBoolean(blockStates.get("lit"))) {
                        return Blocks.lit_redstone_lamp;
                    }

                case "mossy_cobblestone_wall":
                    return Blocks.cobblestone_wall;

                case "daylight_detector":
                    if (Boolean.parseBoolean(blockStates.get("inverted"))) {
                        return Blocks.stone;
                    } else {
                        return Blocks.daylight_detector;
                    }

                case "nether_quartz_ore":
                    return Blocks.quartz_ore;

                case "chiseled_quartz_block":
                case "quartz_pillar":
                    return Blocks.quartz_block;

                case "terracotta":
                    return Blocks.hardened_clay;

                case "sunflower":
                case "lilac":
                case "tall_grass":
                case "large_fern":
                case "rose_bush":
                case "peony":
                    return Blocks.double_plant;

                case "repeater":
                    if (Boolean.parseBoolean(blockStates.get("powered"))) {
                        return Blocks.powered_repeater;
                    } else {
                        return Blocks.unpowered_repeater;
                    }

                case "comparator":
                    if (Boolean.parseBoolean(blockStates.get("powered"))) {
                        return Blocks.powered_comparator;
                    } else {
                        return Blocks.unpowered_comparator;
                    }

                case "skeleton_skull":
                case "skeleton_wall_skull":
                case "wither_skeleton_skull":
                case "wither_skeleton_wall_skull":
                case "zombie_head":
                case "zombie_wall_head":
                case "player_head":
                case "player_wall_head":
                case "creeper_head":
                case "creeper_wall_head":
                case "dragon_head":
                case "dragon_wall_head":
                    return Blocks.skull;

                case "attached_pumpkin_stem":
                    return Blocks.pumpkin_stem;
                case "attached_melon_stem":
                    return Blocks.melon_stem;

                case "potted_poppy":
                case "potted_dandelion":
                case "potted_blue_orchid":
                case "potted_allium":
                case "potted_azure_bluet":
                case "potted_oxeye_daisy":
                case "potted_red_tulip":
                case "potted_orange_tulip":
                case "potted_white_tulip":
                case "potted_pink_tulip":
                case "potted_fern":
                case "potted_cactus":
                case "potted_dead_bush":
                case "potted_red_mushroom":
                case "potted_brown_mushroom":
                case "potted_oak_sapling":
                case "potted_spruce_sapling":
                case "potted_birch_sapling":
                case "potted_jungle_sapling":
                case "potted_acacia_sapling":
                case "potted_dark_oak_sapling":
                    return Blocks.flower_pot;

                case "oak_pressure_plate":
                    return Blocks.wooden_pressure_plate;
                case "oak_fence":
                    return Blocks.fence;
                case "oak_trapdoor":
                    return Blocks.trapdoor;
                case "oak_fence_gate":
                    return Blocks.fence_gate;
                case "oak_button":
                    return Blocks.wooden_button;
                case "oak_door":
                    return Blocks.wooden_door;
                case "oak_sign":
                    return Blocks.standing_sign;
                case "oak_wall_sign":
                    return Blocks.wall_sign;

                case "red_bed":
                    return Blocks.bed;
                case "white_wool":
                case "orange_wool":
                case "magenta_wool":
                case "light_blue_wool":
                case "yellow_wool":
                case "lime_wool":
                case "pink_wool":
                case "gray_wool":
                case "light_gray_wool":
                case "cyan_wool":
                case "purple_wool":
                case "blue_wool":
                case "brown_wool":
                case "green_wool":
                case "red_wool":
                case "black_wool":
                    return Blocks.wool;
                case "white_carpet":
                case "orange_carpet":
                case "magenta_carpet":
                case "light_blue_carpet":
                case "yellow_carpet":
                case "lime_carpet":
                case "pink_carpet":
                case "gray_carpet":
                case "light_gray_carpet":
                case "cyan_carpet":
                case "purple_carpet":
                case "blue_carpet":
                case "brown_carpet":
                case "green_carpet":
                case "red_carpet":
                case "black_carpet":
                    return Blocks.carpet;
                case "white_stained_glass":
                case "orange_stained_glass":
                case "magenta_stained_glass":
                case "light_blue_stained_glass":
                case "yellow_stained_glass":
                case "lime_stained_glass":
                case "pink_stained_glass":
                case "gray_stained_glass":
                case "light_gray_stained_glass":
                case "cyan_stained_glass":
                case "purple_stained_glass":
                case "blue_stained_glass":
                case "brown_stained_glass":
                case "green_stained_glass":
                case "red_stained_glass":
                case "black_stained_glass":
                    return Blocks.stained_glass;
                case "white_stained_glass_pane":
                case "orange_stained_glass_pane":
                case "magenta_stained_glass_pane":
                case "light_blue_stained_glass_pane":
                case "yellow_stained_glass_pane":
                case "lime_stained_glass_pane":
                case "pink_stained_glass_pane":
                case "gray_stained_glass_pane":
                case "light_gray_stained_glass_pane":
                case "cyan_stained_glass_pane":
                case "purple_stained_glass_pane":
                case "blue_stained_glass_pane":
                case "brown_stained_glass_pane":
                case "green_stained_glass_pane":
                case "red_stained_glass_pane":
                case "black_stained_glass_pane":
                    return Blocks.stained_glass_pane;
                case "white_terracotta":
                case "orange_terracotta":
                case "magenta_terracotta":
                case "light_blue_terracotta":
                case "yellow_terracotta":
                case "lime_terracotta":
                case "pink_terracotta":
                case "gray_terracotta":
                case "light_gray_terracotta":
                case "cyan_terracotta":
                case "purple_terracotta":
                case "blue_terracotta":
                case "brown_terracotta":
                case "green_terracotta":
                case "red_terracotta":
                case "black_terracotta":
                    return Blocks.stained_hardened_clay;
                default:
                    break;
            }
        }

        Block block = GameRegistry.findBlock(blockName.substring(0, blockName.indexOf(":")), truncatedName);
        return block == null ? Blocks.stone : block;
    }
}
