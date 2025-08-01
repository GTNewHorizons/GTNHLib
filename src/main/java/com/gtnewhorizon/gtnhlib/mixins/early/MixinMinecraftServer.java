package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Shadow
    @Final
    public Profiler theProfiler;

    @Inject(method = "updateTimeLightAndEntities", at = @At("HEAD"))
    private void runJobs(CallbackInfo ci) {
        this.theProfiler.startSection("jobs");
        ServerThreadUtil.runJobs();
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void saveServerThreadReference(CallbackInfo ci) {
        ServerThreadUtil.setup((MinecraftServer) (Object) this, Thread.currentThread());
    }

    @Inject(method = "run", at = @At("RETURN"))
    private void clearServerThreadReference(CallbackInfo ci) {
        ServerThreadUtil.clear();
    }

    @ModifyArg(method = "run", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"))
    private long runWorkStealingJobs(long msRemaining) {
        if (msRemaining <= 1) { // there is no time to steal :(
            ServerThreadUtil.skipRemainingWorkStealingQueue();
            return msRemaining;
        }

        return ServerThreadUtil.runWorkStealingJobs(msRemaining);
    }

}
