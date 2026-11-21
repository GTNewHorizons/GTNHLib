package com.gtnewhorizon.gtnhlib.network.payload;

import cpw.mods.fml.relauncher.Side;

public class DirectionalPayloadHandler<T extends CustomPacketPayload> implements IPayloadHandler<T> {

    private final IPayloadHandler<T> clientSide;
    private final IPayloadHandler<T> serverSide;

    public DirectionalPayloadHandler(IPayloadHandler<T> clientSide, IPayloadHandler<T> serverSide) {
        this.clientSide = clientSide;
        this.serverSide = serverSide;
    }

    @Override
    public void handle(T payload, IPayloadContext context) {
        (context.side() == Side.CLIENT ? clientSide : serverSide).handle(payload, context);
    }
}
