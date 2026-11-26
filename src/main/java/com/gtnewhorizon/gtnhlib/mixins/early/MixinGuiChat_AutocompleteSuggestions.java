package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompletePopupRenderer;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteStateMachine;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteStateMachine.Completion;

/// All logic lives in [AutocompleteStateMachine] and [AutocompletePopupRenderer] so that stack traces from it have
/// usable line numbers. Keep this class as thin glue.
@Mixin(GuiChat.class)
public abstract class MixinGuiChat_AutocompleteSuggestions extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Unique
    private final AutocompleteStateMachine gtnhlib$autocomplete = new AutocompleteStateMachine();

    @Unique
    private void gtnhlib$updateAutocomplete() {
        gtnhlib$autocomplete.update(mc.thePlayer, inputField.getText(), inputField.getCursorPosition());
    }

    @Inject(method = "getSentHistory", at = @At(value = "HEAD"), cancellable = true)
    private void gtnhlib$interceptArrowKeys(int delta, CallbackInfo ci) {
        gtnhlib$updateAutocomplete();

        if (gtnhlib$autocomplete.moveSelection(delta)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_146404_p_", at = @At("HEAD"), cancellable = true)
    private void gtnhlib$interceptTabCompletion(CallbackInfo ci) {
        gtnhlib$updateAutocomplete();

        Completion completion = gtnhlib$autocomplete.complete(inputField.getText(), inputField.getCursorPosition());

        if (completion != null) {
            inputField.setText(completion.text());
            inputField.setCursorPosition(completion.caret());

            ci.cancel();
        }
    }

    @Inject(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V",
                    shift = Shift.AFTER))
    private void gtnhlib$drawAutocompleteSuggestions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gtnhlib$updateAutocomplete();

        AutocompletePopupRenderer
                .draw(gtnhlib$autocomplete, inputField.getText(), inputField.getCursorPosition(), height);
    }
}
