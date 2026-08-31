package com.gtnewhorizon.gtnhlib.blockstate.storage;

import java.nio.ByteBuffer;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.falsepattern.chunk.api.DataManager;
import com.falsepattern.chunk.api.DataManager.BlockPacketDataManager;
import com.falsepattern.chunk.api.DataManager.CubicPacketDataManager;
import com.falsepattern.chunk.api.DataManager.PacketDataManager;
import com.falsepattern.chunk.api.DataManager.SubChunkDataManager;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.util.EBSUtil;
import com.gtnewhorizon.gtnhlib.util.JsonUtil;
import io.netty.buffer.Unpooled;

@SuppressWarnings({ "resource", "UnstableApiUsage" })
public class BSSDataManager implements DataManager, PacketDataManager, CubicPacketDataManager, BlockPacketDataManager,
    SubChunkDataManager {

    public static final int MAX_BYTES_PER_STATE = 1024;

    private static final ThreadLocal<PacketBuffer> STATE_BUFFERS = ThreadLocal.withInitial(() -> new PacketBuffer(Unpooled.buffer(MAX_BYTES_PER_STATE)));

    @Contract(pure = true)
    @Override
    public String domain() {
        return GTNHLib.MODID;
    }

    @Contract(pure = true)
    @Override
    public String id() {
        return "blockstate";
    }

    @Contract(pure = true)
    @Override
    public @NotNull String version() {
        return "1.0.0";
    }

    @Contract(pure = true)
    @Override
    public @Nullable String newInstallDescription() {
        return "GTNHLib native block state storage. This allows mods to emulate modern's BlockStates without metadata shims.";
    }

    @Contract(pure = true)
    @Override
    public @NotNull String uninstallMessage() {
        return "GTNHLib's native block state storage has been uninstalled. Any blocks that use this will be corrupted.";
    }

    @Contract(pure = true)
    @Override
    public @Nullable String versionChangeMessage(String priorVersion) {
        return null;
    }

    @Contract(mutates = "param1")
    @Override
    public void writeBlockToPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        // Server world -> S23

        if (!(packet.field_148883_d instanceof NativeBlockStateAware)) return;
        if (!(packet instanceof BlockState_S23Ext ext)) return;

        ExtendedBlockStorage ebs = EBSUtil.getEBS(chunk, y >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, false);

        if (storage == null) return;

        BlockState state = storage.getBlockStateUnsafe(x & 0xF, y & 0xF, z & 0xF);

        ext.gtnhlib$setBlockState(state == null ? null : state.toJson());
    }

    @Contract(mutates = "param2")
    @Override
    public void writeBlockPacketToBuffer(S23PacketBlockChange packet, PacketBuffer buffer) {
        // S23 -> buffer

        if (!(packet.field_148883_d instanceof NativeBlockStateAware)) return;
        if (!(packet instanceof BlockState_S23Ext ext)) return;

        JsonUtil.writeBSON(buffer, ext.gtnhlib$getBlockState());
    }

    @Contract(mutates = "param1,param2")
    @Override
    public void readBlockPacketFromBuffer(S23PacketBlockChange packet, PacketBuffer buffer) {
        // Buffer -> S23

        if (!(packet.field_148883_d instanceof NativeBlockStateAware bsa)) return;
        if (!(packet instanceof BlockState_S23Ext ext)) return;

        try {
            ext.gtnhlib$setBlockState(JsonUtil.readBSON(buffer));
        } catch (Throwable t) {
            GTNHLib.LOG.error(
                "Failed to load BlockState from S23PacketBlockChange (X={}, Y={}, Z={}, Block={})",
                packet.func_148879_d(),
                packet.func_148878_e(),
                packet.func_148877_f(),
                bsa,
                t);
        }
    }

    @Contract(mutates = "param1,param5")
    @Override
    public void readBlockFromPacket(Chunk chunk, int x, int y, int z, S23PacketBlockChange packet) {
        // S23 -> client world
        if (!(packet instanceof BlockState_S23Ext ext)) return;

        NativeBlockStateAware bsa = packet.field_148883_d instanceof NativeBlockStateAware bsa2 ? bsa2 : null;

        ExtendedBlockStorage ebs = EBSUtil.getEBS(chunk, y >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, bsa != null);

        if (bsa == null) {
            if (storage != null && storage.hasState(x & 0xF, y & 0xF, z & 0xF)) {
                storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, null);

                if (storage.isEmpty()) {
                    BlockStateStorage.removeStorage(ebs);
                }
            }

            return;
        }

        Objects.requireNonNull(storage, "Storage should not be null");

        BlockState blockState = bsa.getDefaultState(null);

        blockState.fromJson(ext.gtnhlib$getBlockState());

        if (blockState.needsReification()) {
            blockState.reify(chunk.worldObj, (chunk.xPosition << 4) + x, y, (chunk.zPosition << 4) + z);
        }

        storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, blockState);

        chunk.func_150807_a(x & 0xF, y, z & 0xF, bsa.getBlockForState(blockState), bsa.getMetaForState(blockState));

        chunk.worldObj.markBlockForUpdate((chunk.xPosition << 4) + x, y, (chunk.zPosition << 4) + z);
    }

    @Contract(pure = true)
    @Override
    public int maxPacketSizeCubic() {
        return MAX_BYTES_PER_STATE * 16 * 16 * 16;
    }

    @Contract(mutates = "param3")
    @Override
    public void writeToBuffer(Chunk chunk, ExtendedBlockStorage ebs, ByteBuffer buffer) {
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, false);

        PacketBuffer packet = new PacketBuffer(Unpooled.wrappedBuffer(buffer));
        packet.clear();

        if (storage == null) {
            packet.writeVarIntToBuffer(0);
            buffer.position(buffer.position() + packet.writerIndex());
            return;
        }

        var states = storage.getBlockStates();
        packet.writeVarIntToBuffer(states.size());

        PacketBuffer stateBuffer = STATE_BUFFERS.get();

        states.forEach((loc, state) -> {
            stateBuffer.clear();

            stateBuffer.writeShort(loc);
            JsonUtil.writeBSON(stateBuffer, state.toJson());

            int stateLen = stateBuffer.writerIndex();

            if (stateLen > MAX_BYTES_PER_STATE) {
                GTNHLib.LOG.error("Block state tried to sync too many bytes: reduce the number of bytes used, or increase the limit in the GTNHLib config. (Bytes used={}, State={})", stateLen, state);

                stateBuffer.clear();
                stateBuffer.writeShort(loc);
                JsonUtil.writeBSON(stateBuffer, new JsonObject());

                stateLen = stateBuffer.writerIndex();
            }

            packet.writeVarIntToBuffer(stateLen);
            packet.writeBytes(stateBuffer, 0, stateLen);
        });

        buffer.position(buffer.position() + packet.writerIndex());
    }

    @Contract(mutates = "param1,param2,param3")
    @Override
    public void readFromBuffer(Chunk chunk, ExtendedBlockStorage ebs, ByteBuffer buffer) {
        PacketBuffer packet = new PacketBuffer(Unpooled.wrappedBuffer(buffer));
        packet.writerIndex(buffer.remaining()).readerIndex(0);

        int stateCount = packet.readVarIntFromBuffer();

        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, stateCount > 0);

        if (stateCount == 0 && storage != null) {
            // We have a storage, but we don't want to have any states, remove the storage to clear them
            BlockStateStorage.removeStorage(ebs);
            buffer.position(buffer.position() + packet.readerIndex());
            return;
        }

        if (storage == null) {
            // No storage and we aren't adding any new states, do nothing
            buffer.position(buffer.position() + packet.readerIndex());
            return;
        }

        PacketBuffer stateBuffer = STATE_BUFFERS.get();

        for (int i = 0; i < stateCount; i++) {
            stateBuffer.clear();

            int stateLen = packet.readVarIntFromBuffer();
            packet.readBytes(stateBuffer, 0, stateLen);
            stateBuffer.readerIndex(0).writerIndex(stateLen);

            short loc = stateBuffer.readShort();
            JsonObject props = JsonUtil.readBSON(stateBuffer);

            int rx = BlockStateStorage.unpackX(loc);
            int ry = BlockStateStorage.unpackY(loc);
            int rz = BlockStateStorage.unpackZ(loc);

            if (!(ebs.getBlockByExtId(rx, ry, rz) instanceof NativeBlockStateAware bsa)) {
                continue;
            }

            try {
                BlockState state = bsa.getDefaultState(null);

                state.fromJson(props);

                if (state.needsReification()) {
                    state.reify(chunk.worldObj, (chunk.xPosition << 4) + rx, ebs.getYLocation() + ry, (chunk.zPosition << 4) + rz);
                }

                storage.setBlockState(rx, ry, rz, state);
            } catch (Throwable t) {
                GTNHLib.LOG.error(
                    "Failed to load BlockState (World={}, X={}, Y={}, Z={}, Block={})",
                    chunk.worldObj,
                    (chunk.xPosition << 4) + rx,
                    ebs.getYLocation() + ry,
                    (chunk.zPosition << 4) + rz,
                    bsa,
                    t);
            }
        }

        buffer.position(buffer.position() + packet.readerIndex());
    }

    @Contract(pure = true)
    @Override
    public int maxPacketSize() {
        return MAX_BYTES_PER_STATE * 16 * 16 * 16 * 16;
    }

    @Contract(mutates = "param4")
    @Override
    public void writeToBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        var ebses = chunk.getBlockStorageArray();

        for (int i = 0; i < ebses.length; i++) {
            if ((subChunkMask & 1 << i) == 0) continue;

            var ebs = ebses[i];

            writeToBuffer(chunk, ebs, buffer);
        }
    }

    @Contract(mutates = "param1,param4")
    @Override
    public void readFromBuffer(Chunk chunk, int subChunkMask, boolean forceUpdate, ByteBuffer buffer) {
        var ebses = chunk.getBlockStorageArray();

        for (int i = 0; i < ebses.length; i++) {
            if ((subChunkMask & 1 << i) == 0) continue;

            var ebs = ebses[i];

            readFromBuffer(chunk, ebs, buffer);
        }
    }

    @Contract(mutates = "param3")
    @Override
    public void writeSubChunkToNBT(Chunk chunk, ExtendedBlockStorage ebs, NBTTagCompound nbt) {
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, false);
        if (storage == null) return;

        var states = storage.getBlockStates();
        if (states.isEmpty()) return;

        PacketBuffer stateBuffer = STATE_BUFFERS.get();
        NBTTagList list = new NBTTagList();

        states.forEach((loc, state) -> {
            stateBuffer.clear();
            JsonUtil.writeBSON(stateBuffer, state.toJson());
            int len = stateBuffer.writerIndex();
            byte[] data = new byte[len];
            stateBuffer.getBytes(0, data);

            NBTTagCompound entry = new NBTTagCompound();
            entry.setShort("loc", loc);
            entry.setByteArray("data", data);
            list.appendTag(entry);
        });

        nbt.setTag("states", list);
    }

    @Contract(mutates = "param2")
    @Override
    public void readSubChunkFromNBT(Chunk chunk, ExtendedBlockStorage ebs, NBTTagCompound nbt) {
        if (!nbt.hasKey("states")) return;

        NBTTagList list = nbt.getTagList("states", 10); // 10 = TAG_Compound
        if (list.tagCount() == 0) return;

        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, true);
        if (storage == null) return;

        PacketBuffer stateBuffer = STATE_BUFFERS.get();

        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            short loc = entry.getShort("loc");
            byte[] data = entry.getByteArray("data");

            stateBuffer.clear();
            stateBuffer.writeBytes(data);
            stateBuffer.readerIndex(0);

            JsonObject props = JsonUtil.readBSON(stateBuffer);

            int rx = BlockStateStorage.unpackX(loc);
            int ry = BlockStateStorage.unpackY(loc);
            int rz = BlockStateStorage.unpackZ(loc);

            if (!(ebs.getBlockByExtId(rx, ry, rz) instanceof NativeBlockStateAware bsa)) continue;

            try {
                BlockState state = bsa.getDefaultState(null);

                state.fromJson(props);

                // EBS is not in the world at this point so we can't reify the state here

                storage.setBlockState(rx, ry, rz, state);
            } catch (Throwable t) {
                GTNHLib.LOG.error(
                    "Failed to load BlockState (World={}, X={}, Y={}, Z={}, Block={})",
                    chunk.worldObj,
                    (chunk.xPosition << 4) + rx,
                    ebs.getYLocation() + ry,
                    (chunk.zPosition << 4) + rz,
                    bsa,
                    t);
            }
        }
    }

    @Contract(mutates = "param3")
    @Override
    public void cloneSubChunk(Chunk fromChunk, ExtendedBlockStorage from, ExtendedBlockStorage to) {
        if (!(to instanceof BlockState_EBSExt toExt)) return;

        BlockStateStorage storage = BlockStateStorage.getStorage(from, false);

        toExt.gtnhlib$setBlockStateStorage(storage == null ? null : storage.clone());
    }
}
