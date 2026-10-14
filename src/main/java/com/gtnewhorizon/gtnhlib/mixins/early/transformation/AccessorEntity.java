package com.gtnewhorizon.gtnhlib.mixins.early.transformation;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnusedMixin")
@Mixin(Entity.class)
public interface AccessorEntity {

    @Invoker("setSize")
    void invokeSetSize(float width, float height);
}
