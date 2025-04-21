package com.gtnewhorizon.gtnhlib.client.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.StatCollector;
import net.minecraft.util.WeightedRandom;

import org.apache.commons.lang3.StringUtils;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * Helper class for providing random, localized Strings to fields annotated with {@link LoreHolder}.
 *
 * @since 0.5.21
 * @author glowredman
 */
public final class LoreHandler implements IResourceManagerReloadListener {

    private static final Random RANDOM = new Random();

    public static void postInit() {
        LoreHolderDiscoverer.register();
        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(new LoreHandler());
    }

    private LoreHandler() {}

    @Override
    public void onResourceManagerReload(IResourceManager p_110549_1_) {
        LoreHolderDiscoverer.LORE_HOLDERS.forEach((field, keyPrefix) -> {
            try {
                field.setValue(null, getRandomLine(keyPrefix));
            } catch (Exception e) {
                GTNHLib.LOG.warn(
                        "Unable to update LoreHolder in " + field.javaField.getDeclaringClass()
                                + " (Field: "
                                + field.javaField.getName()
                                + ")",
                        e);
            }
        });
    }

    /**
     * @deprecated As of version 0.6.2, fields are discovered automatically.
     * @param clazz The class containing the field(s) to be updated when the resources are reloaded
     * @since 0.5.21
     */
    @Deprecated
    public static void registerLoreHolder(Class<?> clazz) {}

    private static String getRandomLine(String keyPrefix) {
        List<WeightedRandom.Item> lines = getAllLines(keyPrefix);

        if (lines.isEmpty()) {
            return null;
        }

        try {
            return ((WeightedText) WeightedRandom.getRandomItem(RANDOM, lines)).text;
        } catch (IllegalArgumentException e) {
            GTNHLib.LOG.warn("The total weight of all lines for \"" + keyPrefix + "\" exceeds " + Integer.MAX_VALUE, e);
        } catch (Exception e) {
            GTNHLib.LOG
                    .error("An unexpected Exception occurred while choosing a random lore for \"" + keyPrefix + '"', e);
        }

        return null;
    }

    private static List<WeightedRandom.Item> getAllLines(String keyPrefix) {
        List<WeightedRandom.Item> allLines = new ArrayList<>();

        for (int i = 0; true; i++) {
            String unlocalizedLine = keyPrefix + i;
            String localizedLine = StatCollector.translateToLocal(unlocalizedLine);
            if (unlocalizedLine.equals(localizedLine)) {
                break;
            } else {
                if (!StringUtils.isBlank(localizedLine)) {
                    allLines.add(new WeightedText(localizedLine));
                }
            }
        }

        return allLines;
    }

    private static class WeightedText extends WeightedRandom.Item {

        private String text;

        private WeightedText(String weightedText) {
            super(0);
            this.extractWeightAndText(weightedText);
        }

        private void extractWeightAndText(String weightedText) {
            int endOfWeight = weightedText.indexOf(':');

            // no ':' was found or the ':' was escaped using '\'
            // -> lore line has no weight specified
            if (endOfWeight < 1) {
                this.itemWeight = 1;
                this.text = weightedText;
                return;
            }

            if (weightedText.charAt(endOfWeight - 1) == '\\') {
                this.itemWeight = 1;
                this.text = weightedText.substring(0, endOfWeight - 1) + weightedText.substring(endOfWeight);
                return;
            }

            // if a ':' was found, attempt to parse everything before it as int
            String weightString = weightedText.substring(0, endOfWeight);
            try {
                int weight = Integer.parseInt(weightString);

                if (weight < 0) {
                    GTNHLib.LOG.warn(
                            "\"{}\" has a negative weight ({}). This is not allowed, a weight of 1 will be used instead.",
                            weightedText,
                            weight);
                    this.itemWeight = 1;
                } else {
                    this.itemWeight = weight;
                }

                this.text = weightedText.substring(endOfWeight + 1);
                return;
            } catch (NumberFormatException e) {
                GTNHLib.LOG.warn(
                        "Could not parse \"" + weightString
                                + "\" as Integer. If it is not supposed to be a weight, escape the ':' delimiter using '\\'.",
                        e);
            } catch (Exception e) {
                GTNHLib.LOG.error(
                        "An unexpected Exception occurred while extracting weight and text from lore \"" + weightedText
                                + '"',
                        e);
            }

            // fallback
            this.itemWeight = 1;
            this.text = weightedText;
        }
    }
}
