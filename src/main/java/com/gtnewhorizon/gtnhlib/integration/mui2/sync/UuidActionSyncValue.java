package com.gtnewhorizon.gtnhlib.integration.mui2.sync;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.value.sync.AbstractGenericSyncValue;

public class UuidActionSyncValue extends AbstractGenericSyncValue<UUID, UuidActionSyncValue> {

    public UuidActionSyncValue(@Nullable Consumer<UUID> serverSetter) {
        super(UUID.class, () -> null, clientSetValue -> {}, () -> null, serverSetter);
        this.allowC2S();
    }

    @Override
    protected UUID createDeepCopyOf(UUID value) {
        return value;
    }

    @Override
    protected boolean areEqual(UUID a, UUID b) {
        if (a == null || b == null) return a == b;
        return a.equals(b);
    }

    @Override
    protected void serialize(PacketBuffer buffer, UUID value) throws IOException {
        buffer.writeBoolean(value == null);
        if (value != null) {
            buffer.writeLong(value.getMostSignificantBits());
            buffer.writeLong(value.getLeastSignificantBits());
        }
    }

    @Override
    protected UUID deserialize(PacketBuffer buffer) throws IOException {
        if (buffer.readBoolean()) return null;
        return new UUID(buffer.readLong(), buffer.readLong());
    }
}
