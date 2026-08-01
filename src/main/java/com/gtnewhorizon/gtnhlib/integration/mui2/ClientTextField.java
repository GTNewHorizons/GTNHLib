package com.gtnewhorizon.gtnhlib.integration.mui2;

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.widgets.textfield.BaseTextFieldWidget;

public class ClientTextField extends BaseTextFieldWidget<ClientTextField> {

    public ClientTextField() {
        super();

        widgetTheme(CustomWidgetTheme.BACKGROUND_TEXT_FIELD);
        setText("");
    }

    @Override
    public void onFocus(ModularGuiContext context) {
        super.onFocus(context);
        Point main = this.handler.getMainCursor();
        if (main.x == 0) {
            this.handler.setCursor(main.y, getText().length(), true, true);
        }
    }

    @Override
    public boolean canHover() {
        return true;
    }

    @NotNull
    public String getText() {
        if (this.handler.getText().isEmpty()) {
            return "";
        }
        if (this.handler.getText().size() > 1) {
            throw new IllegalStateException("This widget only supports one line!");
        }
        return this.handler.getText().get(0);
    }

    public void setText(@NotNull String text) {
        if (this.handler.getText().isEmpty()) {
            this.handler.getText().add(text);
        } else {
            this.handler.getText().set(0, text);
        }
    }
}
