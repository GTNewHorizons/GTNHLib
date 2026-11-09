package com.gtnewhorizon.gtnhlib.test.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.color.RGBColor;
import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.itemrendering.BlockItemTexture;
import com.gtnewhorizon.gtnhlib.itemrendering.IItemTexture;
import com.gtnewhorizon.gtnhlib.itemrendering.ItemTexture;
import com.gtnewhorizon.gtnhlib.itemrendering.ItemWithTextures;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.numberformatting.ExponentialFormat;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

public class TestItem extends Item implements ItemWithTextures {

    public static final TestItem INSTANCE = new TestItem();

    public TestItem() {
        setUnlocalizedName("testitem");
    }

    private static void n(ImmutableMap.Builder<String, Object> args, String name, @NotNull Number number) {
        args.put(name, number);
    }

    private static void n(ImmutableMap.Builder<String, Object> args, String name, @NotNull Number number,
            NumberOptionsBase<?> options) {
        args.put(name, number);
        args.put(name + "-fmt", options);
    }

    public static ImmutableMap<String, Object> getTooltipArgs() {
        NumberOptionsBase<?> compact = new CompactOptions().setDecimalPlaces(0, 3);
        NumberOptionsBase<?> compactFrac12 = new CompactOptions().setDecimalPlaces(1, 2);
        NumberOptionsBase<?> compactFrac22 = new CompactOptions().setDecimalPlaces(2, 2);
        FormatOptions frac12 = new FormatOptions().setDecimalPlaces(1, 2);
        FormatOptions frac22 = new FormatOptions().setDecimalPlaces(2, 2);

        // spotless:off
        FormatOptions alwaysExp = new FormatOptions()
            .setExponentialThreshold(0);

        FormatOptions alwaysExpFrac12 = new FormatOptions()
            .setExponentialThreshold(0)
            .setDecimalPlaces(1, 2);

        FormatOptions alwaysExpFrac22 = new FormatOptions()
            .setExponentialThreshold(0)
            .setDecimalPlaces(2, 2);

        FormatOptions alwaysExpEng = new FormatOptions()
            .setExponentialThreshold(0)
            .setExponentialFormatOverride(ExponentialFormat.ENGINEERING);

        FormatOptions alwaysExpPoT = new FormatOptions()
            .setExponentialThreshold(0)
            .setExponentialFormatOverride(ExponentialFormat.POWER_OF_TEN);

        // temp overrides

//        compact = new FormatOptions()
//            .setExponentialThreshold(0)
//            .setExponentialFormatOverride(ExponentialFormat.ENGINEERING);
//
//        compactFrac12 = new FormatOptions()
//            .setExponentialThreshold(0)
//            .setDecimalPlaces(1, 2)
//            .setExponentialFormatOverride(ExponentialFormat.ENGINEERING);
//
//        compactFrac22 = new FormatOptions()
//            .setExponentialThreshold(0)
//            .setDecimalPlaces(2, 2)
//            .setExponentialFormatOverride(ExponentialFormat.ENGINEERING);
        // spotless:on

        ImmutableMap.Builder<String, Object> args = ImmutableMap.builder();
        // non-specific vars
        args.put("existing_var", 200);
        args.put("var_bad_tier", -1);
        args.put("some_text", "some_text");
        // boolean
        args.put("true", true);
        args.put("false", false);
        // numerics
        n(args, "-1", -1);
        n(args, "0", 0);
        n(args, "1", 1);
        n(args, "1.0", 1.0, frac12);
        n(args, "1.5", 1.5);
        n(args, "2", 2);
        n(args, "5", 5);
        n(args, "1234", 1234);

        n(args, "1.001K", 1001, new CompactOptions().setDecimalPlaces(3, 3));
        n(args, "1.234K", 1234, new CompactOptions().setDecimalPlaces(3, 3));
        // 1 mil
        n(args, "1_000_000", 1_000_000);
        n(args, "1e6", 1_000_000, alwaysExp);
        n(args, "1x10^6", 1_000_000, alwaysExpPoT);
        // 1 mil + 1
        n(args, "1_000_001", 1_000_001);
        // 1.1 mil
        n(args, "1_100_000", 1_100_000);
        n(args, "1.1e6", 1_100_000, alwaysExp);

        n(args, "999.99K", 999_990, compact);

        n(args, "999.999K", 999_999, compact);

        n(args, "1M", 1_000_000, compact);
        n(args, "1.1M", 1_100_000, compact);
        n(args, "1.11M", 1_110_000, compact);
        n(args, "1.111M", 1_111_000, compact);

        n(args, "11M", 11_000_000, compactFrac12);
        n(args, "11.1M", 11_100_000, compactFrac12);
        n(args, "11.11M", 11_110_000, compactFrac12);
        n(args, "11.111M", 11_111_000, compactFrac12);

        n(args, "111M", 111_000_000, compactFrac22);
        n(args, "111.1M", 111_100_000, compactFrac22);
        n(args, "111.11M", 111_110_000, compactFrac22);
        n(args, "111.111M", 111_111_000, compactFrac22);

        n(args, "b", 1.001e3, compact);
        n(args, "a", 1.234e3, compact);
        n(args, "c", 1.23e3, compact);
        n(args, "e", 1.001e4, compact);
        n(args, "d", 1.234e6, compact);

        n(args, "1e5_sci", 100_000, alwaysExp.copy().setDecimalPlaces(1, 3));
        n(args, "1e5_eng", 100_000, alwaysExpEng.copy().setDecimalPlaces(1, 3));
        n(args, "1e5_pot", 100_000, alwaysExpPoT.copy().setDecimalPlaces(1, 3));
        return args.build();
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advancedTooltips) {
        super.addInformation(stack, player, lines, advancedTooltips);

        String file = switch (stack.stackSize) {
            case 2 -> "showcase-commands";
            case 3 -> "showcase-line-processors";
            case 4 -> "showcase-error-handling";
            case 5 -> "showcase-loc-units";
            case 6 -> "showcase-translated";
            case 7 -> "showcase-translation-file-error";
            case 8 -> "showcase-file-error";
            case 9 -> "showcase-missing-file";
            case 10 -> "showcase-plurality-presets";
            case 11 -> "showcase-old-ext-support";
            default -> "showcase-all";
        };

        ResourceLocation res = new ResourceLocation(GTNHLib.MODID, file);
        List<String> ret = new ArrayList<>();
        ret.add(TextThemingGuidelines.TOOLTIPS.getDefault() + EnumChatFormatting.ITALIC + "showcasing: " + file);
        ret.add("");
        ret.addAll(TemplatedTextLoader.TOOLTIP_DESCRIPTION.loadStandardPath(res, TestItem::getTooltipArgs));
        lines.addAll(ret);
    }

    @Override
    public IItemTexture[] getTextures(ItemStack stack) {
        return new IItemTexture[] { new BlockItemTexture(Blocks.dirt.getIcon(0, 0), RGBColor.WHITE),
                new ItemTexture(Items.diamond_pickaxe.getIconFromDamage(0), RGBColor.WHITE), };
    }
}
