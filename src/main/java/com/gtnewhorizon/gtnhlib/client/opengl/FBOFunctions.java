package com.gtnewhorizon.gtnhlib.client.opengl;

public interface FBOFunctions {

    void glBindFramebuffer(int target, int framebuffer);

    void glBindRenderbuffer(int target, int renderbuffer);

    void glDeleteRenderbuffers(int renderbuffer);

    void glDeleteFramebuffers(int framebuffer);

    int glGenFramebuffers();

    int glGenRenderbuffers();

    void glRenderbufferStorage(int target, int internalformat, int width, int height);

    void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);

    int glCheckFramebufferStatus(int target);

    void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);
}
