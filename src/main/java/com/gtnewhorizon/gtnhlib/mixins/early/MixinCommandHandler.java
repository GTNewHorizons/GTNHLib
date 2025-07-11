package com.gtnewhorizon.gtnhlib.mixins.early;

import java.util.Collection;
import java.util.List;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;

@Mixin(CommandHandler.class)
public class MixinCommandHandler {

    @Unique
    private boolean gtnhlib$isServerCommandManager() {
        // this is used to make sure that we only inject into the server command handler
        return (Object) this instanceof ServerCommandManager;
    }

    @Inject(
            method = "executeCommand",
            at = @At(value = "NEW", target = "()Lnet/minecraft/command/CommandNotFoundException;"),
            cancellable = true)
    private void gtnhlib$fallbackToBrigadier(ICommandSender sender, String command,
            CallbackInfoReturnable<Integer> cir) {
        if (gtnhlib$isServerCommandManager()) {
            int value = BrigadierApi.executeCommand(sender, command);
            cir.setReturnValue(value);
        }
    }

    @Inject(
            method = "getPossibleCommands(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true)
    private void gtnhlib$gatherBrigadierSuggestion(ICommandSender sender, String command,
            CallbackInfoReturnable<List<String>> cir) {
        if (gtnhlib$isServerCommandManager()) {
            List<String> returnValue = cir.getReturnValue();
            List<String> possibleCommands = BrigadierApi.getPossibleCommands(sender, command);
            if (returnValue == null) {
                cir.setReturnValue(possibleCommands);
            } else {
                returnValue.addAll(possibleCommands);
            }
        }
    }

    @Inject(method = "getPossibleCommands(Lnet/minecraft/command/ICommandSender;)Ljava/util/List;", at = @At("RETURN"))
    private void gtnhlib$gatherBrigadierCommands(ICommandSender sender, CallbackInfoReturnable<List<ICommand>> cir) {
        if (gtnhlib$isServerCommandManager()) {
            Collection<ICommand> possibleCommands = BrigadierApi.getPossibleCommands(sender);
            cir.getReturnValue().addAll(possibleCommands);
        }
    }

}
