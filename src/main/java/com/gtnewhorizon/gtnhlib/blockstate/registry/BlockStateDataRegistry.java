package com.gtnewhorizon.gtnhlib.blockstate.registry;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataHandler;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateDataWorld;

/**
 * Holds the {@link BlockStateDataHandler}s and runs them, keeping exceptions and invalid data out.
 *
 * <p>
 * A handler registered for a block takes precedence over one registered for a class. For classes, the closest
 * superclass of the block wins. Only one handler can be registered for each block and each class.
 */
public final class BlockStateDataRegistry {

    /** like vanilla's block state variants, IE {@code east=false,north=true} */
    private static final Pattern VALID_DATA_PATTERN = Pattern.compile("[a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*");

    private static final Map<Block, BlockStateDataHandler> HANDLER_BY_BLOCK = new ConcurrentHashMap<>();
    private static final Map<Class<?>, BlockStateDataHandler> HANDLER_BY_CLASS = new ConcurrentHashMap<>();

    /** the resolved handler for each looked up block, empty for none */
    private static final Map<Block, Optional<BlockStateDataHandler>> HANDLER_CACHE = new ConcurrentHashMap<>();

    /** so each block's problem is only logged once */
    private static final Set<Block> LOGGED_BLOCKS = ConcurrentHashMap.newKeySet();

    private BlockStateDataRegistry() {}

    /** @return false if the block already has a handler */
    public static boolean register(Block block, BlockStateDataHandler handler) {
        return HANDLER_BY_BLOCK.putIfAbsent(block, handler) == null;
    }

    /**
     * Also applies to every block extending the class.
     *
     * @return false if the class already has a handler
     */
    public static boolean register(Class<? extends Block> blockClass, BlockStateDataHandler handler) {
        return HANDLER_BY_CLASS.putIfAbsent(blockClass, handler) == null;
    }

    /** @return null if the block doesn't have a handler */
    @Nullable
    public static BlockStateDataHandler getHandler(Block block) {
        return HANDLER_CACHE.computeIfAbsent(block, BlockStateDataRegistry::findHandler).orElse(null);
    }

    private static Optional<BlockStateDataHandler> findHandler(Block block) {
        BlockStateDataHandler handler = HANDLER_BY_BLOCK.get(block);
        if (handler != null) return Optional.of(handler);

        for (Class<?> clazz = block.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            handler = HANDLER_BY_CLASS.get(clazz);
            if (handler != null) return Optional.of(handler);
        }

        return Optional.empty();
    }

    /** @return the block's data, null if it has none, encoding failed or the data isn't allowed */
    @Nullable
    public static String encode(BlockStateDataHandler handler, IBlockAccess world, int x, int y, int z, Block block,
            int meta) {
        String data;
        try {
            data = handler.encode(world, x, y, z, block, meta);
        } catch (Exception e) {
            logOnce(block, "the handler's encode threw an exception", e);
            return null;
        }

        if (data == null || data.isEmpty()) return null;

        if (!VALID_DATA_PATTERN.matcher(data).matches()) {
            logOnce(
                    block,
                    "invalid data [" + data
                            + "], it must be name=value pairs separated by commas, using only [a-z 0-9 _]",
                    null);
            return null;
        }

        return data;
    }

    /** If decoding fails the world keeps what was set until then. */
    public static void decode(BlockStateDataHandler handler, Block block, int meta, String data,
            BlockStateDataWorld world) {
        try {
            handler.decode(block, meta, data, world);
        } catch (Exception e) {
            logOnce(block, "the handler's decode threw an exception", e);
        }
    }

    private static void logOnce(Block block, String problem, @Nullable Exception exception) {
        if (LOGGED_BLOCKS.add(block)) {
            GTNHLib.LOG.warn(
                    "Block state data for block [{}] is ignored, {}. This is only logged once per block.",
                    Block.blockRegistry.getNameForObject(block),
                    problem,
                    exception);
        }
    }
}
