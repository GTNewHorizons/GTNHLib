package com.gtnewhorizon.gtnhlib.mixins.early;

import static com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent.*;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(GuiScreen.class)
public class MixinGuiScreen extends Gui {

    @Shadow
    protected static RenderItem itemRender;
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Redirect(
            at = @At(
                    remap = false,
                    target = "Lnet/minecraft/client/gui/GuiScreen;drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
                    value = "INVOKE"),
            method = "renderToolTip")
    protected void gtnhlib$drawHoveringText(GuiScreen instance, List<String> textLines, int x, int y, FontRenderer font,
            @Local(argsOnly = true) ItemStack itemIn) {
        if (textLines.isEmpty()) return;

        RenderTooltipEvent event = new RenderTooltipEvent(
                itemIn,
                instance,
                ORIGINAL_BG_START,
                ORIGINAL_BG_END,
                ORIGINAL_BORDER_START,
                ORIGINAL_BORDER_END,
                x,
                y,
                font);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;

        // setup GL states
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        itemRender.zLevel = 300.0F;
        this.zLevel = 300.0F;

        if (event.alternativeRenderer == null) {
            // determine max line width
            font = event.font;
            int maxLineWidth = 0;
            for (String line : textLines) {
                int lineWidth = font.getStringWidth(line);
                if (lineWidth > maxLineWidth) {
                    maxLineWidth = lineWidth;
                }
            }

            // calculate coords
            x = event.x + 12;
            y = event.y - 12;
            int height = 8;

            if (textLines.size() > 1) {
                height += 2 + (textLines.size() - 1) * 10;
            }

            if (x + maxLineWidth > this.width) {
                x -= 28 + maxLineWidth;
            }

            if (y + height + 6 > this.height) {
                y = this.height - height - 6;
            }

            int backgroundStart = event.backgroundStart;
            int backgroundEnd = event.backgroundEnd;
            int borderStart = event.borderStart;
            int borderEnd = event.borderEnd;

            // spotless:off
            // draw outer border
            this.drawGradientRect(x - 4,                y - 3,          x - 3,                y + height + 3, backgroundStart, backgroundEnd); // left
            this.drawGradientRect(x + maxLineWidth + 3, y - 3,          x + maxLineWidth + 4, y + height + 3, backgroundStart, backgroundEnd); // right
            this.drawGradientRect(x - 3,                y - 4,          x + maxLineWidth + 3, y - 3,          backgroundStart, backgroundStart); // top
            this.drawGradientRect(x - 3,                y + height + 3, x + maxLineWidth + 3, y + height + 4, backgroundEnd, backgroundEnd); // bottom
            // draw middle
            this.drawGradientRect(x - 3,                y - 3,          x + maxLineWidth + 3, y + height + 3, backgroundStart, backgroundEnd);
            // draw inner border
            this.drawGradientRect(x - 3,                y - 2,          x - 2,                y + height + 2, borderStart, borderEnd); // left
            this.drawGradientRect(x + maxLineWidth + 2, y - 2,          x + maxLineWidth + 3, y + height + 2, borderStart, borderEnd); // right
            this.drawGradientRect(x - 3,                y - 3,          x + maxLineWidth + 3, y - 2,          borderStart, borderStart); // top
            this.drawGradientRect(x - 3,                y + height + 2, x + maxLineWidth + 3, y + height + 3, borderEnd, borderEnd); // bottom
            // spotless:on

            // draw lines
            for (int i = 0; i < textLines.size(); i++) {
                font.drawStringWithShadow(textLines.get(i), x, y, -1);
                y += i == 0 ? 12 : 10;
            }
        } else {
            event.alternativeRenderer.accept(textLines);
        }

        // reset GL states
        this.zLevel = 0.0F;
        itemRender.zLevel = 0.0F;
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

}
