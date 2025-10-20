package com.gtnewhorizon.gtnhlib.client.model.baked;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.UNASSIGNED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;

@Desugar
public record PileOfQuads(SidedQuadStore sidedQuadStore) implements BakedModel {

    public static final PileOfQuads BLANK = new PileOfQuads(new HashMap<>());

    public PileOfQuads(Map<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore) {
        this(new SidedQuadStore(sidedQuadStore));
    }

    @Override
    public List<ModelQuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta,
            ModelQuadFacing dir, Random random, int color, @Nullable Supplier<ModelQuadViewMutable> quadPool) {
        return sidedQuadStore.getQuads(dir);
    }

    public static class SidedQuadStore {

        private static final ObjectImmutableList<ModelQuadView> EMPTY = ObjectImmutableList.of();

        private final ObjectImmutableList<ModelQuadView> up;
        private final ObjectImmutableList<ModelQuadView> down;
        private final ObjectImmutableList<ModelQuadView> north;
        private final ObjectImmutableList<ModelQuadView> south;
        private final ObjectImmutableList<ModelQuadView> west;
        private final ObjectImmutableList<ModelQuadView> east;
        private final ObjectImmutableList<ModelQuadView> unknown;

        public SidedQuadStore(Map<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore) {
            up = lockList(sidedQuadStore.get(POS_Y));
            down = lockList(sidedQuadStore.get(NEG_Y));
            north = lockList(sidedQuadStore.get(NEG_Z));
            south = lockList(sidedQuadStore.get(POS_Z));
            west = lockList(sidedQuadStore.get(NEG_X));
            east = lockList(sidedQuadStore.get(POS_X));
            unknown = lockList(sidedQuadStore.get(UNASSIGNED));
        }

        public List<ModelQuadView> getQuads(ModelQuadFacing dir) {
            return switch (dir) {
                case POS_Y -> up;
                case NEG_Y -> down;
                case NEG_Z -> north;
                case POS_Z -> south;
                case NEG_X -> west;
                case POS_X -> east;
                case UNASSIGNED -> unknown;
            };
        }

        private ObjectImmutableList<ModelQuadView> lockList(@Nullable List<ModelQuadView> list) {
            if (list == null || list.isEmpty()) return EMPTY;
            return new ObjectImmutableList<>(list);
        }
    }
}
