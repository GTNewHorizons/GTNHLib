package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;

/** {@code null} is the empty stack. Count and damage are unbounded, so validate stacks from clients. */
public final class ItemStackStreamCodecs {

    private static final StreamCodec<ByteBuf, Item> ITEM = ByteBufCodecs.idMapper(Item.itemRegistry);

    /** NBT is sent only if the item is damageable or shares its tag, as in vanilla. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = StreamCodec.of((buf, stack) -> {
        if (isEmpty(stack)) {
            throw new EncoderException("Empty ItemStack not allowed");
        }
        Item item = stack.getItem();
        ITEM.encode(buf, item);
        VarInt.write(buf, stack.stackSize);
        VarInt.write(buf, stack.getItemDamage());
        ByteBufHelpers.writeNbt(buf, item.isDamageable() || item.getShareTag() ? stack.stackTagCompound : null);
    }, buf -> {
        Item item = ITEM.decode(buf);
        int count = VarInt.read(buf);
        int damage = VarInt.read(buf);
        NBTTagCompound tag = ByteBufHelpers.readCompoundNbt(buf, ByteBufHelpers.defaultNbtQuota());
        ItemStack stack = new ItemStack(item, count, damage);
        stack.stackTagCompound = tag;
        return stack;
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = StreamCodec
            .of((buf, stack) -> {
                buf.writeBoolean(!isEmpty(stack));
                if (!isEmpty(stack)) {
                    STREAM_CODEC.encode(buf, stack);
                }
            }, buf -> buf.readBoolean() ? STREAM_CODEC.decode(buf) : null);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> LIST_STREAM_CODEC = STREAM_CODEC
            .apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC
            .apply(ByteBufCodecs.list());

    private ItemStackStreamCodecs() {}

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null;
    }
}
