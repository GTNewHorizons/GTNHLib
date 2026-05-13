package com.gtnewhorizon.gtnhlib.network.base;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class NetworkUtils {

    public static final int MAX_STRING_BYTES = 32767;

    public static boolean isClient(EntityPlayer player) {
        if (player == null) throw new NullPointerException("Cannot determine side of a null player");
        return player.worldObj == null ? player instanceof EntityPlayerSP : player.worldObj.isRemote;
    }

    public static void writePacketBuffer(PacketBuffer writeTo, PacketBuffer writeFrom) {
        writeTo.writeVarIntToBuffer(writeFrom.readableBytes());
        writeTo.writeBytes(writeFrom);
    }

    public static PacketBuffer readPacketBuffer(PacketBuffer buf) {
        ByteBuf slice = buf.readBytes(buf.readVarIntFromBuffer());
        ByteBuf copy = Unpooled.copiedBuffer(slice);
        slice.release();
        return new PacketBuffer(copy);
    }

    public static void writeFluidStack(PacketBuffer buffer, @Nullable FluidStack fluidStack) throws IOException {
        if (fluidStack == null) {
            buffer.writeBoolean(true);
        } else {
            buffer.writeBoolean(false);
            buffer.writeNBTTagCompoundToBuffer(fluidStack.writeToNBT(new NBTTagCompound()));
        }
    }

    @Nullable
    public static FluidStack readFluidStack(PacketBuffer buffer) throws IOException {
        if (buffer.readBoolean()) return null;
        return FluidStack.loadFluidStackFromNBT(buffer.readNBTTagCompoundFromBuffer());
    }

    public static void writeEnumValue(PacketBuffer buffer, Enum<?> value) {
        buffer.writeVarIntToBuffer(value.ordinal());
    }

    public static <T extends Enum<T>> T readEnumValue(PacketBuffer buffer, Class<T> enumClass) {
        T[] constants = enumClass.getEnumConstants();
        int ordinal = buffer.readVarIntFromBuffer();

        // Bounds check to prevent crashes from malformed/poisoned packets
        if (ordinal < 0 || ordinal >= constants.length) {
            throw new IllegalArgumentException("Invalid ordinal " + ordinal + " for enum " + enumClass.getName());
        }

        return constants[ordinal];
    }

    public static void writeUUID(PacketBuffer buffer, UUID uuid) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUUID(PacketBuffer buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    public static void writeBlockPos(PacketBuffer buffer, BlockPos pos) {
        buffer.writeVarIntToBuffer(pos.getX());
        buffer.writeVarIntToBuffer(pos.getY());
        buffer.writeVarIntToBuffer(pos.getZ());
    }

    public static BlockPos readBlockPos(PacketBuffer buffer) {
        return new BlockPos(
                buffer.readVarIntFromBuffer(),
                buffer.readVarIntFromBuffer(),
                buffer.readVarIntFromBuffer());
    }

}
