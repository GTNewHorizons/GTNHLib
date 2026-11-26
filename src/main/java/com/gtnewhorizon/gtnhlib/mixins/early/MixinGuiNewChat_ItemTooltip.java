package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentItemName;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat_ItemTooltip {

    @ModifyExpressionValue(
            method = "func_146237_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ChatStyle;createShallowCopy()Lnet/minecraft/util/ChatStyle;"))
    private ChatStyle gtnhlib$preserveItemTooltip(ChatStyle style, @Local(ordinal = 1) IChatComponent component) {
        HoverEvent hover = style.getChatHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_ITEM
                && component instanceof ChatComponentItemName itemName
                && itemName.stack != null
                && itemName.stack.getItem() != null) {
            style.setChatHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentItemName(itemName.stack)));
        }
        return style;
    }
}
