package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * A lazy-loaded item. Useful for making a static final reference to a item. Can also be used to simplify references to
 * static final items in optional methods.
 *
 * <pre>
 * {@code
 * private static final LazyItem SOME_OPTIONAL_ITEM = new LazyItem(Mods.SomeMod, "foo");
 *
 * {@literal @}Optional.Method(modid = Names.SOME_MOD)
 * public static void doSomething(ItemStack stack) {
 *   if (SOME_OPTIONAL_BLOCK.matches(stack)) {
 *      ...
 *   }
 * }
 * </pre>
 */
public class LazyItem extends Lazy<ImmutableItemMeta> implements ImmutableItemMeta {

    private final IMod mod;

    public LazyItem(IMod mod, String itemName, int meta) {
        super(() -> {
            if (!mod.isModLoaded()) return null;

            Item item = GameRegistry.findItem(mod.getID(), itemName);

            Objects.requireNonNull(item, "could not find item: " + mod.getID() + ":" + itemName);

            return new ItemMeta(item, meta);
        });

        this.mod = mod;
    }

    public LazyItem(IMod mod, String itemName) {
        this(mod, itemName, 0);
    }

    public LazyItem(IMod mod, ItemStackSupplier getter) {
        super(() -> {
            if (!mod.isModLoaded()) return null;

            ItemStack stack = getter.get();

            if (stack == null || stack.getItem() == null) return null;

            return new ItemMeta(stack.getItem(), ItemUtil.getStackMeta(stack));
        });

        this.mod = mod;
    }

    public LazyItem(IMod mod, ItemSupplier getter, int meta) {
        super(() -> {
            if (!mod.isModLoaded()) return null;

            Item item = getter.get();

            if (item == null) return null;

            return new ItemMeta(item, meta);
        });

        this.mod = mod;
    }

    public LazyItem(IMod mod, ItemSupplier getter) {
        this(mod, getter, 0);
    }

    /** Checks if the parent mod is loaded. */
    public boolean isLoaded() {
        return mod.isModLoaded();
    }

    public IMod getMod() {
        return mod;
    }

    @Override
    @Nonnull
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

        return matches(stack.getItem(), ItemUtil.getStackMeta(stack));
    }

    /**
     * Converts this LazyItem to an ItemStack. Returns null if the parent mod isn't loaded, or if the contained
     * ImmutableItemMeta is null.
     */
    @Override
    public ItemStack toStack(int amount) {
        if (!isLoaded()) return null;

        ImmutableItemMeta bm = get();

        if (bm == null) return null;

        return bm.toStack(amount);
    }
}
