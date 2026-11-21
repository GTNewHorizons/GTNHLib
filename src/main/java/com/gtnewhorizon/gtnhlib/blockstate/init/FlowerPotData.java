package com.gtnewhorizon.gtnhlib.blockstate.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Block state data for flower pots, MC 1.7.10 keeps the potted plant in the tile entity. The data is {@code item} with
 * the plant's item id and {@code meta} with its damage, IE {@code item=38,meta=1}. Empty pots have no data.
 */
class FlowerPotData implements BlockStateDataHandler {

    private static final FlowerPotData INSTANCE = new FlowerPotData();

    private static final String ITEM_PREFIX = "item=";
    private static final String META_PREFIX = "meta=";

    static void register() {
        BlockStateDataRegistry.register(BlockFlowerPot.class, INSTANCE);
    }

    @Override
    public Type getType() {
        return Type.LOCAL_ONLY;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String encode(IBlockAccess world, int x, int y, int z, Block block, int meta) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityFlowerPot flowerPot)) return null;

        // vanilla only renders potted blocks
        Item item = flowerPot.getFlowerPotItem();
        if (!(item instanceof ItemBlock)) return null;

        return ITEM_PREFIX + Item.getIdFromItem(item) + "," + META_PREFIX + flowerPot.getFlowerPotData();
    }

    /** the pot's tile entity gives the renderer its plant */
    @Override
    public void decode(Block block, int meta, String data, BlockStateDataWorld world) {
        String[] pairs = data.split(",");
        if (pairs.length != 2 || !pairs[0].startsWith(ITEM_PREFIX) || !pairs[1].startsWith(META_PREFIX)) return;

        int itemId;
        int itemMeta;
        try {
            itemId = Integer.parseInt(pairs[0].substring(ITEM_PREFIX.length()));
            itemMeta = Integer.parseInt(pairs[1].substring(META_PREFIX.length()));
        } catch (NumberFormatException e) {
            return;
        }

        Item item = Item.getItemById(itemId);
        if (item == null) return;

        world.setTileEntity(0, 0, 0, new TileEntityFlowerPot(item, itemMeta));
    }
}
