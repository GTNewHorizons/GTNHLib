package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.gtnewhorizon.gtnhlib.compat.Mods;

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

    private void linkDepthAttachment() {
        OpenGlHelper.func_153188_a(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D,
                this.depthAttachment,
                0);
        if (isEnabled(STENCIL_BUFFER)) {
            OpenGlHelper.func_153188_a(
                    GL30.GL_FRAMEBUFFER,
                    GL30.GL_STENCIL_ATTACHMENT,
                    GL11.GL_TEXTURE_2D,
                    this.depthAttachment,
                    0);
        }
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
            final int attachment = Mods.ANGELICA ? ((IRenderTargetExt) linkedBuffer).iris$getDepthTextureId()
                    : linkedBuffer.depthBuffer;
            if (attachment != depthAttachment) {
                depthAttachment = attachment;
                linkDepthAttachment();
            }
        }
    }
}
