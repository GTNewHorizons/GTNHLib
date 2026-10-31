package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.api.IBlockWithOutlineShape;
import com.gtnewhorizon.gtnhlib.api.OutlineShape;

@Mixin(Block.class)
public abstract class MixinBlock_OutlineShapeRayTrace {

    @Inject(method = "collisionRayTrace", at = @At("HEAD"), cancellable = true)
    private void gtnhlib$rayTraceOutlineShape(World world, int x, int y, int z, Vec3 start, Vec3 end,
            CallbackInfoReturnable<MovingObjectPosition> cir) {
        if (!((Object) this instanceof IBlockWithOutlineShape shapedBlock)) return;
        OutlineShape shape = shapedBlock.getOutlineShape(world, x, y, z);
        if (shape == null || shape.isEmpty()) return;
        // Vanilla's ray trace does this first; hit particles read the bounds.
        ((Block) (Object) this).setBlockBoundsBasedOnState(world, x, y, z);
        cir.setReturnValue(shape.rayTrace(x, y, z, start, end));
    }
}
