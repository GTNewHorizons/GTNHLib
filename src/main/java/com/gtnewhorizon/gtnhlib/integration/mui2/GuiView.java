package com.gtnewhorizon.gtnhlib.integration.mui2;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.network.PacketBuffer;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record GuiView(ScreenType type, UUID currentTeam) {

    public GuiView(ScreenType type, @Nonnull UUID currentTeam) {
        this.type = type;
        this.currentTeam = currentTeam;
    }

    public static GuiView getDefaultView(@Nonnull UUID team) {
        return new GuiView(ScreenType.PLAYER_LIST, team);
    }

    public void writeToBuf(PacketBuffer buffer) {
        buffer.writeInt(type.ordinal());
        buffer.writeLong(currentTeam.getMostSignificantBits());
        buffer.writeLong(currentTeam.getLeastSignificantBits());
    }

    public static GuiView readFromBuf(PacketBuffer buffer) {
        return new GuiView(ScreenType.values()[buffer.readInt()], new UUID(buffer.readLong(), buffer.readLong()));
    }

    public static GuiView deepCopyOf(GuiView value) {
        return new GuiView(value.type, value.currentTeam);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GuiView guiView)) return false;
        return type == guiView.type && Objects.equals(currentTeam, guiView.currentTeam);
    }

}
