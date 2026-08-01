package com.gtnewhorizon.gtnhlib.integration.mui2;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

public class ConfirmationDialog extends Dialog<Boolean> {

    private String message = "";
    private Runnable runnable = () -> {};

    public ConfirmationDialog(String name) {
        super(name, _unused -> {});
        this.size(140, 70).child(
                Flow.column()
                        .child(
                                new TextWidget<>(IKey.dynamic(() -> this.message)).top(10).sizeRel(0.9f, 0.5f)
                                        .horizontalCenter())
                        .child(
                                Flow.row().bottom(5).size(110, 16).horizontalCenter()
                                        .child(
                                                new ButtonWidget<>().size(45, 16).left(5)
                                                        .overlay(IKey.lang("gtnhlib.gui.teams.confirm"))
                                                        .onMouseTapped(mouse -> {
                                                            this.closeWith(true);
                                                            return true;
                                                        }))
                                        .child(
                                                new ButtonWidget<>().size(45, 16).right(5)
                                                        .overlay(IKey.lang("gtnhlib.gui.teams.cancel"))
                                                        .onMouseTapped(mouse -> {
                                                            this.closeWith(false);
                                                            return true;
                                                        }))));

        this.setDisablePanelsBelow(true).setDraggable(false);

    }

    @Override
    public void closeWith(Boolean result) {
        if (result) {
            this.runnable.run();
        }
        closeIfOpen();
    }

    public void setParams(String message, Runnable runnable) {
        this.message = message;
        this.runnable = runnable;
    }

}
