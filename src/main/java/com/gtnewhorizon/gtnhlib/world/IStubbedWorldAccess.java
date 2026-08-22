package com.gtnewhorizon.gtnhlib.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IWorldAccess;

/// A subinterface of [IWorldAccess], but with all the methods stubbed to keep implementations clean.
public interface IStubbedWorldAccess extends IWorldAccess {

    @Override
    default void markBlockForUpdate(int x, int y, int z) {}

    @Override
    default void markBlockForRenderUpdate(int x, int y, int z) {}

    @Override
    default void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    default void playSound(String soundName, double x, double y, double z, float volume, float pitch) {}

    @Override
    default void playSoundToNearExcept(
        EntityPlayer player, String soundName, double x, double y, double z, float volume, float pitch) {}

    @Override
    default void spawnParticle(
        String particleName, double x, double y, double z, double motionX, double motionY, double motionZ) {}

    @Override
    default void onEntityCreate(Entity entity) {}

    @Override
    default void onEntityDestroy(Entity entity) {}

    @Override
    default void playRecord(String recordName, int x, int y, int z) {}

    @Override
    default void broadcastSound(int soundId, int x, int y, int z, int userdata) {}

    @Override
    default void playAuxSFX(EntityPlayer player, int soundId, int x, int y, int z, int userdata) {}

    @Override
    default void destroyBlockPartially(int playerEntityId, int x, int y, int z, int blockDamage) {}

    @Override
    default void onStaticEntitiesChanged() {}
}
