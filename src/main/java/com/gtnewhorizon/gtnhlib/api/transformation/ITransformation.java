package com.gtnewhorizon.gtnhlib.api.transformation;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface ITransformation {

    float DEFAULT_WIDTH = 0.6f;
    float DEFAULT_HEIGHT = 1.8f;
    float DEFAULT_EYE_HEIGHT = 1.62f;

    String getId();

    default void onTransform(EntityPlayer player) {}

    default void onUntransform(EntityPlayer player) {}

    default void onTick(EntityPlayer player) {}

    default float getWidth() {
        return DEFAULT_WIDTH;
    }

    default float getHeight() {
        return DEFAULT_HEIGHT;
    }

    default float getEyeHeight() {
        return DEFAULT_EYE_HEIGHT;
    }

    default boolean renderHands() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    EntityLivingBase getRenderEntity(EntityPlayer player);
}
