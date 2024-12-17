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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent;

// This mixin must run before AppleCore's GuiScreenMixin because we're using an @Overwrite
@Mixin(priority = 999, value = GuiScreen.class)
public class MixinGuiScreen extends Gui {

    @Unique
    private ItemStack gtnhlib$currentStack;

    @Shadow
    protected static RenderItem itemRender;
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(at = @At("HEAD"), method = "renderToolTip")
    private void preRenderToolTip(ItemStack itemIn, int x, int y, CallbackInfo ci) {
        this.gtnhlib$currentStack = itemIn;
    }

    @Inject(at = @At("TAIL"), method = "renderToolTip")
    private void postRenderToolTip(CallbackInfo ci) {
        this.gtnhlib$currentStack = null;
    }

    /**
     * @author glowredman
     * @reason Add RenderTooltipEvent
     */
    @Overwrite(remap = false)
    protected void drawHoveringText(List<String> textLines, int mouseX, int mouseY, FontRenderer font) {
        if (!textLines.isEmpty()) {
            // spotless:off
            /****************************************************************************************************************************************
             * IMPORTANT NOTE:                                                                                                                      *
             * Thes int variables width, lineWidth, x, y and height must stay in this order and no other int variables may be inserted before them. *
             * This due to compat with AppleCore.                                                                                                   *
             *                                                                                                                                      *
             * The potentially conflicting mixin can be found here:                                                                                 *
             * https://github.com/GTNewHorizons/AppleCore/blob/master/src/main/java/squeek/applecore/mixins/early/minecraft/GuiScreenMixin.java     *
             ****************************************************************************************************************************************/
            // spotless:on

            // create event
            RenderTooltipEvent event = new RenderTooltipEvent(
                    this.gtnhlib$currentStack,
                    (GuiScreen) (Object) this,
                    ORIGINAL_BG_START,
                    ORIGINAL_BG_END,
                    ORIGINAL_BORDER_START,
                    ORIGINAL_BORDER_END,
                    mouseX,
                    mouseY,
                    font);

            // post event if called from renderToolTip
            if (this.gtnhlib$currentStack != null) {
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) {
                    // skip all rendering
                    return;
                }
            }

            // set GL states
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            if (event.alternativeRenderer == null) {
                // re-assign variables because they might have been modified by an event handler
                mouseX = event.x;
                mouseY = event.y;
                font = event.font;

                // determine max line width
                int width = 0;
                for (String s : textLines) {
                    int lineWidth = font.getStringWidth(s);
                    if (lineWidth > width) {
                        width = lineWidth;
                    }
                }

                // calculate coordinates
                int x = mouseX + 12;
                int y = mouseY - 12;
                int height = 8;

                if (textLines.size() > 1) {
                    height += 2 + (textLines.size() - 1) * 10;
                }

                if (x + width > this.width) {
                    x -= 28 + width;
                }

                if (y + height + 6 > this.height) {
                    y = this.height - height - 6;
                }

                // set Z level
                this.zLevel = 300.0F;
                itemRender.zLevel = 300.0F;

                int backgroundStart = event.backgroundStart;
                int backgroundEnd = event.backgroundEnd;
                int borderStart = event.borderStart;
                int borderEnd = event.borderEnd;

                // spotless:off
                // draw background
                this.drawGradientRect(x - 3,         y - 4,          x + width + 3, y - 3,          backgroundStart, backgroundStart); // top
                this.drawGradientRect(x - 3,         y + height + 3, x + width + 3, y + height + 4, backgroundEnd, backgroundEnd); // bottom
                this.drawGradientRect(x - 3,         y - 3,          x + width + 3, y + height + 3, backgroundStart, backgroundEnd); // middle
                this.drawGradientRect(x - 4,         y - 3,          x - 3,         y + height + 3, backgroundStart, backgroundEnd); // left
                this.drawGradientRect(x + width + 3, y - 3,          x + width + 4, y + height + 3, backgroundStart, backgroundEnd); // right
                // draw inner border
                this.drawGradientRect(x - 3,         y - 2,          x - 2,         y + height + 2, borderStart, borderEnd); // left
                this.drawGradientRect(x + width + 2, y - 2,          x + width + 3, y + height + 2, borderStart, borderEnd); // right
                this.drawGradientRect(x - 3,         y - 3,          x + width + 3, y - 2,          borderStart, borderStart); // top
                this.drawGradientRect(x - 3,         y + height + 2, x + width + 3, y + height + 3, borderEnd, borderEnd); // bottom
                // spotless:on

                // draw text
                for (int i = 0; i < textLines.size(); i++) {
                    String s = textLines.get(i);
                    font.drawStringWithShadow(s, x, y, -1);

                    // increase Y coordinate
                    if (i == 0) {
                        y += 2;
                    }
                    y += 10;
                }

                // reset Z level
                this.zLevel = 0.0F;
                itemRender.zLevel = 0.0F;
            } else {
                event.alternativeRenderer.accept(textLines);
            }

            // reset GL states
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }
    }

}
