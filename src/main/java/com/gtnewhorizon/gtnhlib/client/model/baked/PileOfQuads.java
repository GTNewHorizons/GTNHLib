package com.gtnewhorizon.gtnhlib.client.model.baked;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.UNASSIGNED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.util.IIcon;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;

public final class PileOfQuads implements BakedModel {

    private final SidedQuadStore sidedQuadStore;
    private final Map<Position, Position.ModelDisplay> display;
    private final IIcon particle;

    public PileOfQuads(SidedQuadStore sidedQuadStore, Map<Position, Position.ModelDisplay> display, IIcon particle) {
        this.sidedQuadStore = sidedQuadStore;
        this.display = display;
        this.particle = particle;
    }

    public PileOfQuads(Map<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore,
            Map<Position, Position.ModelDisplay> display, IIcon particle) {
        this(new SidedQuadStore(sidedQuadStore), display, particle);
    }

    @Override
    public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
        return sidedQuadStore.getQuads(context.getQuadFacing());
    }

    @Override
    public Position.ModelDisplay getDisplay(Position pos, BakedModelQuadContext context) {
        return display.getOrDefault(pos, Position.ModelDisplay.DEFAULT);
    }

    @Override
    public IIcon getParticle(BakedModelQuadContext context) {
        return particle;
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
