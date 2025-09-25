package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class SlotInfo {

    @Getter
    public IInventory inventory;
    @Getter
    public int slot;
    @Getter
    public ItemStack contents;
    @Getter
    public int maxStackSize;

    public SlotInfo(IInventory inventory, int slot, ItemStack contents, int maxStackSize) {
        this.inventory = inventory;
        this.slot = slot;
        this.contents = contents;
        this.maxStackSize = maxStackSize;
    }

    public void set(IInventory inv, int slot, int maxStackSize) {
        this.inventory = inv;
        this.slot = slot;
        this.contents = inv.getStackInSlot(slot);
        this.maxStackSize = maxStackSize;
    }
}
