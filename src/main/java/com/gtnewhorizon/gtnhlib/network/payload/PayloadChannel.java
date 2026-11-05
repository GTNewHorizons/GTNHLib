package com.gtnewhorizon.gtnhlib.network.payload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;

import org.jetbrains.annotations.ApiStatus;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.codec.stream.IdDispatchCodec;
import com.gtnewhorizon.gtnhlib.codec.stream.RegistryFriendlyByteBuf;
import com.gtnewhorizon.gtnhlib.codec.stream.StreamCodec;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

@ApiStatus.Internal
public final class PayloadChannel {

    private static final int MAX_NAME_LENGTH = 20;
    private static final Map<String, PayloadChannel> BY_NAME = new HashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, PayloadChannel> BY_TYPE = new ConcurrentHashMap<>();

    private final String name;
    private final FMLEventChannel channel;
    private final Direction toClient = new Direction();
    private final Direction toServer = new Direction();

    private PayloadChannel(String name) {
        this.name = name;
        this.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(name);
        this.channel.register(this);
    }

    static synchronized PayloadChannel named(String name) {
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Channel name " + name + " is longer than " + MAX_NAME_LENGTH + " characters");
        }
        return BY_NAME.computeIfAbsent(name, PayloadChannel::new);
    }

    static PayloadChannel of(CustomPacketPayload payload) {
        PayloadChannel channel = BY_TYPE.get(payload.type());
        if (channel == null) {
            throw new IllegalStateException("Payload type " + payload.type() + " is not registered");
        }
        return channel;
    }

    <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler, boolean clientbound,
            boolean serverbound) {
        if (BY_TYPE.putIfAbsent(type, this) != null) {
            throw new IllegalStateException("Payload type " + type + " is already registered");
        }
        if (clientbound) {
            toClient.add(type, codec, handler);
        }
        if (serverbound) {
            toServer.add(type, codec, handler);
        }
    }

    FMLProxyPacket packet(CustomPacketPayload payload, Side receiver) {
        ByteBuf out = Unpooled.buffer();
        direction(receiver).codec().encode(new RegistryFriendlyByteBuf(out), payload);
        // FMLProxyPacket sends payload.array(), so the array must be exactly sized
        byte[] bytes = new byte[out.readableBytes()];
        out.readBytes(bytes);
        return new FMLProxyPacket(Unpooled.wrappedBuffer(bytes), name);
    }

    void sendToServer(CustomPacketPayload payload) {
        channel.sendToServer(packet(payload, Side.SERVER));
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        receive(event.packet.payload(), Side.SERVER, ((NetHandlerPlayServer) event.handler).playerEntity);
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        receive(event.packet.payload(), Side.CLIENT, ClientPayloads.player());
    }

    @SuppressWarnings("unchecked")
    private void receive(ByteBuf data, Side side, EntityPlayer player) {
        Direction direction = direction(side);
        CustomPacketPayload payload = direction.codec().decode(new RegistryFriendlyByteBuf(data));
        if (data.isReadable()) {
            throw new DecoderException("Payload " + payload.type() + " left " + data.readableBytes() + " bytes unread");
        }
        IPayloadHandler<CustomPacketPayload> handler = (IPayloadHandler<CustomPacketPayload>) direction
                .handler(payload.type());
        Context context = new Context(payload.type(), side, player);
        context.enqueueWork(() -> handler.handle(payload, context));
    }

    private Direction direction(Side receiver) {
        return receiver == Side.CLIENT ? toClient : toServer;
    }

    private static final class Direction {

        private final List<CustomPacketPayload.Type<?>> types = new ArrayList<>();
        private final List<StreamCodec<? super RegistryFriendlyByteBuf, ? extends CustomPacketPayload>> codecs = new ArrayList<>();
        private final Map<CustomPacketPayload.Type<?>, IPayloadHandler<?>> handlers = new HashMap<>();
        private IdDispatchCodec<RegistryFriendlyByteBuf, CustomPacketPayload, CustomPacketPayload.Type<?>> codec;

        synchronized void add(CustomPacketPayload.Type<?> type,
                StreamCodec<? super RegistryFriendlyByteBuf, ? extends CustomPacketPayload> payloadCodec,
                IPayloadHandler<?> handler) {
            types.add(type);
            codecs.add(payloadCodec);
            handlers.put(type, handler);
            codec = null;
        }

        synchronized IdDispatchCodec<RegistryFriendlyByteBuf, CustomPacketPayload, CustomPacketPayload.Type<?>> codec() {
            if (codec == null) {
                IdDispatchCodec.Builder<RegistryFriendlyByteBuf, CustomPacketPayload, CustomPacketPayload.Type<?>> builder = IdDispatchCodec
                        .builder(CustomPacketPayload::type);
                for (int i = 0; i < types.size(); i++) {
                    builder.add(types.get(i), codecs.get(i));
                }
                codec = builder.build();
            }
            return codec;
        }

        synchronized IPayloadHandler<?> handler(CustomPacketPayload.Type<?> type) {
            return handlers.get(type);
        }
    }

    private static final class Context implements IPayloadContext {

        private final CustomPacketPayload.Type<?> type;
        private final Side side;
        private final EntityPlayer player;

        Context(CustomPacketPayload.Type<?> type, Side side, EntityPlayer player) {
            this.type = type;
            this.side = side;
            this.player = player;
        }

        @Override
        public EntityPlayer player() {
            return player;
        }

        @Override
        public Side side() {
            return side;
        }

        @Override
        public void reply(CustomPacketPayload payload) {
            if (side == Side.SERVER) {
                PacketDistributor.sendToPlayer((EntityPlayerMP) player, payload);
            } else {
                PacketDistributor.sendToServer(payload);
            }
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable task) {
            return enqueueWork(() -> {
                task.run();
                return null;
            });
        }

        @Override
        public <T> CompletableFuture<T> enqueueWork(Supplier<T> task) {
            CompletableFuture<T> result = new CompletableFuture<>();
            Runnable work = () -> {
                try {
                    result.complete(task.get());
                } catch (Throwable t) {
                    GTNHLib.LOG.error("Failed to handle payload {} on the {} side", type, side, t);
                    result.completeExceptionally(t);
                    disconnect("Failed to handle payload " + type);
                }
            };
            if (side == Side.CLIENT ? ClientPayloads.onMainThread() : ServerThreadUtil.isCallingFromMinecraftThread()) {
                work.run();
            } else if (side == Side.CLIENT) {
                ClientPayloads.schedule(work);
            } else {
                ServerThreadUtil.addScheduledTask(work);
            }
            return result;
        }

        private void disconnect(String reason) {
            if (side == Side.CLIENT) {
                ClientPayloads.disconnect(reason);
            } else {
                ((EntityPlayerMP) player).playerNetServerHandler.kickPlayerFromServer(reason);
            }
        }
    }
}
