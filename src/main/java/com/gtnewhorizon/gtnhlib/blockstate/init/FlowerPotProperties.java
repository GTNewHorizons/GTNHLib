package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;

import net.minecraft.block.BlockFlowerPot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateFakeWorld;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty;

/**
 * Block state properties for flower pots, MC 1.7.10 keeps the potted plant in the tile entity. The properties are
 * {@code item} with the plant's item id (0 for empty pots) and {@code item_meta} with its damage, IE
 * {@code item=38,item_meta=1}. Registered for the block instead of the tile entity, so they are known without one.
 *
 * <p>
 * They can only be set in a {@link BlockStateFakeWorld}, so they can't create plants out of nothing. In other worlds
 * setting them does nothing.
 */
class FlowerPotProperties {

    static void register() {
        registerProperty(BlockFlowerPot.class, new ItemProperty());
        registerProperty(BlockFlowerPot.class, new ItemMetaProperty());
    }

    private abstract static class PotProperty implements IntegerBlockProperty {

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, WorldMutable -> true;
                default -> false;
            };
        }

        /** the pot's tile entity gives the renderer its plant, empty pots don't need one */
        static @Nullable TileEntityFlowerPot getOrCreateFlowerPot(World world, int x, int y, int z, boolean create) {
            if (world.getTileEntity(x, y, z) instanceof TileEntityFlowerPot flowerPot) return flowerPot;
            if (!create) return null;

            TileEntityFlowerPot flowerPot = new TileEntityFlowerPot();
            world.setTileEntity(x, y, z, flowerPot);
            return flowerPot;
        }
    }

    private static class ItemProperty extends PotProperty {

        @Override
        public String getName() {
            return "item";
        }

        @Override
        public Integer getValue(IBlockAccess world, int x, int y, int z) {
            if (!(world.getTileEntity(x, y, z) instanceof TileEntityFlowerPot flowerPot)) return 0;

            // vanilla only renders potted blocks
            Item item = flowerPot.getFlowerPotItem();
            if (!(item instanceof ItemBlock)) return 0;

            return Item.getIdFromItem(item);
        }

        @Override
        public void setValue(World world, int x, int y, int z, Integer itemId) {
            if (!(world instanceof BlockStateFakeWorld)) return;

            Item item = (itemId != 0) ? Item.getItemById(itemId) : null;
            TileEntityFlowerPot flowerPot = getOrCreateFlowerPot(world, x, y, z, item != null);
            if (flowerPot == null) return;

            flowerPot.func_145964_a(item, flowerPot.getFlowerPotData());
        }
    }

    private static class ItemMetaProperty extends PotProperty {

        @Override
        public String getName() {
            return "item_meta";
        }

        @Override
        public Integer getValue(IBlockAccess world, int x, int y, int z) {
            if (!(world.getTileEntity(x, y, z) instanceof TileEntityFlowerPot flowerPot)) return 0;

            return flowerPot.getFlowerPotData();
        }

        @Override
        public void setValue(World world, int x, int y, int z, Integer itemMeta) {
            if (!(world instanceof BlockStateFakeWorld)) return;

            TileEntityFlowerPot flowerPot = getOrCreateFlowerPot(world, x, y, z, itemMeta != 0);
            if (flowerPot == null) return;

            flowerPot.func_145964_a(flowerPot.getFlowerPotItem(), itemMeta);
        }
    }
}
