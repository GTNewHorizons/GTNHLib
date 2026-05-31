package com.gtnewhorizon.gtnhlib.integration.mui2.sync;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.value.sync.AbstractGenericSyncValue;
import com.gtnewhorizon.gtnhlib.integration.mui2.GuiView;

public class GuiViewSyncValue extends AbstractGenericSyncValue<GuiView> {

    public GuiViewSyncValue(@Nullable Supplier<GuiView> getter, @Nullable Consumer<GuiView> setter) {
        super(GuiView.class, getter, setter);
    }

    @Override
    protected GuiView createDeepCopyOf(GuiView value) {
        return GuiView.deepCopyOf(value);
    }

    @Override
    protected boolean areEqual(GuiView a, GuiView b) {
        return a.equals(b);
    }

    @Override
    protected void serialize(PacketBuffer buffer, GuiView value) throws IOException {
        value.writeToBuf(buffer);
    }

    @Override
    protected GuiView deserialize(PacketBuffer buffer) throws IOException {
        return GuiView.readFromBuf(buffer);
    }
}
