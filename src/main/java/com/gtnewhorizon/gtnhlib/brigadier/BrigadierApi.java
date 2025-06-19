package com.gtnewhorizon.gtnhlib.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.ICommandSender;

public class BrigadierApi {

    private static final CommandDispatcher<ICommandSender> DISPATCHER = new CommandDispatcher<>();

    public static CommandDispatcher<ICommandSender> getCommandDispatcher() {
        return DISPATCHER;
    }

}
