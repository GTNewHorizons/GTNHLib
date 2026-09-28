package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.debugworld.DebugWorldType;

/**
 * Turns off random block ticks in the debug world, so crops don't grow, leaves don't decay and grass doesn't spread.
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer_DebugWorld {

    @Redirect(
            method = "func_147456_g",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;getNeedsRandomTick()Z"))
    private boolean gtnhlib$skipRandomTicksInDebugWorld(ExtendedBlockStorage section) {
        if (DebugWorldType.isDebugWorld((World) (Object) this)) return false;
        return section.getNeedsRandomTick();
    }
}
