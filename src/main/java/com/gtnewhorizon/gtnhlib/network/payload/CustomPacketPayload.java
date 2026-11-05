package com.gtnewhorizon.gtnhlib.network.payload;

import net.minecraft.util.ResourceLocation;

public interface CustomPacketPayload {

    Type<? extends CustomPacketPayload> type();

    final class Type<T extends CustomPacketPayload> {

        private final ResourceLocation id;

        public Type(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Type<?>otherType && id.equals(otherType.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }
}
