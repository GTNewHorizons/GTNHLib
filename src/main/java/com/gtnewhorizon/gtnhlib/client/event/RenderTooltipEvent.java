package com.gtnewhorizon.gtnhlib.client.event;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

/**
 * RenderTooltipEvent is fired when a tooltip is about to be rendered. With this event you can modify colors,
 * coordinates, font or replace the renderer completely. To stop the tooltip from rendering at all, you can
 * {@link Event#setCanceled(boolean) cancel} it.
 * <p>
 * <b>Note:</b> Use {@link ItemTooltipEvent} to modify the text being displayed.
 * <p>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS} (client-side only).
 * 
 * @since 0.6.0
 * @author glowredman
 * @see ItemTooltipEvent
 */
@Cancelable
public class RenderTooltipEvent extends Event {

    public static final int ORIGINAL_BG_START = 0xF0100010;
    public static final int ORIGINAL_BG_END = 0xF0100010;
    public static final int ORIGINAL_BORDER_START = 0x505000FF;
    public static final int ORIGINAL_BORDER_END = (ORIGINAL_BORDER_START & 0xFEFEFE) >> 1
            | ORIGINAL_BORDER_START & 0xFF000000;

    /**
     * The ItemStack of which the tooltip is being displayed
     */
    @Nonnull
    public final ItemStack itemStack;

    /**
     * The GUI in which the tooltip is being rendered
     */
    @Nonnull
    public final GuiScreen gui;

    /**
     * The upper background color in ARGB format
     * <p>
     * Default value: {@code 0xF0100010}
     */
    public int backgroundStart;

    /**
     * The lower background color in ARGB format
     * <p>
     * Default value: {@code 0xF0100010}
     */
    public int backgroundEnd;

    /**
     * The upper border color in ARGB format
     * <p>
     * Default value: {@code 0x505000FF}
     */
    public int borderStart;

    /**
     * The lower border color in ARGB format
     * <p>
     * Default value: {@code 0x5028007F}
     */
    public int borderEnd;

    /**
     * The X coordinate of the mouse cursor
     */
    public int x;

    /**
     * The Y coordinate of the mouse cursor
     */
    public int y;

    /**
     * The FontRenderer used to render the tooltip's text
     * <p>
     * Default value: {@link GuiScreen#fontRendererObj} or {@link Item#getFontRenderer(ItemStack)} (if that method
     * doesn't return {@code null})
     */
    @Nonnull
    public FontRenderer font;

    /**
     * Optional hook to completely replace the rendering code. Will be used instead of the vanilla code if not
     * {@code null}. The provided argument is the text to render.
     * <p>
     * <b>Note:</b> The usage may break compat with other mods, for example AppleCore!
     * <p>
     * Default value: {@code null}
     * 
     * @apiNote The following GL states will be disabled before calling this hook: {@link GL12#GL_RESCALE_NORMAL},
     *          {@link GL11#GL_LIGHTING}, {@link GL11#GL_LIGHT0}, {@link GL11#GL_LIGHT1},
     *          {@link GL11#GL_COLOR_MATERIAL}, {@link GL11#GL_DEPTH_TEST}. They will be re-enabled after the hook is
     *          called. {@link Gui#zLevel GuiScreen.zLevel} and {@link RenderItem#zLevel GuiScreen.itemRender.zLevel}
     *          are set to {@code 300} before calling {@link Consumer#accept(Object) alternativeRenderer.accept(List)}
     *          and reset to {@code 0} afterwards. Any more complex behaviors must be handled by the hook. An <a
     *          href=https://forge.gemwire.uk/wiki/Access_Transformers>AccessTransformer</a> may be needed for this.
     */
    @Nullable
    public Consumer<List<String>> alternativeRenderer;

    public RenderTooltipEvent(ItemStack itemStack, GuiScreen gui, int backgroundStart, int backgroundEnd,
            int borderStart, int borderEnd, int x, int y, FontRenderer font) {
        this.itemStack = itemStack;
        this.gui = gui;
        this.backgroundStart = backgroundStart;
        this.backgroundEnd = backgroundEnd;
        this.borderStart = borderStart;
        this.borderEnd = borderEnd;
        this.x = x;
        this.y = y;
        this.font = font;
    }

    /**
     * Convenience method to set both background {@link #backgroundStart start}/{@link #backgroundEnd end} color to the
     * same value (the default background does not have a gradient)
     * 
     * @param background The background color in ARGB format
     */
    public void setBackground(int background) {
        this.backgroundStart = background;
        this.backgroundEnd = background;
    }

    /**
     * Calculates {@link #borderEnd} based on {@link #borderStart} using the vanilla algorithm
     */
    public void calculateBorderEnd() {
        this.borderEnd = (this.borderStart & 0xFEFEFE) >> 1 | this.borderStart & 0xFF000000;
    }
}
