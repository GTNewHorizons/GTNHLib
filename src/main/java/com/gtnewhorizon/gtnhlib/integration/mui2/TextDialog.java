package com.gtnewhorizon.gtnhlib.integration.mui2;

import java.util.function.Consumer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gtnewhorizon.gtnhlib.teams.Team;

public class TextDialog extends Dialog<String> {

    private String message = "";
    private String initialValue = "";
    private Consumer<String> stringConsumer = string -> {};
    private final ClientTextField field = new ClientTextField();

    public TextDialog(String name) {
        super(name, _unused -> {});

        this.size(140, 90).child(
                Flow.column().child(
                        new TextWidget<>(IKey.dynamic(() -> this.message)).size(120, 40).top(10).horizontalCenter())
                        .child(this.field.width(120).height(14).horizontalCenter().bottom(25)).child(
                                Flow.row().bottom(5).size(110, 16).horizontalCenter()
                                        .child(
                                                new ButtonWidget<>().size(45, 16).left(5)
                                                        .overlay(IKey.lang("gtnhlib.gui.teams.confirm"))
                                                        .onMouseTapped(mouse -> {
                                                            this.closeWith(this.field.getText());
                                                            return true;
                                                        }))
                                        .child(
                                                new ButtonWidget<>().size(45, 16).right(5)
                                                        .overlay(IKey.lang("gtnhlib.gui.teams.cancel"))
                                                        .onMouseTapped(mouse -> {
                                                            this.closeWith(null);
                                                            return true;
                                                        }))));

        this.setDisablePanelsBelow(true).setDraggable(false);

    }

    @Override
    public void closeWith(String result) {
        if (result != null && !result.isEmpty()
                && result.length() < Team.MAX_TEAM_NAME_LENGTH
                && !result.equals(this.initialValue)) {
            this.stringConsumer.accept(result.replace("\n", ""));
        }
        closeIfOpen();
    }

    public void setParams(String message, String initialValue, Consumer<String> stringConsumer) {
        this.message = message;
        this.initialValue = initialValue;
        this.stringConsumer = stringConsumer;
        this.field.setText(initialValue);
    }
}
