package com.gtnewhorizon.gtnhlib.network.transformation;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Objects;
import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.transformation.TransformationController;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

import cpw.mods.fml.common.network.ByteBufUtils;

public class TransformationAnnouncePacket implements IPacket {

    private int entityId;
    private String transformId;

    public TransformationAnnouncePacket() {}

    public TransformationAnnouncePacket(final int entityId, final String transformId) {
        this.entityId = entityId;
        this.transformId = Objects.firstNonNull(transformId, "");
    }

    public TransformationAnnouncePacket(@NotNull final EntityPlayer player, @NotNull final ITransformation tf) {
        this(player.getEntityId(), tf.getId());
    }

    @Override
    public void encode(final PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, transformId);
    }

    @Override
    public void decode(final PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        transformId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public IPacket executeClient(final NetHandlerPlayClient handler) {
        final WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world != null && world.getEntityByID(entityId) instanceof final EntityPlayer player) {
            final TransformationProperty property = TransformationProperty.get(player);
            if (property != null) {
                if (transformId.isEmpty()) {
                    property.removeTransformation();
                } else {
                    property.setTransformation(TransformationController.getTransformationById(transformId));
                }
            }
        }

        return null;
    }
}
