package com.gtnewhorizon.gtnhlib.integration.mui2.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.integration.mui2.DisplayItem;

public class DisplayListSync {

    public final boolean forceRefresh;
    public final List<DisplayItem> data;

    protected DisplayListSync(boolean forceRefresh, @Nonnull List<DisplayItem> data) {
        this.forceRefresh = forceRefresh;
        this.data = data;
    }

    public static DisplayListSync copyOfIgnoreRefresh(DisplayListSync other) {
        List<DisplayItem> copy = other.data.stream().map(DisplayItem::copyOf).collect(Collectors.toList());
        return new DisplayListSync(false, copy);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DisplayListSync that)) return false;
        return forceRefresh == that.forceRefresh && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forceRefresh, data);
    }

    protected void serializeInto(PacketBuffer buffer) {
        buffer.writeBoolean(forceRefresh);
        buffer.writeInt(data.size());
        data.forEach(displayItem -> displayItem.serializeInto(buffer));
    }

    protected static DisplayListSync deserializeFrom(PacketBuffer buffer) {
        boolean forceRefresh = buffer.readBoolean();
        int size = buffer.readInt();
        List<DisplayItem> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            data.add(DisplayItem.deserializeFrom(buffer));
        }
        return new DisplayListSync(forceRefresh, data);
    }
}
