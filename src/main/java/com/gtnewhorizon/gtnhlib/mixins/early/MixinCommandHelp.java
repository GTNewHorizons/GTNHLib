package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.command.CommandHelp;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.mojang.brigadier.tree.CommandNode;

@Mixin(CommandHelp.class)
public class MixinCommandHelp {

    @Inject(
            method = "processCommand",
            at = @At(value = "NEW", target = "()Lnet/minecraft/command/CommandNotFoundException;"),
            cancellable = true)
    private void gtnhlib$fallbackToBrigadier(ICommandSender sender, String[] args, CallbackInfo ci) {
        CommandNode<ICommandSender> node = BrigadierApi.getCommandDispatcher().getRoot().getChild(args[0]);
        if (node != null) {
            throw new WrongUsageException(node.getUsageText());
        }
    }

}
