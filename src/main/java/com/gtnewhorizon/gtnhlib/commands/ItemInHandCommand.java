package com.gtnewhorizon.gtnhlib.commands;

import static net.minecraft.util.EnumChatFormatting.*;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;

public class ItemInHandCommand extends GTNHClientCommand {

    @Override
    public String getCommandName() {
        return "iteminhand";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("iih");
    }

    @Override
    public void processCommand(ICommandSender iCommandSender, String[] args) {
        // spotless:off
        if (iCommandSender instanceof EntityClientPlayerMP player) {
            ItemStack itemStack = player.getCurrentEquippedItem();
            if (itemStack == null) {
                addChatMessage("Hand is empty!");
                return;
            }
            GameRegistry.UniqueIdentifier UID = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());
            addChatMessage(GREEN.toString() + STRIKETHROUGH + "--------------------" + RESET + " Item info " + GREEN + STRIKETHROUGH + "--------------------");
            addChatMessage(String.format(GREEN + "Unloc.Name:" + RESET + " [%s]", itemStack.getUnlocalizedName()));
            if (UID != null) addChatMessage(String.format(GREEN + "ItemName:" + RESET + " [%s]", UID));
            addChatMessage(String.format(GREEN + "ItemMeta:" + RESET + " [%s]", itemStack.getItemDamage()));
            printFluidContent(itemStack);
            addChatMessage(String.format(GREEN + "ClassName:" + RESET + " [%s]", itemStack.getItem().getClass()));
            printMTHand(itemStack);
            addChatMessage(GREEN.toString() + STRIKETHROUGH + "--------------------------------------------------");
        }
        // spotless:on
    }

    private void printFluidContent(ItemStack itemStack) {
        if (itemStack.getItem() instanceof IFluidContainerItem tFluidContainer) {
            FluidStack fluidStack = tFluidContainer.getFluid(itemStack);
            if (fluidStack != null) {
                String s = String.format(
                        "FluidID: [%d], UnlocName: [%s], Name: [%s]",
                        fluidStack.getFluid().getID(),
                        fluidStack.getFluid().getUnlocalizedName(),
                        fluidStack.getFluid().getName());
                addChatMessage(String.format(GREEN + "FluidContainer:" + RESET + " [%s]", s));
            }
        }
    }

    private void printMTHand(ItemStack itemStack) {
        StringBuilder result = new StringBuilder();
        result.append('<');
        result.append(Item.itemRegistry.getNameForObject(itemStack.getItem()));
        if (itemStack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            result.append(":*");
        } else if (itemStack.getItemDamage() > 0) {
            result.append(':').append(itemStack.getItemDamage());
        }
        result.append('>');
        if (itemStack.getTagCompound() != null) {
            result.append(".withTag(");
            result.append(itemStack.getTagCompound().toString());
            result.append(")");
        }
        String msg = result.toString();
        addChatMessage(GREEN + "mt hand: " + RESET + msg);
        copyToClipboard(msg);
    }

}
