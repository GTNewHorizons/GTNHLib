package com.gtnewhorizon.gtnhlib.api.transformation;

import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.transformation.TransformationRequestPacket;
import com.gtnewhorizon.gtnhlib.transformation.BabyZombieTransform;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Describes a player transforming into a new shape and/or size (e.g. transforming into a wolf.)
 * <p>
 * Make sure to register all instances with {@link TransformationRegistry#registerTransformation(ITransformation)},
 * or you won't be able to use them. Registration should be done in the common proxy of your mod.
 */
public interface ITransformation {

    float DEFAULT_WIDTH = 0.6f;
    float DEFAULT_HEIGHT = 1.8f;
    float DEFAULT_EYE_HEIGHT = 1.62f;

    /**
     * Unique ID for this transformation. It's recommended to use the form:
     * <p>
     * modID:form_name
     * @return A globally unique transformation ID string.
     */
    String getId();

    /**
     * Called whenever the player is changing to this new form.
     * @param player The transforming player
     */
    default void onTransform(EntityPlayer player) {}

    /**
     * Called whenever the player is reverting back to a standard player form.
     * @param player The untransforming player
     */
    default void onUntransform(EntityPlayer player) {}

    /**
     * Called every tick while the player is transformed into this form.
     * @param player The transformed player
     */
    default void onTick(EntityPlayer player) {}

    /**
     * The width of the new form, in block-widths.
     * @return A float with the width of the new form. Defaults to {@value #DEFAULT_WIDTH}
     */
    default float getWidth() {
        return DEFAULT_WIDTH;
    }

    /**
     * The height of the new form, in block-heights.
     * @return A float with the height of the new form. Defaults to {@value #DEFAULT_HEIGHT}
     */
    default float getHeight() {
        return DEFAULT_HEIGHT;
    }

    /**
     * How far off the ground the new form's eyes are, in block-heights.
     * @return A float with the eye height of the new form. Defaults to {@value #DEFAULT_EYE_HEIGHT}
     */
    default float getEyeHeight() {
        return DEFAULT_EYE_HEIGHT;
    }

    /**
     * Whether to render the hands of the new form in the player's first-person view.
     * @return true to render the character's hands (defaults to false)
     */
    default boolean renderHands() {
        return false;
    }

    /**
     * The new entity that the player should turn in to.
     * @param player The transforming player
     * @return The new entity to transform in to
     */
    @SideOnly(Side.CLIENT)
    EntityLivingBase getRenderEntity(EntityPlayer player);

    /**
     * Convenience method for sending the packet that tells the server to transform the player into this form.
     * <p>
     * Do not override this function in your implementing classes; code that should run when the player transforms into
     * your new form should be in {@link #onTransform(EntityPlayer)}.
     */
    @SideOnly(Side.CLIENT)
    default void transform() {
        NetworkHandler.instance.sendToServer(new TransformationRequestPacket(getId()));
    }
}
