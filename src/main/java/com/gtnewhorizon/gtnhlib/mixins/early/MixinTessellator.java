package com.gtnewhorizon.gtnhlib.mixins.early;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.client.renderer.ITessellatorInstance;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;

@Mixin(Tessellator.class)
public abstract class MixinTessellator implements ITessellatorInstance {

    @Shadow
    public int vertexCount;
    @Shadow
    public boolean isDrawing;

    @Shadow
    public abstract void reset();

    /**
     * @reason Allow using multiple tessellator instances concurrently by removing static field access in alternate
     *         instances.
     **/
    @Redirect(method = "reset", at = @At(value = "INVOKE", target = "Ljava/nio/ByteBuffer;clear()Ljava/nio/Buffer;"))
    private Buffer removeStaticBufferResetOutsideSingleton(ByteBuffer buffer) {
        if (TessellatorManager.isMainInstance(this)) {
            return buffer.clear();
        }
        return buffer;
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void preventOffMainThreadDrawing(CallbackInfoReturnable<Integer> cir) {
        if (!TessellatorManager.isMainInstance(this)) {
            throw new RuntimeException("Tried to draw on a tessellator that isn't on the main thread!");
        }
    }

    // New methods from ITesselatorInstance
    @Override
    public void discard() {
        isDrawing = false;
        reset();
    }

}
