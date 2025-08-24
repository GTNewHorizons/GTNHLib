package com.gtnewhorizon.gtnhlib.client.model;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.Nullable;

public record PileOfQuads(SidedQuadStore sidedQuadStore) implements BakedModel {
    public static final PileOfQuads BLANK = new PileOfQuads(new HashMap<>());

    public PileOfQuads(Map<ForgeDirection, ArrayList<QuadView>> sidedQuadStore) {
        this(new SidedQuadStore(sidedQuadStore));
    }

    @Override
    public List<QuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir, Random random, int color, @Nullable Supplier<QuadView> quadPool) {
        return sidedQuadStore.getQuads(dir);
    }

    public static class SidedQuadStore {
        private static final ObjectImmutableList<QuadView> EMPTY = ObjectImmutableList.of();

        private final ObjectImmutableList<QuadView> up;
        private final ObjectImmutableList<QuadView> down;
        private final ObjectImmutableList<QuadView> north;
        private final ObjectImmutableList<QuadView> south;
        private final ObjectImmutableList<QuadView> west;
        private final ObjectImmutableList<QuadView> east;
        private final ObjectImmutableList<QuadView> unknown;

        public SidedQuadStore(Map<ForgeDirection, ArrayList<QuadView>> sidedQuadStore) {
            up = lockList(sidedQuadStore.get(UP));
            down = lockList(sidedQuadStore.get(DOWN));
            north = lockList(sidedQuadStore.get(NORTH));
            south = lockList(sidedQuadStore.get(SOUTH));
            west = lockList(sidedQuadStore.get(WEST));
            east = lockList(sidedQuadStore.get(EAST));
            unknown = lockList(sidedQuadStore.get(UNKNOWN));
        }

        public List<QuadView> getQuads(ForgeDirection dir) {
            return switch (dir) {
                case UP -> up;
                case DOWN -> down;
                case NORTH -> north;
                case SOUTH -> south;
                case WEST -> west;
                case EAST -> east;
                case UNKNOWN -> unknown;
            };
        }

        private ObjectImmutableList<QuadView> lockList(List<QuadView> list) {
            if (list.isEmpty()) return EMPTY;
            return new ObjectImmutableList<>(list);
        }
    }
}
