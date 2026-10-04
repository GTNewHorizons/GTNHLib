package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.eventhandlers.TransformationEventHandler;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

@SuppressWarnings("UnusedMixin")
@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    @SuppressWarnings("NameDoesntMatchTargetClass")
    @Inject(
            method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("HEAD"),
            cancellable = true)
    public void gtnhlib$onDoRender(AbstractClientPlayer player, double x, double y, double z, float entityYaw,
            float partialTicks, CallbackInfo ci) {
        final TransformationProperty data = TransformationProperty.get(player);

        if (data != null && data.getTransformation() != null) {
            final EntityLivingBase proxy = data.getRenderEntity();

            if (proxy != null) {
                ci.cancel();

                proxy.worldObj = player.worldObj;
                proxy.ticksExisted = player.ticksExisted;

                proxy.posX = player.posX;
                proxy.posY = player.boundingBox.minY;
                proxy.posZ = player.posZ;

                proxy.prevPosX = player.prevPosX;
                proxy.prevPosY = player.boundingBox.minY;
                proxy.prevPosZ = player.prevPosZ;

                proxy.rotationPitch = player.rotationPitch;
                proxy.prevRotationPitch = player.prevRotationPitch;
                proxy.rotationYaw = player.rotationYaw;
                proxy.prevRotationYaw = player.prevRotationYaw;
                proxy.renderYawOffset = player.renderYawOffset;
                proxy.prevRenderYawOffset = player.prevRenderYawOffset;
                proxy.rotationYawHead = player.rotationYawHead;
                proxy.prevRotationYawHead = player.prevRotationYawHead;

                proxy.hurtTime = player.hurtTime;
                proxy.maxHurtTime = player.maxHurtTime;
                proxy.deathTime = player.deathTime;
                proxy.setSneaking(player.isSneaking());
                proxy.setInvisible(player.isInvisible());

                proxy.swingProgress = player.swingProgress;
                proxy.swingProgressInt = player.swingProgressInt;
                proxy.prevSwingProgress = player.prevSwingProgress;
                proxy.isSwingInProgress = player.isSwingInProgress;
                proxy.limbSwing = player.limbSwing;
                proxy.limbSwingAmount = player.limbSwingAmount;
                proxy.prevLimbSwingAmount = player.prevLimbSwingAmount;

                for (int i = 0; i <= 4; i++) {
                    proxy.setCurrentItemOrArmor(i, player.getEquipmentInSlot(i));
                }
                double renderY = gtnhlib$getRenderY(player, y, proxy);

                RenderManager.instance.renderEntityWithPosYaw(proxy, x, renderY, z, entityYaw, partialTicks);
            }
        }
    }

    @Unique
    private static double gtnhlib$getRenderY(AbstractClientPlayer player, double y, EntityLivingBase proxy) {
        double renderY = y;

        // This lowers the inventory player render to the correct height
        boolean inInventory = false;
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if ((screen instanceof GuiInventory || screen instanceof GuiContainerCreative)
                && RenderManager.instance.playerViewY == 180.0F) {
            inInventory = true;
        }

        if (player == Minecraft.getMinecraft().thePlayer && !inInventory) {
            renderY += TransformationEventHandler.renderYOffset;
        }

        // Logic for lightmap adapted from vanilla
        int i = proxy.isBurning() ? 15728880 : proxy.getBrightnessForRender(0);

        int j = i % 65536;
        int k = i / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);

        renderY = renderY - player.yOffset + proxy.yOffset;
        return renderY;
    }
}
