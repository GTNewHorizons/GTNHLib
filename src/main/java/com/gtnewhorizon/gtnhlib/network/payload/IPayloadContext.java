package com.gtnewhorizon.gtnhlib.network.payload;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;

public interface IPayloadContext {

    /** The sender on the server, the local player on the client. */
    EntityPlayer player();

    /** The receiving side. Replaces NeoForge's {@code flow()}. */
    Side side();

    void reply(CustomPacketPayload payload);

    /** Runs on the main thread, inline if already on it. A task that throws closes the connection. */
    CompletableFuture<Void> enqueueWork(Runnable task);

    <T> CompletableFuture<T> enqueueWork(Supplier<T> task);
}
