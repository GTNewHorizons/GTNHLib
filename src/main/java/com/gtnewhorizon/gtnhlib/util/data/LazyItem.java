package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * A lazy-loaded item. Useful for making a static final reference to a item. Can also be used to simplify references to
 * static final items in optional methods.
 * 
 * <pre>
 * {@code
 * private static final LazyItem SOME_OPTIONAL_ITEM = new LazyItem(Mods.SomeMod, "foo");
 * 
 * {@literal @}Optional.Method(Names.SOME_MOD)
 * public static void doSomething(ItemStack stack) {
 *   if (SOME_OPTIONAL_BLOCK.matches(stack)) {
 *      ...
 *   }
 * }
 * </pre>
 */
public class LazyItem extends Lazy<ImmutableItemMeta> implements ImmutableItemMeta {

    public final IMod mod;
    public final String itemName;

    public LazyItem(IMod mod, String itemName, int meta) {
        super(() -> {
            if (!mod.isModLoaded()) return null;

            Item item = GameRegistry.findItem(mod.getID(), itemName);

            Objects.requireNonNull(item, "could not find item: " + mod.getID() + ":" + itemName);

            return new ItemMeta(item, meta);
        });

        this.mod = mod;
        this.itemName = itemName;
    }

    public LazyItem(IMod mod, String itemName) {
        this(mod, itemName, 0);
    }

    /** Checks if the parent mod is loaded. */
    public boolean isLoaded() {
        return mod.isModLoaded();
    }

    @Override
    @SuppressWarnings("null")
    public Item getItem() {
        return get().getItem();
    }

    @Override
    public int getItemMeta() {
        return get().getItemMeta();
    }

    @Override
    public boolean matches(Item other, int metaOther) {
        if (!isLoaded()) return false;

        ImmutableItemMeta bm = get();

        if (bm == null) return false;

        return bm.getItem() == other
                && (bm.getItemMeta() == metaOther || bm.getItemMeta() == OreDictionary.WILDCARD_VALUE
                        || metaOther == OreDictionary.WILDCARD_VALUE);
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack == null) return false;

        return matches(stack.getItem(), Items.feather.getDamage(stack));
    }
}
