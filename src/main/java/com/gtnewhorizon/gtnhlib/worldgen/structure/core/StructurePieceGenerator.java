package com.gtnewhorizon.gtnhlib.worldgen.structure.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3i;
import org.joml.primitives.AABBf;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a64;
import com.gtnewhorizon.gtnhlib.util.VoxelAABB;
import com.gtnewhorizon.gtnhlib.util.XSTR;
import com.gtnewhorizon.gtnhlib.util.dynamicaabbtree.AABBTree;
import com.gtnewhorizon.gtnhlib.util.dynamicaabbtree.Boundable;
import com.gtnewhorizon.gtnhlib.util.dynamicaabbtree.Identifiable;
import com.gtnewhorizon.gtnhlib.worldgen.structure.core.StructurePiece.BudgetOperation;
import com.gtnewhorizon.gtnhlib.worldgen.structure.core.StructurePiece.StructureSocket;
import cpw.mods.fml.common.IWorldGenerator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public abstract class StructurePieceGenerator extends MapGenStructure implements MapGenStructureExt, StructureStartExt,
    IWorldGenerator {

    private final BiMap<String, StructurePiece> pieces = HashBiMap.create();

    private final HashSet<String> starts = new HashSet<>();

    private final Multimap<String, StructurePiece> piecesBySockets = MultimapBuilder.hashKeys().arrayListValues().build();

    @Getter
    private final String structureName;

    @Getter
    @Setter
    private int rarity = 100;

    @Getter
    @Setter
    private int minDistance = 16;

    @Getter
    @Setter
    private boolean generatesOnSurface = true;

    protected final XSTR rng = new XSTR(0);

    private final StructureMap structureMap = new StructureMap();

    private final Logger logger;

    protected StructurePieceGenerator(String name) {
        this.structureName = name;
        super.structureMap = this.structureMap;
        this.logger = LogManager.getLogger(this.getClass().getSimpleName() + ":" + this.structureName);
    }

    @Override
    public String func_143025_a() {
        return structureName;
    }

    protected void registerPiece(String name, StructurePiece piece) {
        pieces.put(name, piece);

        if (piece.sockets != null) {
            piece.sockets.forEach(socket -> piecesBySockets.put(socket.category, piece));
        }
    }

    protected void registerStart(String name, StructurePiece piece) {
        registerPiece(name, piece);

        starts.add(name);
    }

    protected void primeRNG(int chunkX, int chunkZ) {
        long hash = getRNGSeed(chunkX, chunkZ);
        rng.setSeed(hash);
    }

    private long getRNGSeed(int chunkX, int chunkZ) {
        long hash = Fnv1a64.initialState();
        hash = Fnv1a64.hashStep(hash, worldObj.getSeed());
        hash = Fnv1a64.hashStep(hash, chunkX);
        hash = Fnv1a64.hashStep(hash, chunkZ);
        return hash;
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        if (Math.abs(chunkX) + Math.abs(chunkZ) < minDistance) return false;

        primeRNG(chunkX, chunkZ);

        return rng.nextInt(rarity) == 0;
    }

    protected Candidate pickPiece(Collection<Candidate> pieces) {
        float sum = 0;

        for (var piece : pieces) {
            sum += piece.piece.weight;
        }

        float cursor = rng.nextFloat() * sum;

        for (var piece : pieces) {
            cursor -= piece.piece.weight;

            if (cursor <= 0) return piece;
        }

        return null;
    }

    protected Pair<String, StructurePiece> pickPiece(Iterator<String> pieceNames) {
        ObjectList<String> pieceList = ObjectIterators.pour(pieceNames);

        float sum = 0;

        for (String pieceName : pieceList) {
            sum += pieces.get(pieceName).weight;
        }

        float cursor = rng.nextFloat() * sum;

        for (String pieceName : pieceList) {
            StructurePiece piece = pieces.get(pieceName);

            cursor -= piece.weight;

            if (cursor <= 0) return Pair.of(pieceName, piece);
        }

        return null;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        primeRNG(chunkX, chunkZ);

        var piece = pickPiece(starts.iterator());

        StructurePieceStart start = createStart(piece.left(), chunkX, chunkZ);

        int nextStartId = this.getStructureData().func_143041_a().getInteger("nextStartId");

        start.id = nextStartId++;

        this.getStructureData().func_143041_a().setInteger("nextStartId", nextStartId);

        return start;
    }

    @SneakyThrows
    protected StructurePieceStart createStart(String name, int chunkX, int chunkZ) {
        primeRNG(chunkX, chunkZ);

        return new StructurePieceStart(worldObj, rng, chunkX, chunkZ, name, pieces.get(name));
    }

    @SneakyThrows
    protected StructurePieceComponent createComponent(String name) {
        return new StructurePieceComponent(0, name, pieces.get(name));
    }

    @Override
    public StructurePieceStart loadStructureStart(NBTTagCompound tag, World world) {
        String name = tag.getString("Piece");

        StructurePieceStart start = new StructurePieceStart(name, pieces.get(name));

        start.func_143020_a(world, tag);

        return start;
    }

    @Override
    public StructurePieceComponent loadStructureComponent(NBTTagCompound tag, World world) {
        String name = tag.getString("Piece");

        StructurePieceComponent component = new StructurePieceComponent(name, pieces.get(name));

        component.func_143009_a(world, tag);

        return component;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (chunkGenerator instanceof ChunkProviderFlat) return;
        if (chunkProvider instanceof ChunkProviderFlat) return;

        this.func_151539_a(chunkProvider, world, chunkX, chunkZ, null);

        primeRNG(chunkX, chunkZ);

        this.generateStructuresInChunk(world, this.rng, chunkX, chunkZ);
    }

    @Override
    public boolean generateStructuresInChunk(World world, Random rng, int chunkX, int chunkZ) {
        if (world.getChunkProvider() instanceof ChunkProviderFlat) return false;

        this.loadDataIfNeeded(world);

        boolean didSomething = false;

        for (StructurePieceStart start : structureMap.getOverlaps(chunkX, chunkZ)) {
            start.generateStructure(world, rng, chunkX, chunkZ);
            didSomething = true;
            this.addStructureToChunk(start.func_143019_e(), start.func_143018_f(), start);
        }

        return didSomething;
    }

    protected int getYLevel(World world, Random rng, StructurePieceStart start, StructurePieceComponent piece) {
        if (this.generatesOnSurface) {
            int x = piece.caabb.origin.x;
            int z = piece.caabb.origin.z;

            BiomeGenBase biome = world.getBiomeGenForCoords(x, z);

            int y = world.getHeightValue(x, z);

            for (; y > 0; y--) {
                Block block = world.getBlock(x, y, z);

                if (block == biome.fillerBlock || block == biome.topBlock) {
                    break;
                }
            }

            return y + 1;
        } else {
            return rng.nextInt(40) + 20;
        }
    }

    public static class StructureMap extends Long2ObjectOpenHashMap<StructureStart> {

        private final AABBTree<StructurePieceStart> tree = new AABBTree<>();

        @Override
        public StructureStart put(long k, StructureStart structureStart) {
            tree.add((StructurePieceStart) structureStart);

            return super.put(k, structureStart);
        }

        public ArrayList<StructurePieceStart> getOverlaps(int chunkX, int chunkZ) {
            AABBf test = new AABBf(chunkX << 4, Float.NEGATIVE_INFINITY, chunkZ << 4, (chunkX << 4) + 15, Float.POSITIVE_INFINITY, (chunkZ << 4) + 15);

            ArrayList<StructurePieceStart> out = new ArrayList<>();

            tree.detectOverlaps(test, out);

            return out;
        }
    }

    @Data
    protected static class Candidate {
        public final StructurePiece piece;
        public final StructureSocket inboundSocket;
        public final Object2IntOpenHashMap<String> budget;
    }

    public class StructurePieceStart extends StructureStart implements StructureStartExt, Boundable, Identifiable {

        protected int id;
        protected final String name;
        @Getter
        protected final StructurePiece piece;
        protected boolean isPositioned;

        protected int originX;
        protected int originZ;

        protected LongArrayList queuedChunks;

        public StructurePieceStart(String name, StructurePiece piece) {
            this.name = name;
            this.piece = piece;
        }

        public StructurePieceStart(World world, Random rng, int chunkX, int chunkZ, String name, StructurePiece piece) {
            super(chunkX, chunkZ);
            this.name = name;
            this.piece = piece;

            logger.info("Starting structure with piece {} at {},{}", name, chunkX, chunkZ);

            PieceTracker tracker = new PieceTracker();

            StructurePieceComponent start = StructurePieceGenerator.this.createComponent(name);

            originX = (chunkX << 4) + rng.nextInt(16);
            originZ = (chunkZ << 4) + rng.nextInt(16);

            start.caabb.moveOrigin(originX, generatesOnSurface ? 0 : getYLevel(world, rng, this, start), originZ);

            this.isPositioned = !generatesOnSurface;

            logger.info("Initial position: {} (isPositioned: {}, generatesOnSurface: {})", start.caabb.origin, isPositioned, generatesOnSurface);

            BlockPos receivingSocketWorldPos = new BlockPos();

            XSTR budgetRNG = new XSTR(rng.nextLong());

            {
                Object2IntOpenHashMap<String> budget = new Object2IntOpenHashMap<>();

                applyBudget(budgetRNG, budget, start.piece.budgetOperations);

                logger.info("Initial budget: {}", budget);

                tracker.add(rng, start, null, budget);
            }

            while (!tracker.pendingSockets.isEmpty()) {
                PendingSocket pendingSocket = tracker.pendingSockets.poll();

                logger.info("Processing socket from piece {}: {}", pendingSocket.piece.name, pendingSocket);
                logger.info("Budget: {}", pendingSocket.budget);

                List<Candidate> validPieces = new ArrayList<>();

                // Structure-local location of the parent component's origin
                receivingSocketWorldPos.set(pendingSocket.piece.caabb.origin);
                // Structure-local location of the socket
                receivingSocketWorldPos.add(pendingSocket.socket.pos);
                // Structure-local location of the block the socket faces
                receivingSocketWorldPos.add(pendingSocket.socket.forward);

                List<String> categories = pendingSocket.socket.connectsTo.isEmpty() ?
                    Collections.singletonList(pendingSocket.socket.category) :
                    pendingSocket.socket.connectsTo;

                logger.info("Checking sockets with the following categories: {}", categories);

                List<StructurePiece> possiblePieces = categories.stream().flatMap(s -> piecesBySockets.get(s).stream()).distinct().collect(Collectors.toList());

                for (StructurePiece candidatePiece : possiblePieces) {
                    logger.info("Checking piece: {}", candidatePiece.path);

                    for (StructureSocket candidateSocket : candidatePiece.getSockets()) {
                        if (candidateSocket.forward != pendingSocket.socket.forward.getOpposite()) continue;
                        if (!categories.contains(candidateSocket.category)) continue;

                        logger.info("Found socket with correct orientation and category: {}", candidateSocket);

                        {
                            BlockPos testOrigin = new BlockPos();
                            // Structure-local location of the block the parent socket faces
                            testOrigin.set(receivingSocketWorldPos);
                            // Structure-local location of the next component's origin
                            testOrigin.sub(candidateSocket.pos);

                            VoxelAABB aabb = candidatePiece.aabb.clone().moveOrigin(testOrigin);

                            logger.info("Checking for overlaps if piece {} had origin {} (aabb: {})", candidatePiece.path, testOrigin, aabb);

                            List<StructurePieceComponent> conflicts = new ArrayList<>();

                            for (StructurePieceComponent component : tracker.byId.values()) {
                                if (component.caabb.contains(aabb)) {
                                    conflicts.add(component);
                                }
                            }

                            if (!conflicts.isEmpty()) {
                                logger.info("Piece overlapped with the following other pieces: {}", conflicts);

                                continue;
                            }
                        }

                        Object2IntOpenHashMap<String> budget = new Object2IntOpenHashMap<>(pendingSocket.budget);

                        logger.info("Checking budget: {}", budget);

                        if (!applyBudget(budgetRNG, budget, candidateSocket.budgetOperations)) {
                            logger.info("Failed inbound socket budget check: {}", candidateSocket.budgetOperations);

                            continue;
                        }

                        if (!applyBudget(budgetRNG, budget, candidatePiece.budgetOperations)) {
                            logger.info("Failed piece budget check: {}", candidatePiece.budgetOperations);

                            continue;
                        }

                        logger.info("Found valid piece: {} (via {}, budget {})", candidatePiece, candidateSocket, budget);

                        validPieces.add(new Candidate(candidatePiece, candidateSocket, budget));
                    }
                }

                if (validPieces.isEmpty()) {
                    logger.info("No valid pieces: ignoring this socket for the remainder of this structure: {}", pendingSocket);
                }

                logger.info("Selecting a piece from {} valid candidates", validPieces.size());

                Candidate next = pickPiece(validPieces);

                if (next == null) {
                    logger.info("Could not select a piece");

                    continue;
                }

                StructurePieceComponent component = createComponent(pieces.inverse().get(next.piece));

                {
                    BlockPos origin = new BlockPos();
                    // Structure-local location of the block the parent socket faces
                    origin.set(receivingSocketWorldPos);
                    // Structure-local location of the next component's origin
                    origin.sub(next.inboundSocket.pos);

                    component.caabb.moveOrigin(origin);
                }

                tracker.add(rng, component, next.inboundSocket, next.budget.clone());

                logger.info("Created component with id {} (parent: {}): {}", component.id, pendingSocket.piece.id, component);
                logger.info("Remaining budget: {}", next.budget);
                logger.info("There are {} sockets remaining that need to be processed (structure contains {} pieces)", tracker.pendingSockets.size(), tracker.tree.size());

                if (tracker.tree.size() > 1000) {
                    logger.info("Bailing: structure has too many pieces: this is likely a bug");
                    throw new IllegalStateException("StructurePieceGenerator created a structure with too many parts: the provided structure pieces likely have an infinite loop");
                }
            }

            this.updateBoundingBox();
        }

        protected boolean applyBudget(Random rng, Object2IntOpenHashMap<String> budget, List<BudgetOperation> operations) {
            if (operations == null || operations.isEmpty()) return true;

            for (BudgetOperation operation : operations) {
                if (!applyBudget(rng, budget, operation)) return false;
            }

            return true;
        }

        protected boolean applyBudget(Random rng, Object2IntOpenHashMap<String> budget, BudgetOperation operation) {
            switch (operation.action) {
                case Consume -> {
                    int stored = budget.getInt(operation.category);

                    if (stored < operation.amount) return false;

                    logger.info("Consuming {} {} budget", operation.amount, operation.category);

                    stored -= operation.amount;

                    if (stored > 0) {
                        budget.put(operation.category, stored);
                    } else {
                        budget.removeInt(operation.category);
                    }

                    return true;
                }
                case Reset -> {
                    logger.info("Resetting {} budget to {}", operation.category, operation.amount);

                    if (operation.amount == 0) {
                        budget.removeInt(operation.category);
                    } else {
                        budget.put(operation.category, operation.amount);
                    }
                }
                case ResetRandom -> {
                    int amount = rng.nextInt(operation.upper - operation.lower) + operation.lower;

                    logger.info("Resetting {} budget to {} (random, {}..{})", operation.category, amount, operation.lower, operation.upper);

                    budget.put(operation.category, amount);
                }
                case RequireExact -> {
                    return budget.getInt(operation.category) == operation.amount;
                }
                case RequireRange -> {
                    int value = budget.getInt(operation.category);

                    return value >= operation.lower && value <= operation.upper;
                }
            }

            return true;
        }

        protected class PieceTracker {
            private int nextId = 0;

            private final Int2ObjectOpenHashMap<StructurePieceComponent> byId = new Int2ObjectOpenHashMap<>();
            private final ArrayDeque<PendingSocket> pendingSockets = new ArrayDeque<>();
            private final AABBTree<StructurePieceComponent> tree = new AABBTree<>();

            public void add(Random rng, StructurePieceComponent component, StructureSocket via, Object2IntOpenHashMap<String> budget) {
                component.id = nextId++;

                byId.put(component.id, component);

                for (StructureSocket socket : component.getSockets()) {
                    if (socket == via) continue;

                    Object2IntOpenHashMap<String> budgetCopy = budget.clone();

                    if (!applyBudget(rng, budgetCopy, socket.budgetOperations)) {
                        logger.info("Skipping socket on piece {} because its budget check failed (input budget: {}): socket: {}", component.name, budget, socket);
                        continue;
                    }

                    pendingSockets.add(new PendingSocket(component, socket, budgetCopy));
                }

                tree.add(component);
                StructurePieceStart.this.components.add(component);
            }
        }

        @Override
        protected void updateBoundingBox() {
            this.boundingBox = StructureBoundingBox.getNewBoundingBox();

            for (StructureComponent structurecomponent : this.components) {
                this.boundingBox.expandTo(structurecomponent.getBoundingBox());
            }

            if (!isPositioned) {
                this.boundingBox = new StructureBoundingBox(this.boundingBox.minX, this.boundingBox.minZ, this.boundingBox.maxX, this.boundingBox.maxZ);
            }
        }

        public void generateStructure(World world, Random rng, int chunkX, int chunkZ) {
            if (!isPositioned && chunkX == this.field_143024_c && chunkZ == this.field_143023_d) {
                int yLevel = getYLevel(world, rng, this, (StructurePieceComponent) this.components.get(0));

                for (StructureComponent c : this.components) {
                    StructurePieceComponent component = (StructurePieceComponent) c;

                    component.caabb.moveOrigin(new Vector3i(component.caabb.origin).add(0, yLevel, 0));
                }

                this.isPositioned = true;

                this.updateBoundingBox();
            }

            if (!isPositioned) {
                if (this.queuedChunks == null) this.queuedChunks = new LongArrayList();

                this.queuedChunks.add(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
                return;
            }

            StructureBoundingBox chunk = new StructureBoundingBox(chunkX << 4, chunkZ << 4, (chunkX << 4) + 15, (chunkZ << 4) + 15);

            XSTR rng2 = new XSTR();

            for (StructureComponent c : this.components) {
                StructurePieceComponent component = (StructurePieceComponent) c;

                if (component.getBoundingBox().intersectsWith(chunk)) {
                    long hash = getRNGSeed(chunkX, chunkZ);
                    hash = Fnv1a64.hashStep(hash, component.id);

                    rng2.setSeed(hash);

                    c.addComponentParts(world, rng2, chunk);
                }
            }

            if (this.queuedChunks != null) {
                queuedChunks.forEach(l -> {
                    int cX = (int) (l & 0xFFFFFFFFL);
                    int cZ = (int) ((l >> 32) & 0xFFFFFFFFL);

                    chunk.minX = cX << 4;
                    chunk.minZ = cZ << 4;
                    chunk.maxX = (cX << 4) + 15;
                    chunk.maxZ = (cZ << 4) + 15;

                    for (StructureComponent c : this.components) {
                        StructurePieceComponent component = (StructurePieceComponent) c;

                        if (component.getBoundingBox().intersectsWith(chunk)) {
                            long hash = getRNGSeed(cX, cZ);
                            hash = Fnv1a64.hashStep(hash, component.id);

                            rng2.setSeed(hash);

                            c.addComponentParts(world, rng2, chunk);
                        }
                    }
                });

                this.queuedChunks = null;
            }
        }

        @Override
        public void generateStructure(World p_75068_1_, Random p_75068_2_, StructureBoundingBox p_75068_3_) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NBTTagCompound func_143021_a(int chunkX, int chunkZ) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Piece", this.name);
            tag.setInteger("ChunkX", chunkX);
            tag.setInteger("ChunkZ", chunkZ);
            tag.setTag("BB", this.boundingBox.func_151535_h());

            NBTTagList components = new NBTTagList();

            for (StructureComponent component : this.components) {
                components.appendTag(component.func_143010_b());
            }

            tag.setTag("Children", components);
            this.func_143022_a(tag);
            return tag;
        }

        @Override
        public void func_143022_a(NBTTagCompound tag) {
            super.func_143022_a(tag);

            tag.setInteger("id", id);
            tag.setBoolean("isPositioned", isPositioned);
            tag.setInteger("originX", originX);
            tag.setInteger("originZ", originZ);

            if (this.queuedChunks != null) {
                IntArrayList xs = new IntArrayList(queuedChunks.size());
                IntArrayList zs = new IntArrayList(queuedChunks.size());

                queuedChunks.forEach(l -> {
                    int cX = (int) (l & 0xFFFFFFFFL);
                    int cZ = (int) ((l >> 32) & 0xFFFFFFFFL);

                    xs.add(cX);
                    zs.add(cZ);
                });

                tag.setIntArray("qx", xs.toIntArray());
                tag.setIntArray("qz", zs.toIntArray());
            }
        }

        @Override
        public void func_143017_b(NBTTagCompound tag) {
            super.func_143017_b(tag);

            this.id = tag.getInteger("id");
            this.isPositioned = tag.getBoolean("isPositioned");
            this.originX = tag.getInteger("originX");
            this.originZ = tag.getInteger("originZ");

            if (tag.hasKey("qx") && tag.hasKey("qz")) {
                int[] qx = tag.getIntArray("qx");
                int[] qz = tag.getIntArray("qz");

                this.queuedChunks = new LongArrayList();

                for (int i = 0; i < qx.length && i < qz.length; i++) {
                    queuedChunks.add(ChunkCoordIntPair.chunkXZ2Int(qx[i], qz[i]));
                }
            }
        }

        @Override
        public StructurePieceComponent loadStructureComponent(NBTTagCompound tag, World world) {
            return StructurePieceGenerator.this.loadStructureComponent(tag, world);
        }

        @Override
        public long getID() {
            return id;
        }

        @Override
        public AABBf getAABB(AABBf dest) {
            dest.setMin(this.boundingBox.minX, this.boundingBox.minY, this.boundingBox.minZ);
            dest.setMax(this.boundingBox.maxX, this.boundingBox.maxY, this.boundingBox.maxZ);

            return dest;
        }
    }

    @Desugar
    private record PendingSocket(StructurePieceComponent piece, StructureSocket socket, Object2IntOpenHashMap<String> budget) {

    }

    public static class StructurePieceComponent extends StructureComponent implements Boundable, Identifiable {

        private final String name;
        @Getter
        private final StructurePiece piece;

        private int id;

        private final VoxelAABB caabb;

        public StructurePieceComponent(String name, StructurePiece piece) {
            this.name = name;
            this.piece = piece;
            this.caabb = piece.aabb.clone();
        }

        public StructurePieceComponent(int componentType, String name, StructurePiece piece) {
            super(componentType);
            this.name = name;
            this.piece = piece;
            this.caabb = piece.aabb.clone();
        }

        @Override
        public NBTTagCompound func_143010_b() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Piece", this.name);
            tag.setInteger("O", this.coordBaseMode);
            tag.setInteger("id", id);
            tag.setInteger("x", caabb.origin.x);
            tag.setInteger("y", caabb.origin.y);
            tag.setInteger("z", caabb.origin.z);
            return tag;
        }

        @Override
        protected void func_143012_a(NBTTagCompound p_143012_1_) {

        }

        @Override
        public void func_143009_a(World world, NBTTagCompound tag) {
            this.coordBaseMode = tag.getInteger("O");
            id = tag.getInteger("id");
            caabb.moveOrigin(new Vector3i(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
        }

        @Override
        protected void func_143011_b(NBTTagCompound p_143011_1_) {

        }

        @Override
        public StructureBoundingBox getBoundingBox() {
            Vector3i min = this.caabb.min();
            Vector3i max = this.caabb.max();

            return new StructureBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
        }

        @Override
        public boolean addComponentParts(World world, Random rng, StructureBoundingBox boundingBox) {
            piece.place(world, caabb.origin, boundingBox);

            return true;
        }

        public List<StructureSocket> getSockets() {
            return piece.sockets == null ? Collections.emptyList() : piece.sockets;
        }

        @Override
        public AABBf getAABB(AABBf dest) {
            return caabb.getAABB(dest);
        }

        @Override
        public long getID() {
            return id;
        }

        @Override
        public String toString() {
            return "StructurePieceComponent{"
                + "name='"
                + name
                + '\''
                + ", piece="
                + piece
                + ", id="
                + id
                + ", caabb="
                + caabb
                + ", aabb="
                + getAABB(new AABBf())
                + '}';
        }
    }
}
