package com.gtnewhorizon.gtnhlib.api.transformation;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.gtnhlib.transformation.TransformationController;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Describes a player transforming into a new shape and/or size (e.g. transforming into a wolf.)
 * <p>
 * Make sure to register all instances with {@link TransformationController#registerTransformation(ITransformation)}, or
 * you won't be able to use them. Registration should be done in the common proxy of your mod.
 */
@SuppressWarnings("unused")
public interface ITransformation {

    float DEFAULT_WIDTH = 0.6f;
    float DEFAULT_HEIGHT = 1.8f;
    float DEFAULT_EYE_HEIGHT = 1.62f;

    /**
     * Unique ID for this transformation. It's recommended to use the form:
     * <p>
     * modID:form_name
     *
     * @return A globally unique transformation ID string.
     */
    String getId();

    /**
     * Called whenever the player is changing to this new form.
     *
     * @param player The transforming player
     */
    default void onTransform(EntityPlayer player) {}

    /**
     * Called whenever the player is reverting back to a standard player form.
     *
     * @param player The untransforming player
     */
    default void onUntransform(EntityPlayer player) {}

    /**
     * Called every tick while the player is transformed into this form.
     *
     * @param player The transformed player
     */
    default void onTick(EntityPlayer player) {}

    /**
     * Controls whether the form should persist on death.
     *
     * @return true to persist the form on death (default), false to revert back to the regular player on death
     */
    default boolean retainFormOnDeath() {
        return true;
    }

    /**
     * The width of the new form, in block-widths.
     *
     * @return A float with the width of the new form. Defaults to {@value #DEFAULT_WIDTH}
     */
    default float getWidth() {
        return DEFAULT_WIDTH;
    }

    /**
     * The height of the new form, in block-heights.
     *
     * @return A float with the height of the new form. Defaults to {@value #DEFAULT_HEIGHT}
     */
    default float getHeight() {
        return DEFAULT_HEIGHT;
    }

    /**
     * How far off the ground the new form's eyes are, in block-heights.
     *
     * @return A float with the eye height of the new form. Defaults to {@value #DEFAULT_EYE_HEIGHT}
     */
    default float getEyeHeight() {
        return DEFAULT_EYE_HEIGHT;
    }

    /**
     * Whether to render the hands of the new form in the player's first-person view.
     *
     * @return true to render the character's hands (defaults to false)
     */
    default boolean renderHands() {
        return false;
    }

    /**
     * The new entity that the player should turn in to.
     *
     * @param player The transforming player
     * @return The new entity to transform in to
     */
    @SideOnly(Side.CLIENT)
    EntityLivingBase getRenderEntity(EntityPlayer player);

    /**
     * Saves any custom NBT data for this transformation.
     *
     * @param tag the tag to add information to
     */
    default void saveNBTData(final NBTTagCompound tag) {}

    /**
     * Loads any custom NBT data for this transformation.
     *
     * @param tag data loaded for the transformation to consume
     */
    default void loadNBTData(final NBTTagCompound tag) {}
}
