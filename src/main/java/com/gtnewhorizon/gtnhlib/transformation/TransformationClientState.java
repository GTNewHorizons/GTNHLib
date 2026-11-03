package com.gtnewhorizon.gtnhlib.transformation;

import net.minecraft.entity.player.EntityPlayer;

import org.jetbrains.annotations.ApiStatus;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;

import lombok.Getter;
import lombok.Setter;

/**
 * Used by client event handlers and mixins to handle adjusting the client view and raytracing when the player has
 * transformed.
 */
@ApiStatus.Internal
public class TransformationClientState {

    @Setter
    @Getter
    private static float renderYOffset = 0.0f;
    @Setter
    @Getter
    private static boolean isShifted = false;

    public static float getModifiedEyeHeight(final EntityPlayer player, final ITransformation form) {
        float eyeHeight = form.getEyeHeight();
        if (player.isPlayerSleeping()) {
            eyeHeight = 0.2f;
        } else if (player.isSneaking()) {
            eyeHeight -= 0.08f;
        }
        return eyeHeight;
    }
}
