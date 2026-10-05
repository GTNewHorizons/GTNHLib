package com.gtnewhorizon.gtnhlib.network.transformation;

import com.gtnewhorizon.gtnhlib.api.transformation.TransformationRegistry;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;
import cpw.mods.fml.common.network.ByteBufUtils;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

public class TransformationRequestPacket implements IPacket {
    private String transformId;

    public TransformationRequestPacket() {}

    public TransformationRequestPacket(final String transformId) {
        this.transformId = transformId;
    }

    @Override
    public void encode(final PacketBuffer buf) throws IOException {
        ByteBufUtils.writeUTF8String(buf, transformId);
    }

    @Override
    public void decode(final PacketBuffer buf) throws IOException {
        transformId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public IPacket executeServer(final NetHandlerPlayServer handler) {
        final TransformationProperty prop = TransformationProperty.get(handler.playerEntity);
        if (prop != null) {
            if (prop.getTransformation() != null) {
                prop.setTransformation(null);
            } else {
                prop.setTransformation(TransformationRegistry.getTransformation(transformId));
            }
        }

        return null;
    }
}
