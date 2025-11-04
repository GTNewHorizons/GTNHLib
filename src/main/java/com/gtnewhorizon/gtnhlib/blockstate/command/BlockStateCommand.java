package com.gtnewhorizon.gtnhlib.blockstate.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class BlockStateCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "blockstate";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/blockstate <get|set> [property name] [property value]";
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<>();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer player)) return;

        String action = BlockProperty.getIndexSafe(args, 0);
        String name = BlockProperty.getIndexSafe(args, 1);
        String value = BlockProperty.getIndexSafe(args, 2);

        if (action == null || "set".equals(action) && name != null && value == null) {
            sendErrorToPlayer(sender, getCommandUsage(sender));
            return;
        }

        var hit = getHitResult(player);

        if (hit == null || hit.typeOfHit != MovingObjectType.BLOCK) {
            sendErrorToPlayer(sender, "You must be looking at a block to use this command.");
            return;
        }

        Map<String, BlockProperty<?>> properties = new Object2ObjectOpenHashMap<>();
        BlockPropertyRegistry.getProperties(player.worldObj, hit.blockX, hit.blockY, hit.blockZ, properties);

        if ("get".equals(action)) {
            if (name != null) {
                @SuppressWarnings("unchecked")
                BlockProperty<Object> prop = (BlockProperty<Object>) properties.get(name);

                if (prop != null) {
                    Object v = prop.getValue(player.worldObj, hit.blockX, hit.blockY, hit.blockZ);

                    sendChatToPlayer(sender, prop.getName() + ": " + prop.stringify(v));
                    return;
                }

                sendErrorToPlayer(sender, "Property not found.");
            } else {
                sendChatToPlayer(player, "Properties:");

                if (properties.isEmpty()) {
                    sendChatToPlayer(player, "None");
                    return;
                }

                for (var e : properties.entrySet()) {
                    @SuppressWarnings("unchecked")
                    BlockProperty<Object> prop = (BlockProperty<Object>) e.getValue();

                    Object v = prop.getValue(player.worldObj, hit.blockX, hit.blockY, hit.blockZ);

                    sendChatToPlayer(player, prop.getName() + ": " + prop.stringify(v));
                }
            }
        } else {
            @SuppressWarnings("unchecked")
            BlockProperty<Object> prop = (BlockProperty<Object>) properties.get(name);

            if (prop != null) {
                try {
                    Object v = prop.parse(value);

                    prop.setValue(player.worldObj, hit.blockX, hit.blockY, hit.blockZ, v);
                } catch (Throwable t) {
                    GTNHLib.LOG.error("Error setting property {}", name, t);
                    sendErrorToPlayer(player, "Error setting property: " + t.getMessage());
                }

                return;
            }

            sendErrorToPlayer(sender, "Property not found.");
        }
    }

    private static MovingObjectPosition getHitResult(EntityPlayer player) {
        double reachDistance = player instanceof EntityPlayerMP mp ? mp.theItemInWorldManager.getBlockReachDistance()
                : Minecraft.getMinecraft().playerController.getBlockReachDistance();

        Vec3 posVec = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);

        Vec3 lookVec = player.getLook(1);

        Vec3 modifiedPosVec = posVec.addVector(
                lookVec.xCoord * reachDistance,
                lookVec.yCoord * reachDistance,
                lookVec.zCoord * reachDistance);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(posVec, modifiedPosVec, true);

        return hit != null && hit.typeOfHit != MovingObjectType.BLOCK ? null : hit;
    }

    private static void sendErrorToPlayer(ICommandSender sender, String aChatMessage) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + aChatMessage));
    }

    private static void sendChatToPlayer(ICommandSender sender, String aChatMessage) {
        sender.addChatMessage(new ChatComponentText(aChatMessage));
    }
}
