package com.gtnewhorizon.gtnhlib.client.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;

public class LivingEquipmentChangeEvent extends LivingEvent {
    /*
     * Backported from github.com/MinecraftForge/MinecraftForge/pull/3411
     */
    private final int slot;
    private final ItemStack from;
    private final ItemStack to;

    /**
     * {@link LivingEquipmentChangeEvent} is fired when the Equipment of an Entity changes. <br>
     * This event is fired whenever changes in Equipment are detected in {@link EntityLivingBase#onUpdate()}. <br>
     * This also includes entities joining the World, as well as being cloned. <br>
     * This event is fired on server-side only. <br>
     * <br>
     * {@link #slot} contains the index of the affected inventory slot. <br>
     * {@link #from} contains the {@link ItemStack} that was equipped previously. <br>
     * {@link #to} contains the {@link ItemStack} that is equipped now. <br>
     * <br>
     * This event is not {@link cpw.mods.fml.common.eventhandler.Cancelable}. <br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}.
     **/

    public LivingEquipmentChangeEvent(EntityLivingBase entity, int slot, ItemStack from, ItemStack to)
    {
        super(entity);
        this.slot = slot;
        this.from = from;
        this.to = to;
    }

    public int getSlot() { return this.slot; }
    public ItemStack getFrom() { return this.from; }
    public ItemStack getTo() { return this.to; }
}
