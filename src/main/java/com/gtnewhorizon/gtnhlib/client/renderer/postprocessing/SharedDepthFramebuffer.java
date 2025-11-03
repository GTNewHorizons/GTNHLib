package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

public class SharedDepthFramebuffer extends CustomFramebuffer {

    private final Framebuffer linkedBuffer;

    public SharedDepthFramebuffer(int settings, Framebuffer linkedBuffer) {
        super(settings | DEPTH_ENABLED);
        this.linkedBuffer = linkedBuffer;
    }

    public SharedDepthFramebuffer(int width, int height, int settings, Framebuffer linkedBuffer) {
        super(width, height, settings | DEPTH_ENABLED);
        this.linkedBuffer = linkedBuffer;
    }

    @Override
    protected void createDepthAttachment(int width, int height) {

    }

    private void linkDepthAttachment() {
        GLStateManager.glBindTexture(GL11.GL_TEXTURE_2D, this.depthAttachment);
        final boolean stencil = MinecraftForgeClient.getStencilBits() != 0;
        OpenGlHelper.func_153188_a(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthAttachment, 0);
        if (stencil) {
            OpenGlHelper.func_153188_a(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthAttachment, 0);
        }
//        if (isEnabled(STENCIL_BUFFER)) {
//            OpenGlHelper.func_153188_a(
//                    GL30.GL_FRAMEBUFFER,
//                    GL30.GL_STENCIL_ATTACHMENT,
//                    GL11.GL_TEXTURE_2D,
//                    this.depthAttachment,
//                    0);
//        }
    }

    @Override
    protected void deleteFramebuffer() {
        if (this.framebufferTexture > -1) {
            GL11.glDeleteTextures(this.framebufferTexture);
            this.framebufferTexture = -1;
        }

        if (this.framebufferObject > -1) {
            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, 0);
            OpenGlHelper.func_153174_h(this.framebufferObject);
            this.framebufferObject = -1;
        }
    }

    @Override
    public void bindFramebuffer() {
        super.bindFramebuffer();
        // TODO clean up
        if (linkedBuffer != null && ((IRenderTargetExt) linkedBuffer).iris$getDepthTextureId() != depthAttachment) {
            this.depthAttachment = ((IRenderTargetExt) linkedBuffer).iris$getDepthTextureId();
            createFramebuffer(framebufferWidth, framebufferHeight);
            linkDepthAttachment();
        }
    }

    @Override
    protected final int createBufferBits() {
        return GL11.GL_COLOR_BUFFER_BIT;
    }
}
