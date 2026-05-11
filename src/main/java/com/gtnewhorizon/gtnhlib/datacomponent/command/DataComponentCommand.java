package com.gtnewhorizon.gtnhlib.datacomponent.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentType;
import com.gtnewhorizon.gtnhlib.datacomponent.registry.DataComponentRegistry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class DataComponentCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "datacomponent";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/datacomponent <get|set> [component name] [component value]";
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<>();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer player)) return;

        ItemStack stack = player.inventory.getCurrentItem();
        if (stack == null) {
            sendErrorToPlayer(sender, "You must be holding an item to use this command.");
            return;
        }

        String action = DataComponentType.getIndexSafe(args, 0);
        String name = DataComponentType.getIndexSafe(args, 1);
        String value = DataComponentType.getIndexSafe(args, 2);

        if (action == null || ("set".equals(action) && (name == null || value == null))) {
            sendErrorToPlayer(sender, getCommandUsage(sender));
            return;
        }

        Map<String, DataComponentType<?>> components = new Object2ObjectOpenHashMap<>();
        DataComponentRegistry.getComponents(stack, components);

        if ("get".equals(action)) {
            if (name != null) {
                @SuppressWarnings("unchecked")
                DataComponentType<Object> comp = (DataComponentType<Object>) components.get(name);

                if (comp != null) {
                    Object v = comp.getValue(stack);
                    sendChatToPlayer(
                            sender,
                            EnumChatFormatting.GOLD + comp.getName()
                                    + ": "
                                    + EnumChatFormatting.WHITE
                                    + comp.stringify(v));
                    return;
                }
                sendErrorToPlayer(sender, "Component '" + name + "' not found on this item.");
            } else {
                sendChatToPlayer(player, EnumChatFormatting.AQUA + "Components for [" + stack.getDisplayName() + "]:");
                if (components.isEmpty()) {
                    sendChatToPlayer(player, EnumChatFormatting.GRAY + "None");
                    return;
                }

                for (var entry : components.entrySet()) {
                    @SuppressWarnings("unchecked")
                    DataComponentType<Object> comp = (DataComponentType<Object>) entry.getValue();
                    Object v = comp.getValue(stack);
                    sendChatToPlayer(
                            player,
                            "- " + EnumChatFormatting.YELLOW
                                    + comp.getName()
                                    + ": "
                                    + EnumChatFormatting.WHITE
                                    + comp.stringify(v));
                }
            }
        } else if ("set".equals(action)) {

            @SuppressWarnings("unchecked")
            DataComponentType<Object> comp = (DataComponentType<Object>) components.get(name);

            if (comp != null) {
                try {
                    Object v = comp.parse(value);
                    comp.setValue(stack, v);

                    sendChatToPlayer(sender, EnumChatFormatting.GREEN + "Set " + name + " to " + value);
                } catch (Throwable t) {
                    GTNHLib.LOG.error("Error setting component {}", name, t);
                    sendErrorToPlayer(player, "Error setting component: " + t.getMessage());
                }
                return;
            }
            sendErrorToPlayer(sender, "Component '" + name + "' not found on this item.");
        }
    }

    private static void sendErrorToPlayer(ICommandSender sender, String aChatMessage) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + aChatMessage));
    }

    private static void sendChatToPlayer(ICommandSender sender, String aChatMessage) {
        sender.addChatMessage(new ChatComponentText(aChatMessage));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
