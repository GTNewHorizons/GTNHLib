package com.gtnewhorizon.gtnhlib.datacomponent.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.datacomponent.components.BooleanComponent;
import com.gtnewhorizon.gtnhlib.datacomponent.components.IntegerComponent;
import com.gtnewhorizon.gtnhlib.datacomponent.components.StringComponent;
import com.gtnewhorizon.gtnhlib.datacomponent.registry.DataComponentRegistry;

public class DataComponents {

    public static void init() {
        DataComponentRegistry.registerComponent(DAMAGED);
        DataComponentRegistry.registerComponent(BROKEN);
        DataComponentRegistry.registerComponent(CARRIED);
        DataComponentRegistry.registerComponent(EXTENDED_VIEW);
        DataComponentRegistry.registerComponent(RARITY);
        DataComponentRegistry.registerComponent(DAMAGE);
        DataComponentRegistry.registerComponent(MAX_DAMAGE);
        DataComponentRegistry.registerComponent(STACK_SIZE);
        DataComponentRegistry.registerComponent(MAX_STACK_SIZE);
        DataComponentRegistry.registerComponent(REPAIRABLE);
        DataComponentRegistry.registerComponent(REPAIR_COST);
        DataComponentRegistry.registerComponent(UNBREAKABLE);
    }

    public static final BooleanComponent DAMAGED = new BooleanComponent() {

        @Override
        public String getName() {
            return "damaged";
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            return stack.isItemDamaged();
        }
    };

    public static final BooleanComponent BROKEN = new BooleanComponent() {

        @Override
        public String getName() {
            return "broken";
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            if (stack == null) return false;
            Item item = stack.getItem();
            if (item == null || !item.isDamageable()) return false;
            return stack.getItemDamage() >= (stack.getMaxDamage() - 1);
        }
    };

    public static final BooleanComponent CARRIED = new BooleanComponent() {

        @Override
        public String getName() {
            return "carried";
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            if (stack == null) return false;
            if (Minecraft.getMinecraft().currentScreen instanceof GuiContainer) {
                return Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == stack;
            }
            return false;
        }
    };

    public static final BooleanComponent EXTENDED_VIEW = new BooleanComponent() {

        @Override
        public String getName() {
            return "extended_view";
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            return Minecraft.getMinecraft().currentScreen != null && GuiScreen.isShiftKeyDown();
        }
    };

    public static final IntegerComponent DAMAGE = new IntegerComponent() {

        @Override
        public String getName() {
            return "damage";
        }

        @Override
        public Integer getValue(ItemStack stack) {
            return stack.getItemDamage();
        }
    };

    public static final IntegerComponent MAX_DAMAGE = new IntegerComponent() {

        @Override
        public String getName() {
            return "max_damage";
        }

        @Override
        public Integer getValue(ItemStack stack) {
            return stack.getMaxDamage();
        }
    };

    public static final IntegerComponent STACK_SIZE = new IntegerComponent() {

        @Override
        public String getName() {
            return "stack_size";
        }

        @Override
        public Integer getValue(ItemStack stack) {
            return stack.stackSize;
        }
    };

    public static final IntegerComponent MAX_STACK_SIZE = new IntegerComponent() {

        @Override
        public String getName() {
            return "max_stack_size";
        }

        @Override
        public Integer getValue(ItemStack stack) {
            return stack.getMaxStackSize();
        }
    };

    public static final StringComponent RARITY = new StringComponent() {

        @Override
        public String getName() {
            return "rarity";
        }

        @Override
        public String getValue(ItemStack stack) {
            return stack.getRarity().rarityName;
        }
    };

    public static final IntegerComponent REPAIR_COST = new IntegerComponent() {

        @Override
        public String getName() {
            return "repair_cost";
        }

        @Override
        public Integer getValue(ItemStack stack) {
            return stack.getRepairCost();
        }
    };

    public static final BooleanComponent REPAIRABLE = new BooleanComponent() {

        @Override
        public String getName() {
            return "repairable";
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            return stack != null && stack.getItem() != null && stack.getItem().isRepairable();
        }
    };

    public static final BooleanComponent UNBREAKABLE = new BooleanComponent() {

        @Override
        public String getName() {
            return "unbreakable";
        }

        @Override
        public Boolean getValue(ItemStack stack) {
            return stack != null && stack.hasTagCompound() && stack.stackTagCompound.getBoolean("Unbreakable");
        }
    };

}
