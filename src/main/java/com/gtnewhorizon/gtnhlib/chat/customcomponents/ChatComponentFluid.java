package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings("unused")
public final class ChatComponentFluid extends AbstractChatComponentNumber {

    public ChatComponentFluid() {
        super();
    }

    public ChatComponentFluid(Number number) {
        super(number);
    }

    public ChatComponentFluid(FluidStack number) {
        super(number.amount);
    }

    @Override
    protected String formatNumber(Number value) {
        return NumberFormatUtil.formatFluid(value);
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentFluid";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentFluid(number);
    }

}
