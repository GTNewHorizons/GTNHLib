package com.gtnewhorizon.gtnhlib.modern;

import java.util.Locale;
import java.util.function.ToIntFunction;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;

/// This is an adhoc registry that maps 1.7.10 potions and enchantments to their modern (1.13+) namespaced ids. Vanilla
/// entries use the modern ids directly, everything else gets a default id derived from its owning mod and its name.
/// Once a proper registry exists, this will be deprecated in favour of that. For now, this should be treated as a
/// temporary API.
@Internal
public class ModernIds {

    private static final IdTable<Potion> POTIONS = new IdTable<>(potion -> potion.id);
    private static final IdTable<Enchantment> ENCHANTMENTS = new IdTable<>(enchantment -> enchantment.effectId);

    public static void setPotionModernId(@NotNull String id, @NotNull Potion potion) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        setPotionModernId(new ResourceLocation(id), potion);
    }

    /// Explicitly assigns an id. If another potion already had this id, that potion is moved to its default id.
    public static void setPotionModernId(@NotNull ResourceLocation id, @NotNull Potion potion) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        POTIONS.set(id, potion);
    }

    public static void setEnchantmentModernId(@NotNull String id, @NotNull Enchantment enchantment) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        setEnchantmentModernId(new ResourceLocation(id), enchantment);
    }

    /// Explicitly assigns an id. If another enchantment already had this id, that enchantment is moved to its default
    /// id.
    public static void setEnchantmentModernId(@NotNull ResourceLocation id, @NotNull Enchantment enchantment) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        ENCHANTMENTS.set(id, enchantment);
    }

    /// Returns the potion's id, assigning it a default one if it has none yet.
    @NotNull
    public static ResourceLocation getPotionId(@NotNull Potion potion) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        return POTIONS.getId(potion);
    }

    /// Returns the enchantment's id, assigning it a default one if it has none yet.
    @NotNull
    public static ResourceLocation getEnchantmentId(@NotNull Enchantment enchantment) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        return ENCHANTMENTS.getId(enchantment);
    }

    /// Only finds potions with an assigned id. Assumption: [#assignDefaultIds()] was called after all mods loaded.
    @Nullable
    public static Potion getPotion(@NotNull ResourceLocation id) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        return POTIONS.get(id);
    }

    /// Case-insensitive variant of [#getPotion(ResourceLocation)], for user input.
    @Nullable
    public static Potion getPotion(@NotNull String id) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        return getPotion(new ResourceLocation(id.toLowerCase(Locale.ROOT)));
    }

    /// Only finds enchantments with an assigned id. Assumption: [#assignDefaultIds()] was called after all mods
    /// loaded.
    @Nullable
    public static Enchantment getEnchantment(@NotNull ResourceLocation id) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        return ENCHANTMENTS.get(id);
    }

    /// Case-insensitive variant of [#getEnchantment(ResourceLocation)], for user input.
    @Nullable
    public static Enchantment getEnchantment(@NotNull String id) {
        if (!GTNHLibConfig.injectModernIdExt) {
            throw new UnsupportedOperationException(
                    "The ModernIds system is disabled: set injectModernIdExt to true in the GTNHLib config");
        }

        return getEnchantment(new ResourceLocation(id.toLowerCase(Locale.ROOT)));
    }

    /// Gives every registered potion and enchantment without an id its default id, so that they can be looked up by
    /// name. Must be called after all mods have registered their potions and enchantments.
    public static void assignDefaultIds() {
        if (!GTNHLibConfig.injectModernIdExt) return;

        for (Potion potion : Potion.potionTypes) {
            if (potion != null) getPotionId(potion);
        }

        for (Enchantment enchantment : Enchantment.enchantmentsList) {
            if (enchantment != null) getEnchantmentId(enchantment);
        }
    }

    public static void init() {
        if (!GTNHLibConfig.injectModernIdExt) return;

        setPotionModernId("minecraft:speed", Potion.moveSpeed);
        setPotionModernId("minecraft:slowness", Potion.moveSlowdown);
        setPotionModernId("minecraft:haste", Potion.digSpeed);
        setPotionModernId("minecraft:mining_fatigue", Potion.digSlowdown);
        setPotionModernId("minecraft:strength", Potion.damageBoost);
        setPotionModernId("minecraft:instant_health", Potion.heal);
        setPotionModernId("minecraft:instant_damage", Potion.harm);
        setPotionModernId("minecraft:jump_boost", Potion.jump);
        setPotionModernId("minecraft:nausea", Potion.confusion);
        setPotionModernId("minecraft:regeneration", Potion.regeneration);
        setPotionModernId("minecraft:resistance", Potion.resistance);
        setPotionModernId("minecraft:fire_resistance", Potion.fireResistance);
        setPotionModernId("minecraft:water_breathing", Potion.waterBreathing);
        setPotionModernId("minecraft:invisibility", Potion.invisibility);
        setPotionModernId("minecraft:blindness", Potion.blindness);
        setPotionModernId("minecraft:night_vision", Potion.nightVision);
        setPotionModernId("minecraft:hunger", Potion.hunger);
        setPotionModernId("minecraft:weakness", Potion.weakness);
        setPotionModernId("minecraft:poison", Potion.poison);
        setPotionModernId("minecraft:wither", Potion.wither);
        setPotionModernId("minecraft:health_boost", Potion.field_76434_w);
        setPotionModernId("minecraft:absorption", Potion.field_76444_x);
        setPotionModernId("minecraft:saturation", Potion.field_76443_y);

        setEnchantmentModernId("minecraft:protection", Enchantment.protection);
        setEnchantmentModernId("minecraft:fire_protection", Enchantment.fireProtection);
        setEnchantmentModernId("minecraft:feather_falling", Enchantment.featherFalling);
        setEnchantmentModernId("minecraft:blast_protection", Enchantment.blastProtection);
        setEnchantmentModernId("minecraft:projectile_protection", Enchantment.projectileProtection);
        setEnchantmentModernId("minecraft:respiration", Enchantment.respiration);
        setEnchantmentModernId("minecraft:aqua_affinity", Enchantment.aquaAffinity);
        setEnchantmentModernId("minecraft:thorns", Enchantment.thorns);
        setEnchantmentModernId("minecraft:sharpness", Enchantment.sharpness);
        setEnchantmentModernId("minecraft:smite", Enchantment.smite);
        setEnchantmentModernId("minecraft:bane_of_arthropods", Enchantment.baneOfArthropods);
        setEnchantmentModernId("minecraft:knockback", Enchantment.knockback);
        setEnchantmentModernId("minecraft:fire_aspect", Enchantment.fireAspect);
        setEnchantmentModernId("minecraft:looting", Enchantment.looting);
        setEnchantmentModernId("minecraft:efficiency", Enchantment.efficiency);
        setEnchantmentModernId("minecraft:silk_touch", Enchantment.silkTouch);
        setEnchantmentModernId("minecraft:unbreaking", Enchantment.unbreaking);
        setEnchantmentModernId("minecraft:fortune", Enchantment.fortune);
        setEnchantmentModernId("minecraft:power", Enchantment.power);
        setEnchantmentModernId("minecraft:punch", Enchantment.punch);
        setEnchantmentModernId("minecraft:flame", Enchantment.flame);
        setEnchantmentModernId("minecraft:infinity", Enchantment.infinity);
        setEnchantmentModernId("minecraft:luck_of_the_sea", Enchantment.field_151370_z);
        setEnchantmentModernId("minecraft:lure", Enchantment.field_151369_A);
    }

    /// Cleans a human name into a 'good enough' snake_case id. Some names (such as `XPOrb`) get mangled (into `xporb`),
    /// but for the most part this works well.
    /// ```
    /// Foo Bar -> foo_bar
    /// FooBar -> foo_bar
    /// Foo_Bar -> foo_bar
    /// Foo-Bar -> foo_bar
    /// foo bar -> foo_bar
    /// etc
    /// ```
    public static String cleanId(String human) {
        StringBuilder underscores = new StringBuilder();

        // First step: normalize separators into underscores
        for (char curr : human.toCharArray()) {
            char lastChar = underscores.length() == 0 ? 0 : underscores.charAt(underscores.length() - 1);

            if (curr == ' ' || curr == '_' || curr == '-') {
                if (lastChar != '_') {
                    underscores.append('_');
                }

                continue;
            }

            underscores.append(curr);
        }

        StringBuilder cleaned = new StringBuilder();

        char[] us_chars = underscores.toString().toCharArray();

        // Second step: convert PascalCase into snake_case, keeping any separators generated by the previous step
        for (int i = 0; i < us_chars.length; i++) {
            char curr = us_chars[i];

            if (i > 0) {
                char prev = us_chars[i - 1];

                if (Character.isAlphabetic(prev) && Character.isAlphabetic(curr)
                        && !Character.isUpperCase(prev)
                        && Character.isUpperCase(curr)) {
                    cleaned.append('_');
                }
            }

            cleaned.append(Character.toLowerCase(curr));
        }

        return cleaned.toString();
    }

    /// A thread-safe bidirectional id table. Values that don't implement [ModernIdExt] (i.e. the mixin isn't applied)
    /// cannot be given default ids.
    private static final class IdTable<T> {

        private final BiMap<ResourceLocation, T> ids = HashBiMap.create();
        private final ToIntFunction<T> numericId;

        IdTable(ToIntFunction<T> numericId) {
            this.numericId = numericId;
        }

        synchronized void set(ResourceLocation id, T value) {
            T displaced = ids.forcePut(id, value);

            if (displaced != null && displaced != value) {
                assignDefault(displaced);
            }
        }

        synchronized ResourceLocation getId(T value) {
            ResourceLocation id = ids.inverse().get(value);

            return id != null ? id : assignDefault(value);
        }

        synchronized T get(ResourceLocation id) {
            return ids.get(id);
        }

        /// Falls back to `<default>_<numeric id>` when the default id is taken, since numeric ids are unique.
        private ResourceLocation assignDefault(T value) {
            ResourceLocation id = ((ModernIdExt) value).gtnhlib$getDefaultModernId();

            if (ids.containsKey(id)) {
                String path = id.getResourcePath() + "_" + numericId.applyAsInt(value);
                id = new ResourceLocation(id.getResourceDomain(), path);
            }

            ids.forcePut(id, value);

            return id;
        }
    }
}
