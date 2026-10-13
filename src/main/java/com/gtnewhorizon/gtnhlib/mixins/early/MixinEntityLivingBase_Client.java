package com.gtnewhorizon.gtnhlib.mixins.early;

import static com.gtnewhorizon.gtnhlib.transformation.TransformationClientState.getModifiedEyeHeight;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.transformation.TransformationClientState;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@SuppressWarnings("UnusedMixin")
@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase_Client {

    @WrapOperation(
        method = "rayTrace",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;getPosition(F)Lnet/minecraft/util/Vec3;"
        )
    )
    private Vec3 gtnhlib$rayTrace(final EntityLivingBase instance, final float p_70666_1_, final Operation<Vec3> original) {
        if (!TransformationClientState.isShifted() && instance instanceof final EntityPlayer player) {
            final ITransformation tf = TransformationProperty.getTransformation(player);
            if (tf != null) {
                final float defaultEyeHeight = player.getDefaultEyeHeight();
                final double renderYOffset = instance.yOffset - getModifiedEyeHeight(player, tf) + defaultEyeHeight;
                final float previousEyeHeight = player.eyeHeight;

                instance.prevPosY -= renderYOffset;
                instance.posY -= renderYOffset;
                player.eyeHeight = defaultEyeHeight;

                final Vec3 vec = original.call(instance, p_70666_1_);

                instance.prevPosY += renderYOffset;
                instance.posY += renderYOffset;
                player.eyeHeight += previousEyeHeight;

                return vec;
            }
        }

        return original.call(instance, p_70666_1_);
    }
}
