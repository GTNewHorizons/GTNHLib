package com.gtnewhorizon.gtnhlib.network.payload;

/** Called on the main thread. Throwing closes the connection. */
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload> {

    void handle(T payload, IPayloadContext context);
}
