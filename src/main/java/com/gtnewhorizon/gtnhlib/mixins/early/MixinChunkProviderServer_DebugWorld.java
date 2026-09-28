package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.debugworld.DebugWorldType;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

/**
 * Stops mod world generators (villages, ores, trees, ruins, ...) from decorating chunks of the debug world.
 */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServer_DebugWorld {

    @WrapWithCondition(
            method = "populate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcpw/mods/fml/common/registry/GameRegistry;generateWorld(IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)V",
                    remap = false))
    private boolean gtnhlib$skipModGeneratorsInDebugWorld(int chunkX, int chunkZ, World world,
            IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        return !DebugWorldType.isDebugWorld(world);
    }
}
