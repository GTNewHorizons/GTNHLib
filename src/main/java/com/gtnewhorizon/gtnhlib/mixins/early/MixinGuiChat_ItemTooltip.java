package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.nbt.NBTBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.chat.ItemHoverNbt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(GuiChat.class)
public class MixinGuiChat_ItemTooltip {

    @WrapOperation(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/JsonToNBT;func_150315_a(Ljava/lang/String;)Lnet/minecraft/nbt/NBTBase;"))
    private NBTBase gtnhlib$parseItemHoverNbt(String text, Operation<NBTBase> original) {
        return ItemHoverNbt.isEncoded(text) ? ItemHoverNbt.decode(text) : original.call(text);
    }
}
