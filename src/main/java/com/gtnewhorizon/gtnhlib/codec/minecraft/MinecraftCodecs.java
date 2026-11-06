package com.gtnewhorizon.gtnhlib.codec.minecraft;

import java.util.Optional;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.codec.stream.ByteBufCodecs;
import com.gtnewhorizon.gtnhlib.codec.stream.StreamCodec;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public final class MinecraftCodecs {

    public static final Codec<Item> ITEM = Codec.STRING.flatXmap(name -> {
        Object entry = Item.itemRegistry.getObject(name);
        return entry instanceof Item item ? DataResult.success(item) : DataResult.error(() -> "Unknown item: " + name);
    }, item -> {
        String name = Item.itemRegistry.getNameForObject(item);
        return name != null ? DataResult.success(name) : DataResult.error(() -> item + " is not registered");
    });

    public static final Codec<NBTTagCompound> COMPOUND_TAG = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        NBTBase tag = dynamic.convert(NbtOps.INSTANCE).getValue();
        if (!(tag instanceof NBTTagCompound compound)) {
            return DataResult.error(() -> "Not a compound tag: " + tag);
        }
        return DataResult.success(tag == dynamic.getValue() ? (NBTTagCompound) compound.copy() : compound);
    }, tag -> new Dynamic<>(NbtOps.INSTANCE, tag.copy()));

    /** Damage is the raw field, as vanilla saves it. Count and damage are unbounded; validate client stacks. */
    public static final Codec<ItemStack> ITEM_STACK = RecordCodecBuilder.<ItemStack>create(
            instance -> instance
                    .group(
                            ITEM.fieldOf("id").forGetter(ItemStack::getItem),
                            Codec.INT.optionalFieldOf("count", 1).forGetter(stack -> stack.stackSize),
                            Codec.INT.optionalFieldOf("damage", 0).forGetter(stack -> stack.itemDamage),
                            COMPOUND_TAG.optionalFieldOf("tag")
                                    .forGetter(stack -> Optional.ofNullable(stack.stackTagCompound)))
                    .apply(instance, (item, count, damage, tag) -> {
                        ItemStack stack = new ItemStack(item, count, damage);
                        stack.stackTagCompound = tag.orElse(null);
                        return stack;
                    }))
            .validate(
                    stack -> isEmpty(stack) ? DataResult.error(() -> "Empty ItemStack not allowed")
                            : DataResult.success(stack));

    /** Empty is {@code {}}. Optional, not null, since DFU records reject null fields. */
    public static final Codec<Optional<ItemStack>> OPTIONAL_ITEM_STACK = new Codec<Optional<ItemStack>>() {

        @Override
        public <T> DataResult<Pair<Optional<ItemStack>, T>> decode(DynamicOps<T> ops, T input) {
            boolean emptyMap = ops.getMap(input).result().map(MinecraftCodecs::isEmptyMap).orElse(false);
            return emptyMap ? DataResult.success(Pair.of(Optional.empty(), ops.empty()))
                    : ITEM_STACK.decode(ops, input).map(pair -> pair.mapFirst(Optional::of));
        }

        @Override
        public <T> DataResult<T> encode(Optional<ItemStack> input, DynamicOps<T> ops, T prefix) {
            ItemStack stack = input.orElse(null);
            return isEmpty(stack) ? ops.mergeToPrimitive(prefix, ops.emptyMap())
                    : ITEM_STACK.encode(stack, ops, prefix);
        }
    };

    /** 1.7.10 chat JSON, written as a string. Inline JSON is also accepted. */
    public static final Codec<IChatComponent> CHAT_COMPONENT = new Codec<IChatComponent>() {

        @Override
        public <T> DataResult<Pair<IChatComponent, T>> decode(DynamicOps<T> ops, T input) {
            String json = ops.getStringValue(input).result()
                    .orElseGet(() -> new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue().toString());
            return parseChat(json).map(component -> Pair.of(component, ops.empty()));
        }

        @Override
        public <T> DataResult<T> encode(IChatComponent input, DynamicOps<T> ops, T prefix) {
            return ops.mergeToPrimitive(prefix, ops.createString(IChatComponent.Serializer.func_150696_a(input)));
        }
    };
    public static final StreamCodec<ByteBuf, IChatComponent> CHAT_COMPONENT_STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(json -> parseChat(json).getOrThrow(DecoderException::new), IChatComponent.Serializer::func_150696_a);

    private MinecraftCodecs() {}

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null;
    }

    private static <T> boolean isEmptyMap(MapLike<T> map) {
        return !map.entries().findAny().isPresent();
    }

    private static DataResult<IChatComponent> parseChat(String json) {
        IChatComponent component;
        try {
            component = IChatComponent.Serializer.func_150699_a(json);
        } catch (JsonParseException e) {
            return DataResult.error(() -> "Invalid chat component " + json + ": " + e.getMessage());
        }
        return component != null ? DataResult.success(component)
                : DataResult.error(() -> "Invalid chat component " + json);
    }
}
