package com.gtnewhorizon.gtnhlib.debugworld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.IFluidBlock;

import com.google.common.collect.ImmutableSet;
import com.gtnewhorizon.gtnhlib.GTNHLib;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Decides which block (and metadata) goes where in the debug world, the same way the modern vanilla debug world does.
 * <p>
 * Every block variant is put into one long list (sorted by registry name, so blocks from the same mod end up next to
 * each other). That list is then laid out row by row in a square grid, with one block of air between neighbours so each
 * block can be looked at from all sides.
 * <p>
 * Most variants are fully described by block + metadata. Some blocks (like GregTech machines) store what they really
 * are in their tile entity, set up by their item when placed. Those variants keep their item stack, and
 * {@link DebugChunkProvider} places them through that item instead.
 */
public final class DebugWorldLayout {

    public static final int GRID_Y = 70;

    /** Distance between two neighbouring blocks in the grid, leaves one block of air in between. */
    private static final int SPACING = 2;

    /**
     * Blocks that are known to break or get in the way when placed on their own. Use modid:name to skip a whole block,
     * or modid:name@damage to skip a single item variant (e.g. one GregTech machine).
     */
    private static final Set<String> SKIPPED = ImmutableSet.of(
            "ae2fc:walrus",
            "gadomancy:BlockAdditionalEldritchPortal",
            "gregtech:gt.blackholerenderer",
            "gregtech:gt.nanoforgerenderer",
            "gregtech:gt.wormholerenderer",
            "tectech:Eye of Harmony Renderer",
            "tectech:ForgeOfGodsRenderBlock",
            "Thaumcraft:blockStoneDevice@9",
            "Thaumcraft:blockStoneDevice@10",
            "Thaumcraft:blockStoneDevice@11");

    private final Block[] blocks;
    private final int[] metas;
    private final ItemStack[] placementItems;
    private final int gridWidth;

    private DebugWorldLayout(List<Variant> variants) {
        this.blocks = new Block[variants.size()];
        this.metas = new int[variants.size()];
        this.placementItems = new ItemStack[variants.size()];
        for (int i = 0; i < variants.size(); i++) {
            blocks[i] = variants.get(i).block;
            metas[i] = variants.get(i).meta;
            placementItems[i] = variants.get(i).placementItem;
        }
        this.gridWidth = Math.max(1, (int) Math.ceil(Math.sqrt(blocks.length)));
    }

    public static DebugWorldLayout create() {
        boolean canQueryVariants = FMLCommonHandler.instance().getSide().isClient();

        List<Block> sortedBlocks = new ArrayList<>();
        for (Object entry : Block.blockRegistry) {
            Block block = (Block) entry;
            if (block == Blocks.air || SKIPPED.contains(Block.blockRegistry.getNameForObject(block))) continue;
            // Blocks the crosshair can't target (and WAILA can't show) are render helpers of multiblocks and similar
            if (!block.isCollidable()) continue;
            // Vanilla (BlockLiquid) and Forge (IFluidBlock) fluids, they would start flowing as soon as anything nearby
            // changes
            if (block instanceof BlockLiquid || block instanceof IFluidBlock) continue;
            sortedBlocks.add(block);
        }
        sortedBlocks.sort(Comparator.comparing(block -> Block.blockRegistry.getNameForObject(block)));

        List<Variant> variants = new ArrayList<>();
        int placedBlockCount = 0;
        int itemPlacedCount = 0;
        // First grid index and variant count of every block placed through its item, logged below to help find them
        Map<String, int[]> itemPlacedBlocks = new LinkedHashMap<>();
        for (Block block : sortedBlocks) {
            List<Variant> blockVariants = canQueryVariants ? findVariants(block)
                    : Arrays.asList(new Variant(block, 0, null));
            if (blockVariants.isEmpty()) continue;

            placedBlockCount++;
            if (blockVariants.get(0).placementItem != null) {
                itemPlacedCount += blockVariants.size();
                itemPlacedBlocks.put(
                        Block.blockRegistry.getNameForObject(block),
                        new int[] { variants.size(), blockVariants.size() });
            }
            variants.addAll(blockVariants);
        }

        DebugWorldLayout layout = new DebugWorldLayout(variants);
        GTNHLib.LOG.info(
                "Debug world layout contains {} variants of {} blocks ({} placed through their item, {} blocks skipped)",
                variants.size(),
                placedBlockCount,
                itemPlacedCount,
                sortedBlocks.size() - placedBlockCount);
        itemPlacedBlocks.forEach(
                (name, firstIndexAndCount) -> GTNHLib.LOG.info(
                        "Debug world: {} has {} variants placed through their item, starting at x={} z={}",
                        name,
                        firstIndexAndCount[1],
                        layout.getX(firstIndexAndCount[0]),
                        layout.getZ(firstIndexAndCount[0])));
        return layout;
    }

    /**
     * Finds the variants a block actually uses, based on what it shows in the creative menu. Placing every metadata
     * from 0 to 15 blindly would crash the renderer of many modded blocks that only expect a few.
     * <p>
     * Blocks without an item or without a creative tab get no variants at all, so they are left out. Those are
     * technical blocks players never hold (render helpers, portals, multiblock animations), which often crash or
     * misbehave when placed on their own.
     * <p>
     * The creative menu list only exists on the client, so on a dedicated server this is never called.
     */
    private static List<Variant> findVariants(Block block) {
        Item item = Item.getItemFromBlock(block);
        CreativeTabs tab = block.getCreativeTabToDisplayOn();
        if (item == null || tab == null) return new ArrayList<>();

        String blockName = Block.blockRegistry.getNameForObject(block);
        List<ItemStack> stacks = new ArrayList<>();
        try {
            List<ItemStack> subBlocks = new ArrayList<>();
            block.getSubBlocks(item, tab, subBlocks);
            for (ItemStack stack : subBlocks) {
                if (stack == null || stack.getItem() != item) continue;
                if (SKIPPED.contains(blockName + "@" + stack.getItemDamage())) continue;
                stacks.add(stack);
            }
        } catch (Throwable t) {
            GTNHLib.LOG.warn("Could not read variants of {}, it will be left out of the debug world", blockName, t);
            return new ArrayList<>();
        }

        List<Variant> variants = new ArrayList<>();
        if (needsItemPlacement(stacks)) {
            // The block can only be set up properly by its item, see DebugChunkProvider#placeWithItem
            if (!(item instanceof ItemBlock)) return variants;
            for (ItemStack stack : stacks) {
                variants.add(new Variant(block, 0, stack.copy()));
            }
            return variants;
        }

        // Several creative entries can map to the same metadata, only place each one once
        Map<Integer, Variant> variantsByMeta = new TreeMap<>();
        for (ItemStack stack : stacks) {
            int meta = item.getMetadata(stack.getItemDamage());
            if (meta >= 0 && meta < 16) variantsByMeta.putIfAbsent(meta, new Variant(block, meta, null));
        }
        variants.addAll(variantsByMeta.values());
        return variants;
    }

    /**
     * Metadata only goes from 0 to 15, so an item damage above that, or extra item data (NBT), means the item carries
     * information the block alone can't hold. GregTech machines, for example, use the damage as their machine ID.
     */
    private static boolean needsItemPlacement(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.getItemDamage() > 15 || stack.hasTagCompound()) return true;
        }
        return false;
    }

    /**
     * @return the position in the variant list of the block at the given x/z on the grid layer, or -1 if that spot
     *         should stay empty.
     */
    public int getIndexAt(int x, int z) {
        // The grid starts at 1/1 and only uses every other block, like the vanilla debug world
        if (x <= 0 || z <= 0 || x % SPACING == 0 || z % SPACING == 0) return -1;

        int column = x / SPACING;
        int row = z / SPACING;
        if (column >= gridWidth) return -1;

        int index = row * gridWidth + column;
        return index < blocks.length ? index : -1;
    }

    /** The reverse of {@link #getIndexAt}: the world x coordinate of a variant. */
    public int getX(int index) {
        return (index % gridWidth) * SPACING + 1;
    }

    /** The reverse of {@link #getIndexAt}: the world z coordinate of a variant. */
    public int getZ(int index) {
        return (index / gridWidth) * SPACING + 1;
    }

    public Block getBlock(int index) {
        return blocks[index];
    }

    public int getMeta(int index) {
        return metas[index];
    }

    /**
     * @return the item this variant has to be placed with, or null if block + metadata is enough.
     */
    public ItemStack getPlacementItem(int index) {
        return placementItems[index];
    }

    private static final class Variant {

        private final Block block;
        private final int meta;
        private final ItemStack placementItem;

        private Variant(Block block, int meta, ItemStack placementItem) {
            this.block = block;
            this.meta = meta;
            this.placementItem = placementItem;
        }
    }
}
