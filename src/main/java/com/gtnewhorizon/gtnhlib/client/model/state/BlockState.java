package com.gtnewhorizon.gtnhlib.client.model.state;

import java.util.Objects;

import net.minecraft.block.Block;

public final class BlockState {

    private final Block block;
    private final int meta;

    public BlockState(Block block, int meta) {
        this.block = block;
        this.meta = meta;
    }

    public Block block() {
        return block;
    }

    public int meta() {
        return meta;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BlockState) obj;
        return Objects.equals(this.block, that.block) && this.meta == that.meta;
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, meta);
    }
}
