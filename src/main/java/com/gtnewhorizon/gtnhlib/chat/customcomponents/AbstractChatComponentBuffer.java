package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.util.Base64;

import net.minecraft.network.PacketBuffer;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;

import io.netty.buffer.Unpooled;

/**
 * There are some help method for serializing or deserializing item/fluid to/from bytes or Base64 string. So we make the
 * class generic so methods can return the concrete subtype.
 *
 * @param <T> Just use the same type as the class itself.
 */
public abstract class AbstractChatComponentBuffer<T extends AbstractChatComponentBuffer<T>>
        extends AbstractChatComponentCustom {

    public abstract void encode(PacketBuffer buf);

    public abstract void decode(PacketBuffer buf);

    @Override
    public @NotNull JsonElement serialize() {
        final JsonObject obj = new JsonObject();

        String encoded = this.encodeToString();

        obj.addProperty(this.getID(), encoded);
        return obj;
    }

    /**
     * @return Base64 encoding (default)
     */
    public String encodeToString() {
        byte[] bytes = this.encodeToBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public byte[] encodeToBytes() {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        try {
            this.encode(buf);
        } catch (Exception e) {
            GTNHLib.LOG.error("Error encoding chat component {}: {}", this.getID(), e.getMessage());
            return new byte[0];
        }

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    @Override
    public void deserialize(@NotNull JsonElement jsonElement) {
        JsonObject obj = jsonElement.getAsJsonObject();
        final String id = this.getID();

        if (!obj.has(id)) {
            GTNHLib.LOG.error("Missing id for deserializing chat component '{}'", id);
            return;
        }

        String encoded = obj.get(id).getAsString();
        this.decodeFromString(encoded);
    }

    /**
     * Decode from String
     *
     * @param encoded Base64 encoding (default)
     * @return self with concrete type
     */
    @SuppressWarnings("UnusedReturnValue")
    public T decodeFromString(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        return decodeFromBytes(bytes);
    }

    @SuppressWarnings("unchecked")
    public T decodeFromBytes(byte[] bytes) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer(bytes.length));
        try {
            buf.writeBytes(bytes);
            // reset readerIndex to 0 in case writeBytes moved it; ensure readable from start
            buf.readerIndex(0);

            this.decode(buf);
        } catch (Exception e) {
            GTNHLib.LOG.error("Error decoding chat component {}: {}", this.getID(), e.getMessage());
        } finally {
            buf.release();
        }
        return (T) this;
    }

}
