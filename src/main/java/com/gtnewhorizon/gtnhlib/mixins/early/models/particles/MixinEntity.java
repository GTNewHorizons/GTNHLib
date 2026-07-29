package com.gtnewhorizon.gtnhlib.mixins.early.models.particles;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@SuppressWarnings("UnusedMixin")
@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    public World worldObj;

    @WrapOperation(
            method = "onEntityUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"))
    private void nhlib$fixBreakIcon(World instance, String particleName, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, Operation<Void> original, @Local(name = "block") Block block,
            @Local(name = "j") int j, @Local(name = "i") int i, @Local(name = "k") int k) {
        if (!((BlockModelInfo) block).nhlib$isModeled()) {
            original.call(instance, particleName, x, y, z, velocityX, velocityY, velocityZ);
            return;
        }

        // This is a modeled block! Refer to our helper for particles instead of the default.
        for (var iwa : ((WorldAccessor) worldObj).getWorldAccesses()) {
            if (iwa instanceof RenderGlobal rg)
                nhlib$spawnWalkParticle(rg, j, i, k, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    /// Spawn a particle for an entity walking on a block. This method injects
    ///
    /// @param rg The RenderGlobal instance the particle is spawned in.
    /// @param blockX X position of the block the particle is spawned from.
    /// @param blockY Y position...
    /// @param blockZ Z position...
    /// @param x X position of the particle
    /// @param y Y position...
    /// @param z Z position...
    /// @param vX X velocity of particle
    /// @param vY Y velocity...
    /// @param vZ Z velocity...
    @Unique
    private static EntityFX nhlib$spawnWalkParticle(RenderGlobal rg, int blockX, int blockY, int blockZ, double x,
            double y, double z, double vX, double vY, double vZ) {
        var mc = Minecraft.getMinecraft();
        if (mc == null || mc.renderViewEntity == null || mc.effectRenderer == null) return null;

        // Skip 3/4 of particles when they're set to decreased
        var world = ((RenderGlobalAccessor) rg).getTheWorld();
        if (mc.gameSettings.particleSetting == 1 && world.rand.nextInt(3) == 0) return null;

        // Don't render distant (>16 blocks) particles.
        var dx = mc.renderViewEntity.posX - x;
        var dy = mc.renderViewEntity.posY - y;
        var dz = mc.renderViewEntity.posZ - z;
        if (dx * dx + dy * dy * dz * dz > 16 * 16) return null;

        var block = world.getBlock(blockX, blockY, blockZ);
        var meta = world.getBlockMetadata(blockX, blockY, blockZ);
        var digFX = new EntityDiggingFX(world, x, y, z, vX, vY, vZ, block, meta);
        digFX.setParticleIcon(ModelISBRH.INSTANCE.get().getParticleIcon(world, blockX, blockY, blockZ));
        mc.effectRenderer.addEffect(digFX);

        return digFX;
    }
}
