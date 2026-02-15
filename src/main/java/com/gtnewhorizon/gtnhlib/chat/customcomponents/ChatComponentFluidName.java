package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;

public class ChatComponentFluidName extends AbstractChatComponentBuffer<ChatComponentFluidName> {

    protected FluidStack stack = null;

    public ChatComponentFluidName() {}

    public ChatComponentFluidName(FluidStack stack) {
        this.stack = stack;
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentFluidName";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentFluidName(stack == null ? null : stack.copy());
    }

    @Override
    public String getUnformattedTextForChat() {
        return stack == null ? "" : stack.getLocalizedName();
    }

    @Override
    public void encode(PacketBuffer buffer) {
        if (stack == null) {
            buffer.writeBoolean(true);
        } else {
            buffer.writeBoolean(false);
            NBTTagCompound fluidStackTag = stack.writeToNBT(new NBTTagCompound());
            try {
                buffer.writeNBTTagCompoundToBuffer(fluidStackTag);
            } catch (IOException ignored) {

            }
        }
    }

    @Override
    public void decode(PacketBuffer buffer) {
        if (buffer.readBoolean()) {
            this.stack = null;
        }
        try {
            this.stack = FluidStack.loadFluidStackFromNBT(buffer.readNBTTagCompoundFromBuffer());
        } catch (IOException e) {
            this.stack = null;
        }
    }
}
