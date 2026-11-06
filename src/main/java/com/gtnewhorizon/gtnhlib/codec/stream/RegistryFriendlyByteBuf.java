package com.gtnewhorizon.gtnhlib.codec.stream;

import io.netty.buffer.ByteBuf;

/** API parity with 1.21 only. Registries are global in 1.7.10. */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {

    public RegistryFriendlyByteBuf(ByteBuf source) {
        super(source);
    }
}
