package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;

/**
 * A mutable implementation of {@link ImmutableBlockMeta}. If your API should return a mutable pair, return this
 * instead. Must follow the same contracts as the immutable version if this is ever upcast to a
 * {@link ImmutableBlockMeta} in your API. If this type is exposed instead of the immutable interface, assume that the
 * contained values can change.
 */
public class BlockMeta implements ImmutableBlockMeta {

    @Nonnull
    private Block block;
    private int meta;

    public BlockMeta(@Nonnull Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    public BlockMeta(@Nonnull Block block) {
        this(block, 0);
    }

    public BlockMeta(ImmutableBlockMeta bm) {
        this(bm.getBlock(), bm.getBlockMeta());
    }

    @Override
    @Nonnull
    public Block getBlock() {
        return block;
    }

    @Override
    public int getBlockMeta() {
        return meta;
    }

    /**
     * Note: see the header comment in {@link ImmutableBlockMeta} for this method's contract.
     */
    public BlockMeta setBlock(@Nonnull Block block) {
        this.block = Objects.requireNonNull(block);

        return this;
    }

    /**
     * Note: see the header comment in {@link ImmutableBlockMeta} for this method's contract.
     */
    public BlockMeta setBlockMeta(int meta) {
        this.meta = meta;

        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + block.hashCode();
        result = prime * result + meta;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BlockMeta other = (BlockMeta) obj;
        if (!block.equals(other.block)) return false;
        if (meta != other.meta) return false;
        return true;
    }

    @Override
    public String toString() {
        return "BlockMeta [block=" + block + ", meta=" + meta + "]";
    }
}
