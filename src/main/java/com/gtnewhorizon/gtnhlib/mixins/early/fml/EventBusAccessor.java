package com.gtnewhorizon.gtnhlib.mixins.early.fml;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;

@Mixin(value = EventBus.class, remap = false)
public interface EventBusAccessor {

    @Accessor
    Map<Object, ModContainer> getListenerOwners();

    @Accessor
    ConcurrentHashMap<Object, ArrayList<IEventListener>> getListeners();

    @Accessor
    int getBusID();
}
