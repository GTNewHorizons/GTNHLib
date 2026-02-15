package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

/**
 * This chat component is used to display a fluid amount in a formatted way. It only stores the fluid amount as a
 * number, so it doesn't know which fluid it is. If you want to display the fluid name, please use
 * {@link ChatComponentFluidName} instead.
 */
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
