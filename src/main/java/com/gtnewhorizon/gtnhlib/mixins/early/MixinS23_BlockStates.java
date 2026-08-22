package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.network.play.server.S23PacketBlockChange;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockState_S23Ext;

@Mixin(S23PacketBlockChange.class)
public class MixinS23_BlockStates implements BlockState_S23Ext {

    @Unique
    private JsonObject gtnhlib$blockState;

    @Override
    public JsonObject gtnhlib$getBlockState() {
        return gtnhlib$blockState;
    }

    @Override
    public void gtnhlib$setBlockState(JsonObject state) {
        gtnhlib$blockState = state;
    }
}
