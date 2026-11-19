package com.gtnewhorizon.gtnhlib.mixins.early.models.particles;

import java.util.List;

import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(World.class)
public interface WorldAccessor {

    @Accessor
    List<IWorldAccess> getWorldAccesses();
}
