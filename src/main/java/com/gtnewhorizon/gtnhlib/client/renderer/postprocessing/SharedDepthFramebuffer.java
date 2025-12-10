package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.compat.Mods;

/**
 * A framebuffer that shares the depth buffer of another {@link Framebuffer}.
 * <p>
 * This class is useful when rendering multiple targets that need to maintain consistent depth information — for
 * example, when rendering color and post-processing passes that must use the same depth data.
 * </p>
 *
 * <p>
 * Unlike a normal {@link CustomFramebuffer}, a {@code SharedDepthFramebuffer} does not create or own its own depth
 * attachment. Instead, it reuses the depth buffer from the specified source framebuffer. This allows multiple
 * framebuffers to share the same depth information without redundant depth copies or memory usage.
 * </p>
 *
 * <p>
 * By default, {@link #DEPTH_ENABLED} will be {@code true}. <br>
 * Keep in mind that the framebuffer will only use a stencil if the linked {@link Framebuffer} has stencil enabled.
 * </p>
 *
 * @see CustomFramebuffer
 */
public class SharedDepthFramebuffer extends CustomFramebuffer {

    private final Framebuffer linkedBuffer;

    public SharedDepthFramebuffer(int settings, Framebuffer linkedBuffer) {
        super(settings);
        if (linkedBuffer == null) {
            throw new IllegalArgumentException("linkedBuffer cannot be null!");
        }
        this.linkedBuffer = linkedBuffer;
    }

    public SharedDepthFramebuffer(int width, int height, int settings, Framebuffer linkedBuffer) {
        super(width, height, settings);
        if (linkedBuffer == null) {
            throw new IllegalArgumentException("linkedBuffer cannot be null!");
        }
        this.linkedBuffer = linkedBuffer;
    }

    public SharedDepthFramebuffer(int settings) {
        this(settings, Minecraft.getMinecraft().getFramebuffer());
    }

    public SharedDepthFramebuffer(int width, int height, int settings) {
        this(width, height, settings, Minecraft.getMinecraft().getFramebuffer());
    }

    @Override
    protected void createDepthAttachment(int width, int height) {
        depthAttachment = -1;
    }

    @Override
    protected final int createBufferBits() {
        return GL11.GL_COLOR_BUFFER_BIT;
    }

    private boolean isStencilEnabled() {
        return isEnabled(STENCIL_BUFFER) && isMinecraftStencilEnabled();
    }

    // Don't delete the depth texture, it's still being used by the linked framebuffer.
    @Override
    protected void deleteFramebuffer() {
        unbindFramebufferTexture();
        unbindFramebuffer();

        this.depthAttachment = -1;

        if (this.framebufferTexture > -1) {
            GL11.glDeleteTextures(this.framebufferTexture);
            this.framebufferTexture = -1;
        }

        OpenGlHelper.func_153174_h(this.framebufferObject);
        this.framebufferObject = -1;
    }

    @Override
    public void bindFramebuffer() {
        super.bindFramebuffer();
        if (linkedBuffer != null) {
            if (Mods.ANGELICA && linkedBuffer instanceof IRenderTargetExt) {
                final int attachment = ((IRenderTargetExt) linkedBuffer).iris$getDepthTextureId();
                if (attachment != depthAttachment) {
                    depthAttachment = attachment;
                    linkDepthTexture(isStencilEnabled());
                }
            } else {
                final int attachment = linkedBuffer.depthBuffer;
                if (attachment != depthAttachment) {
                    depthAttachment = attachment;
                    linkDepthRenderbuffer(isStencilEnabled());
                }
            }
        }
    }

    public static boolean isMinecraftStencilEnabled() {
        return MinecraftForgeClient.getStencilBits() != 0;
    }
}
