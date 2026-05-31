package com.gtnewhorizon.gtnhlib.integration.mui2;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;

public class TeamGuiFactory extends AbstractUIFactory<TeamGuiData> {

    public static final TeamGuiFactory INSTANCE = new TeamGuiFactory("gtnhlib:team_panel_gui", TeamGui::new);

    private final Supplier<IGuiHolder<TeamGuiData>> guiHolderSupplier;
    private IGuiHolder<TeamGuiData> guiHolder;

    @Override
    public @NotNull IGuiHolder<TeamGuiData> getGuiHolder(TeamGuiData data) {
        if (this.guiHolder == null) {
            this.guiHolder = this.guiHolderSupplier.get();
            Objects.requireNonNull(this.guiHolder, "IGuiHolder must not be null");
        }
        return this.guiHolder;
    }

    public TeamGuiFactory(String name, IGuiHolder<TeamGuiData> guiHolder) {
        this(name, () -> guiHolder);
    }

    public TeamGuiFactory(String name, Supplier<IGuiHolder<TeamGuiData>> guiHolderSupplier) {
        super(name);
        this.guiHolderSupplier = guiHolderSupplier;
        GuiManager.registerFactory(this);
    }

    public void init() {}

    public void open(EntityPlayerMP player, @Nonnull UUID playerTeam) {
        GuiManager.open(this, new TeamGuiData(player, GuiView.getDefaultView(playerTeam)), player);
    }

    @Override
    public void writeGuiData(TeamGuiData guiData, PacketBuffer buffer) {
        guiData.currentView.writeToBuf(buffer);
    }

    @Override
    public @NotNull TeamGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        return new TeamGuiData(player, GuiView.readFromBuf(buffer));
    }

    public IGuiHolder<TeamGuiData> getGuiHolder() {
        if (this.guiHolder == null) {
            this.guiHolder = this.guiHolderSupplier.get();
            Objects.requireNonNull(this.guiHolder, "IGuiHolder must not be null");
        }
        return guiHolder;
    }
}
