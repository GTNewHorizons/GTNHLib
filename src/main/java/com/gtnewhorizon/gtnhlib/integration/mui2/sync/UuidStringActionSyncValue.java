package com.gtnewhorizon.gtnhlib.integration.mui2.sync;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.network.PacketBuffer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.value.sync.AbstractGenericSyncValue;
import com.gtnewhorizon.gtnhlib.teams.Team;

public class UuidStringActionSyncValue extends AbstractGenericSyncValue<Pair<UUID, String>, UuidStringActionSyncValue> {

    public UuidStringActionSyncValue(@Nullable Consumer<Pair<UUID, String>> serverSetter) {
        super(
                (Class<Pair<UUID, String>>) (Class<?>) Pair.class,
                () -> null,
                clientSetValue -> {},
                () -> null,
                serverSetter);
        this.allowC2S();
    }

    @Override
    protected Pair<UUID, String> createDeepCopyOf(Pair<UUID, String> value) {
        if (value == null) {
            return null;
        }
        return new ImmutablePair<>(value.getLeft(), value.getRight());
    }

    @Override
    protected boolean areEqual(Pair<UUID, String> a, Pair<UUID, String> b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getLeft().equals(b.getLeft()) && a.getRight().equals(b.getRight());
    }

    @Override
    protected void serialize(PacketBuffer buffer, Pair<UUID, String> value) throws IOException {
        // SyncValueIsNull, uuidIsNull, UUID, stringIsNull, String
        if (value == null) {
            buffer.writeBoolean(true);
            return;
        }
        buffer.writeBoolean(false);
        buffer.writeBoolean(value.getLeft() == null);
        if (value.getLeft() != null) {
            buffer.writeLong(value.getLeft().getMostSignificantBits());
            buffer.writeLong(value.getLeft().getLeastSignificantBits());
        }
        buffer.writeBoolean(value.getRight() == null);
        if (value.getRight() != null) {
            buffer.writeStringToBuffer(value.getRight());
        }
    }

    @Override
    protected Pair<UUID, String> deserialize(PacketBuffer buffer) throws IOException {
        if (buffer.readBoolean()) {
            return null;
        }

        UUID firstValue = null;
        if (!buffer.readBoolean()) {
            firstValue = new UUID(buffer.readLong(), buffer.readLong());
        }

        String secondValue = null;
        if (!buffer.readBoolean()) {
            secondValue = buffer.readStringFromBuffer(Team.MAX_TEAM_NAME_LENGTH);
        }
        return new ImmutablePair<>(firstValue, secondValue);
    }
}
