package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.math.BigInteger;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings("unused")
public final class ChatComponentEnergy extends ChatComponentNumber {

    public ChatComponentEnergy() {
        super();
    }

    public ChatComponentEnergy(Number number) {
        super(number);
    }

    @Override
    protected String formatLong(long value) {
        return NumberFormatUtil.formatEnergy(value);
    }

    @Override
    protected String formatDouble(double value) {
        return NumberFormatUtil.formatEnergy(value);
    }

    @Override
    protected String formatBigInteger(BigInteger value) {
        return NumberFormatUtil.formatEnergy(value);
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentEnergy";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentEnergy(number);
    }

}
