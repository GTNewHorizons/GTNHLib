package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.event.HoverEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentItemName;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat_ItemTooltip extends GuiScreen {

    @ModifyExpressionValue(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/event/HoverEvent;getAction()Lnet/minecraft/event/HoverEvent$Action;",
                    ordinal = 0))
    private HoverEvent.Action gtnhlib$renderItemTooltip(HoverEvent.Action action, @Local HoverEvent hover,
            @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        if (action == HoverEvent.Action.SHOW_ITEM && hover.getValue() instanceof ChatComponentItemName itemName
                && itemName.stack != null
                && itemName.stack.getItem() != null) {
            this.renderToolTip(itemName.stack, mouseX, mouseY);
            return null;
        }
        return action;
    }
}
