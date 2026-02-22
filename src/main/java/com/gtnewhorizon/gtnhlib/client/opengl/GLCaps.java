package com.gtnewhorizon.gtnhlib.client.opengl;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VaoFunctions;

public final class GLCaps {

    private static final boolean bufferStorage;
    private static final boolean texStorage;
    private static final boolean coreProfile;

    // To replace the obfuscated OpenGlHelper method names
    public static final FBOFunctions FBO;
    public static final VaoFunctions VAO;

    public static boolean bufferStorageSupported() {
        return bufferStorage;
    }

    public static boolean texStorageSupported() {
        return texStorage;
    }

    public static boolean fboSupported() {
        return FBO != null;
    }

    public static boolean isCoreProfile() {
        return coreProfile;
    }

    static {
        ContextCapabilities caps = GLContext.getCapabilities();
        bufferStorage = caps.OpenGL44 || caps.GL_ARB_buffer_storage;
        texStorage = caps.OpenGL42 || caps.GL_ARB_texture_storage;
        if (caps.OpenGL32) {
            final int mask = GL11.glGetInteger(GL32.GL_CONTEXT_PROFILE_MASK);
            coreProfile = (mask & GL32.GL_CONTEXT_CORE_PROFILE_BIT) != 0;
        } else if (caps.OpenGL31) {
            coreProfile = !caps.GL_ARB_compatibility;
        } else {
            coreProfile = false;
        }

        if (caps.OpenGL30 || caps.GL_ARB_framebuffer_object) {
            FBO = new FBOGL3();
        } else if (caps.GL_EXT_framebuffer_object) {
            FBO = new FBOEXT();
        } else {
            FBO = null; // Any device that does not support FBO's likely won't be able to run MC anyway
        }

        VAO = UniversalVAO.getImplementation(caps);
    }

    private static final class FBOGL3 implements FBOFunctions {

        @Override
        public void glBindFramebuffer(int target, int framebuffer) {
            GL30.glBindFramebuffer(target, framebuffer);
        }

        @Override
        public void glBindRenderbuffer(int target, int renderbuffer) {
            GL30.glBindRenderbuffer(target, renderbuffer);
        }

        @Override
        public void glDeleteRenderbuffers(int renderbuffer) {
            GL30.glDeleteRenderbuffers(renderbuffer);
        }

        @Override
        public void glDeleteFramebuffers(int framebuffer) {
            GL30.glDeleteFramebuffers(framebuffer);
        }

        @Override
        public int glGenFramebuffers() {
            return GL30.glGenFramebuffers();
        }

        @Override
        public int glGenRenderbuffers() {
            return GL30.glGenRenderbuffers();
        }

        @Override
        public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
            GL30.glRenderbufferStorage(target, internalformat, width, height);
        }

        @Override
        public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
            GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        }

        @Override
        public int glCheckFramebufferStatus(int target) {
            return GL30.glCheckFramebufferStatus(target);
        }

        @Override
        public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
            GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        }
    }

    private static final class FBOEXT implements FBOFunctions {

        @Override
        public void glBindFramebuffer(int target, int framebuffer) {
            EXTFramebufferObject.glBindFramebufferEXT(target, framebuffer);
        }

        @Override
        public void glBindRenderbuffer(int target, int renderbuffer) {
            EXTFramebufferObject.glBindRenderbufferEXT(target, renderbuffer);
        }

        @Override
        public void glDeleteRenderbuffers(int renderbuffer) {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(renderbuffer);
        }

        @Override
        public void glDeleteFramebuffers(int framebuffer) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
        }

        @Override
        public int glGenFramebuffers() {
            return EXTFramebufferObject.glGenFramebuffersEXT();
        }

        @Override
        public int glGenRenderbuffers() {
            return EXTFramebufferObject.glGenRenderbuffersEXT();
        }

        @Override
        public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
            EXTFramebufferObject.glRenderbufferStorageEXT(target, internalformat, width, height);
        }

        @Override
        public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
            EXTFramebufferObject.glFramebufferRenderbufferEXT(target, attachment, renderbuffertarget, renderbuffer);
        }

        @Override
        public int glCheckFramebufferStatus(int target) {
            return EXTFramebufferObject.glCheckFramebufferStatusEXT(target);
        }

        @Override
        public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
            EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
        }
    }
}
