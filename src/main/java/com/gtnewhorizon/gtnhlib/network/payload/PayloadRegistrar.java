package com.gtnewhorizon.gtnhlib.network.payload;

import com.gtnewhorizon.gtnhlib.codec.stream.RegistryFriendlyByteBuf;
import com.gtnewhorizon.gtnhlib.codec.stream.StreamCodec;

public class PayloadRegistrar {

    private final PayloadChannel channel;

    /** @param channelName FML channel name, at most 20 chars. Registrars with the same name share a channel. */
    public PayloadRegistrar(String channelName) {
        this.channel = PayloadChannel.named(channelName);
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        channel.register(type, codec, handler, true, false);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        channel.register(type, codec, handler, false, true);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        channel.register(type, codec, handler, true, true);
        return this;
    }
}
