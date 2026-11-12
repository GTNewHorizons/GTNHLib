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

import com.gtnewhorizon.gtnhlib.CommonProxy;
import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.util.CommandUtils;
import com.mojang.brigadier.tree.CommandNode;

@Mixin(CommandHandler.class)
public class MixinCommandHandler {

    @Unique
    private boolean gtnhlib$isServerCommandManager() {
        // this is used to make sure that we only inject into the server command handler
        return (Object) this instanceof ServerCommandManager;
    }

    /**
     * Thermos and its forks strip the leading slash before calling this, and vanilla strips one more below. A command
     * whose name starts with a slash, such as {@code //wand}, arrives here as {@code /wand} and would be looked up as
     * {@code wand}, so it is run before vanilla touches it.
     */
    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void gtnhlib$keepSlashOnThermos(ICommandSender sender, String command,
            CallbackInfoReturnable<Integer> cir) {
        if (!gtnhlib$isServerCommandManager() || !CommonProxy.isThermosServer()) return;

        String trimmed = command.trim();
        if (!trimmed.startsWith("/")) return;

        int space = trimmed.indexOf(' ');
        String name = space == -1 ? trimmed : trimmed.substring(0, space);
        CommandNode<ICommandSender> node = BrigadierApi.getCommandDispatcher().getRoot().getChild(name);
        if (node == null) return;

        if (!node.canUse(sender)) {
            cir.setReturnValue(CommandUtils.error(sender, "commands.generic.permission"));
            return;
        }
        cir.setReturnValue(BrigadierApi.executeCommand(sender, trimmed));
    }

    @Inject(
            method = "executeCommand",
            at = @At(value = "NEW", target = "()Lnet/minecraft/command/CommandNotFoundException;"),
            cancellable = true)
    private void gtnhlib$fallbackToBrigadier(ICommandSender sender, String command,
            CallbackInfoReturnable<Integer> cir) {
        if (!gtnhlib$isServerCommandManager()) return;

        // an empty console line or a lone slash is not a command, so do not report it as one
        if (command.trim().isEmpty()) {
            cir.setReturnValue(0);
            return;
        }

        int value = BrigadierApi.executeCommand(sender, command);
        cir.setReturnValue(value);
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
