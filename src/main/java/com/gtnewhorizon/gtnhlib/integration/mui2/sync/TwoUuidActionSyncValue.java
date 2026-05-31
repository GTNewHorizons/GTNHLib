package com.gtnewhorizon.gtnhlib.integration.mui2.sync;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.PacketBuffer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.cleanroommc.modularui.value.sync.AbstractGenericSyncValue;

public class TwoUuidActionSyncValue extends AbstractGenericSyncValue<Pair<UUID, UUID>> {

    public TwoUuidActionSyncValue(@Nullable Consumer<Pair<UUID, UUID>> serverSetter) {
        super(
                (Class<Pair<UUID, UUID>>) (Class<?>) Pair.class,
                () -> null,
                clientSetValue -> {},
                () -> null,
                serverSetter);
    }

    @Override
    protected Pair<UUID, UUID> createDeepCopyOf(Pair<UUID, UUID> value) {
        if (value == null) {
            return null;
        }
        return new ImmutablePair<>(value.getLeft(), value.getRight());
    }

    @Override
    protected boolean areEqual(Pair<UUID, UUID> a, Pair<UUID, UUID> b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getLeft().equals(b.getLeft()) && a.getRight().equals(b.getRight());
    }

    @Override
    protected void serialize(PacketBuffer buffer, Pair<UUID, UUID> value) throws IOException {
        // SyncValueIsNull, LeftUUIDIsNull, LeftUUID, RightUUIDIsNull, RightUUID
        if (value == null) {
            buffer.writeBoolean(true);
        }
        buffer.writeBoolean(false);
        buffer.writeBoolean(value.getLeft() == null);
        if (value.getLeft() != null) {
            buffer.writeLong(value.getLeft().getMostSignificantBits());
            buffer.writeLong(value.getLeft().getLeastSignificantBits());
        }
        buffer.writeBoolean(value.getRight() == null);
        if (value.getRight() != null) {
            buffer.writeLong(value.getRight().getMostSignificantBits());
            buffer.writeLong(value.getRight().getLeastSignificantBits());
        }
    }

    @Override
    protected Pair<UUID, UUID> deserialize(PacketBuffer buffer) throws IOException {
        if (buffer.readBoolean()) {
            return null;
        }
        UUID firstValue = null;
        if (!buffer.readBoolean()) {
            firstValue = new UUID(buffer.readLong(), buffer.readLong());
        }
        UUID secondValue = null;
        if (!buffer.readBoolean()) {
            secondValue = new UUID(buffer.readLong(), buffer.readLong());
        }
        return new ImmutablePair<>(firstValue, secondValue);
    }
}
