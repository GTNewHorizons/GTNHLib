package com.gtnewhorizon.gtnhlib.network.transformation;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.transformation.TransformationAction;
import com.gtnewhorizon.gtnhlib.transformation.TransformationController;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

import cpw.mods.fml.common.network.ByteBufUtils;

public class TransformationRequestPacket implements IPacket {

    private String transformId;
    private TransformationAction type;

    public TransformationRequestPacket() {}

    public TransformationRequestPacket(final String transformId, TransformationAction type) {
        this.transformId = transformId;
        this.type = type;
    }

    public TransformationRequestPacket(final String transformId) {
        this(transformId, TransformationAction.TOGGLE);
    }

    public static TransformationRequestPacket getUntransformPacket() {
        return new TransformationRequestPacket("", TransformationAction.UNTRANSFORM);
    }

    @Override
    public void encode(final PacketBuffer buf) throws IOException {
        ByteBufUtils.writeUTF8String(buf, transformId);
        buf.writeInt(type.getId());
    }

    @Override
    public void decode(final PacketBuffer buf) throws IOException {
        transformId = ByteBufUtils.readUTF8String(buf);
        final int typeId = buf.readInt();

        if (transformId.isEmpty()) {
            type = TransformationAction.UNTRANSFORM;
        } else {
            type = TransformationAction.getById(typeId);
        }
    }

    @Override
    public IPacket executeServer(final NetHandlerPlayServer handler) {
        final TransformationProperty prop = TransformationProperty.get(handler.playerEntity);
        if (prop == null) {
            return null;
        }

        final ITransformation currentForm = prop.getTransformation();
        final ITransformation requestedForm = TransformationController.getTransformationById(transformId);

        switch (type) {
            case TRANSFORM -> {
                if (requestedForm != null && currentForm != requestedForm) {
                    prop.setTransformation(requestedForm);
                }
            }
            case UNTRANSFORM -> prop.setTransformation(null);
            case TOGGLE -> {
                if (currentForm == null) {
                    prop.setTransformation(requestedForm);
                } else {
                    prop.setTransformation(null);
                }
            }
        }

        return null;
    }
}
