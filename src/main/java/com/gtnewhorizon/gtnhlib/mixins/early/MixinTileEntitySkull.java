package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.blockstate.mixin.BlockSkullExt;

@Mixin(TileEntitySkull.class)
public abstract class MixinTileEntitySkull implements BlockSkullExt {

    @Shadow
    private int field_145910_i;

    @Override
    public int gtnhlib$getRotation() {
        return field_145910_i;
    }

    @Override
    public void gtnhlib$setRotation(int rotation) {
        this.field_145910_i = rotation;
        ((TileEntity) (Object) this).markDirty();
    }
}
