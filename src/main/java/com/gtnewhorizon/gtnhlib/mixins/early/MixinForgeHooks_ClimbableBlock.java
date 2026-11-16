package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.blocks.util.BlockClimbing;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(value = ForgeHooks.class, remap = false)
public class MixinForgeHooks_ClimbableBlock {

    @ModifyReturnValue(method = "isLivingOnLadder", at = @At("RETURN"))
    private static boolean gtnhlib$climbBlock(boolean original, Block block, World world, int x, int y, int z,
            EntityLivingBase entity) {
        return original || BlockClimbing.isClimbing(world, entity);
    }
}
