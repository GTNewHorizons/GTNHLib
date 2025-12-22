package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings("unused")
public class ChatComponentNumber extends AbstractChatComponentNumber {

    public ChatComponentNumber() {
        super();
    }

    public ChatComponentNumber(Number number) {
        super(number);
    }

    @Override
    protected String formatNumber(Number value) {
        return NumberFormatUtil.formatNumber(value);
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentNumber";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentNumber(number);
    }

}
