package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * A lazy-loaded block. Useful for making a static final reference to a block. Can also be used to simplify references
 * to static final blocks in optional methods.
 * 
 * <pre>
 * {@code
 * private static final LazyBlock SOME_OPTIONAL_BLOCK = new LazyBlock(Mods.SomeMod, "foo");
 * 
 * {@literal @}Optional.Method(modid = Names.SOME_MOD)
 * public static void doSomething(World world, int x, int y, int z) {
 *   if (world.getBlock(x, y, z) == SOME_OPTIONAL_BLOCK.getBlock()) {
 *      ...
 *   }
 * }
 * </pre>
 */
public class LazyBlock extends Lazy<ImmutableBlockMeta> implements ImmutableBlockMeta {

    private final IMod mod;

    public LazyBlock(IMod mod, String blockName, int meta) {
        super(() -> {
            if (!mod.isModLoaded()) throw new IllegalStateException(
                    "cannot get() LazyBlock " + mod.getID() + ":" + blockName + " because its mod is not loaded");

            Block block = GameRegistry.findBlock(mod.getID(), blockName);

            Objects.requireNonNull(block, "could not find block: " + mod.getID() + ":" + blockName);

            return new BlockMeta(block, meta);
        });

        this.mod = mod;
    }

    public LazyBlock(IMod mod, String blockName) {
        this(mod, blockName, 0);
    }

    public LazyBlock(IMod mod, BlockSupplier getter, int meta) {
        super(() -> {
            if (!mod.isModLoaded()) return null;

            Block block = getter.get();

            if (block == null) return null;

            return new BlockMeta(block, meta);
        });

        this.mod = mod;
    }

    public LazyBlock(IMod mod, BlockSupplier getter) {
        this(mod, getter, 0);
    }

    /** Checks if the parent mod is loaded. */
    public boolean isPresent() {
        return mod.isModLoaded();
    }

    public IMod getMod() {
        return mod;
    }

    @Override
    @SuppressWarnings("null")
    public Block getBlock() {
        return get().getBlock();
    }

    @Override
    public int getBlockMeta() {
        return get().getBlockMeta();
    }

    @Override
    public boolean matches(Block other, int metaOther) {
        if (!isPresent()) return false;

        ImmutableBlockMeta bm = get();

        return bm == null ? false : bm.matches(other, metaOther);
    }
}
