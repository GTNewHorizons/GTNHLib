package com.gtnewhorizon.gtnhlib;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.config.Config;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.GuiUtils;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

@Config(modid = GTNHLib.MODID, category = "example", filename = "gtnhlib-example")
@Config.Order(0)
@Config.Entry(ExampleConfig.SortedCategory.class)
public class ExampleConfig {

    @Config.Comment("The text should be green")
    @Config.Entry(GreenString.class)
    public static String greenField = "I should be Green";

    @Config.Entry(ModIDEntry.class)
    public static String selectionList = "Mod ID";

    @Config.Comment("This category button should have RED text.")
    @Config.Entry(RedCategory.class)
    @Config.Order(0)
    // the ordering happens right after SortedCategory so it will be on top
    public static testCategory redCategory = new testCategory();

    public static class testCategory {

        @Config.Comment("This category button should have RED text.")
        @Config.Entry(RedCategory.class)
        public testCategory2 redCategory = new testCategory2();

        public static class testCategory2 {

            @Config.Comment("The boolean text should be yellow")
            @Config.Entry(YellowBoolean.class)
            public boolean yellowBoolean = true;

            @Config.Order(0)
            public boolean enable;

            @Config.Order(2)
            public int speed;

            @Config.Order(1)
            public int size;

            public int noOrder;

        }

        @Config.Comment("The boolean text should be yellow")
        @Config.Entry(YellowBoolean.class)
        @Config.Order(0)
        public boolean yellowBoolean = true;
    }

    public static class SortedCategory extends GuiConfigEntries.CategoryEntry {

        public SortedCategory(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.btnSelectCategory.displayString = EnumChatFormatting.RED + this.btnSelectCategory.displayString;
        }

        protected GuiScreen buildChildScreen() {

            List<IConfigElement> children = new ArrayList<>(this.configElement.getChildElements());

            children.sort(Comparator.comparing(IConfigElement::getName, String.CASE_INSENSITIVE_ORDER));

            return new GuiConfig(
                    this.owningScreen,
                    children,
                    this.owningScreen.modID,
                    owningScreen.allRequireWorldRestart || this.configElement.requiresWorldRestart(),
                    owningScreen.allRequireMcRestart || this.configElement.requiresMcRestart(),
                    this.owningScreen.title,
                    ((this.owningScreen.titleLine2 == null ? "" : this.owningScreen.titleLine2) + " > " + this.name));
        }
    }

    public static class ModIDEntry extends GuiConfigEntries.SelectValueEntry {

        public ModIDEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement prop) {
            super(owningScreen, owningEntryList, prop, getSelectableValues());
            if (this.selectableValues.size() == 0) this.btnValue.enabled = false;
        }

        private static Map<Object, String> getSelectableValues() {
            Map<Object, String> selectableValues = new TreeMap<Object, String>();

            for (ModContainer mod : Loader.instance().getActiveModList())
                // only add mods to the list that have a non-immutable ModContainer
                if (!mod.isImmutable() && mod.getMod() != null) selectableValues.put(mod.getModId(), mod.getName());

            return selectableValues;
        }
    }

    public static class GreenString extends GuiConfigEntries.StringEntry {

        public GreenString(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            textFieldValue.setTextColor(65280);
        }
    }

    public static class RedCategory extends GuiConfigEntries.CategoryEntry {

        public RedCategory(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.btnSelectCategory.displayString = EnumChatFormatting.RED + this.btnSelectCategory.displayString;
        }
    }

    // Mimic BooleanEntry because it has a private constructor
    public static class YellowBoolean extends GuiConfigEntries.ButtonEntry {

        protected final boolean beforeValue;
        protected boolean currentValue;

        public YellowBoolean(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
            this.beforeValue = Boolean.valueOf(configElement.get().toString());
            this.currentValue = beforeValue;
            this.btnValue.enabled = enabled();
            updateValueButtonText();
        }

        @Override
        public void updateValueButtonText() {
            this.btnValue.displayString = EnumChatFormatting.YELLOW + I18n.format(String.valueOf(currentValue));
            btnValue.packedFGColour = currentValue ? GuiUtils.getColorCode('2', true)
                    : GuiUtils.getColorCode('4', true);
        }

        @Override
        public void valueButtonPressed(int slotIndex) {
            if (enabled()) currentValue = !currentValue;
        }

        @Override
        public boolean isDefault() {
            return currentValue == Boolean.valueOf(configElement.getDefault().toString());
        }

        @Override
        public void setToDefault() {
            if (enabled()) {
                currentValue = Boolean.valueOf(configElement.getDefault().toString());
                updateValueButtonText();
            }
        }

        @Override
        public boolean isChanged() {
            return currentValue != beforeValue;
        }

        @Override
        public void undoChanges() {
            if (enabled()) {
                currentValue = beforeValue;
                updateValueButtonText();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean saveConfigElement() {
            if (enabled() && isChanged()) {
                configElement.set(currentValue);
                return configElement.requiresMcRestart();
            }
            return false;
        }

        @Override
        public Boolean getCurrentValue() {
            return currentValue;
        }

        @Override
        public Boolean[] getCurrentValues() {
            return new Boolean[] { getCurrentValue() };
        }
    }
}
