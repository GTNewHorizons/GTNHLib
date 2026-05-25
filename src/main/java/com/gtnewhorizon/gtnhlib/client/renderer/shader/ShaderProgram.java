package com.gtnewhorizon.gtnhlib.client.renderer.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.GTNHLib;

public class ShaderProgram {

    protected int program;

    public ShaderProgram(String domain, String vertShaderFilename, String fragShaderFilename, IShaderDefinesInjector... defines) {
        int program;
        try {
            final String vsh = loadShaderSource(domain, vertShaderFilename, defines);
            final String fsh = loadShaderSource(domain, fragShaderFilename, defines);
            program = createProgram(vsh, fsh);
        } catch (Exception e) {
            GTNHLib.LOG.error("Could not initialize shader program!", e);
            program = 0;
        }
        this.program = program;
    }

    public ShaderProgram(String vertShaderSource, String fragShaderSource) {
        this.program = createProgram(vertShaderSource, fragShaderSource);
    }

    // ONLY WORKS IN DEV ENV
    protected final void reload(Path vertexFile, Path fragmentFile, IShaderDefinesInjector[] defines) {
        int program;
        try {
            final String vsh = loadShaderSource(vertexFile, defines);
            final String fsh = loadShaderSource(fragmentFile, defines);
            program = createProgram(vsh, fsh);
        } catch (Exception e) {
            GTNHLib.LOG.error("Could not initialize shader program!", e);
            program = 0;
        }
        this.program = program;
    }

    private static String getProgramLogInfo(int obj) {
        return GL20.glGetProgramInfoLog(obj, GL20.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
    }

    private static String getShaderLogInfo(int obj) {
        return GL20.glGetShaderInfoLog(obj, GL20.glGetShaderi(obj, GL20.GL_INFO_LOG_LENGTH));
    }

    public static int createProgram(String vertSource, String fragSource) {
        final int program = GL20.glCreateProgram();

        final int vertShader = loadShaderFromSource(program, vertSource, GL20.GL_VERTEX_SHADER);
        final int fragShader = loadShaderFromSource(program, fragSource, GL20.GL_FRAGMENT_SHADER);

        if (vertShader != 0) GL20.glAttachShader(program, vertShader);
        if (fragShader != 0) GL20.glAttachShader(program, fragShader);

        GL20.glLinkProgram(program);

        if (vertShader != 0) GL20.glDeleteShader(vertShader);
        if (fragShader != 0) GL20.glDeleteShader(fragShader);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            GTNHLib.LOG.error("Could not link shader: {}", getProgramLogInfo(program));
            GL20.glDeleteProgram(program);
            return 0;
        }

        GL20.glValidateProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            GTNHLib.LOG.error("Could not validate shader: {}", getProgramLogInfo(program));
            GL20.glDeleteProgram(program);
            return 0;
        }

        return program;
    }

    private static int loadShaderFromSource(int program, String source, int shaderType) {
        final int shader = GL20.glCreateShader(shaderType);

        if (shader == 0) {
            GTNHLib.LOG.error(
                "Could not create shader of type " + shaderType + ": " + getProgramLogInfo(program),
                new IllegalStateException()
            );
            return 0;
        }

        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GTNHLib.LOG.error("Could not compile shader: " + getShaderLogInfo(shader), new IllegalStateException());
            GL20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public static String loadShaderSource(String domain, String path, IShaderDefinesInjector... defines) {
        return loadShaderSource(new ResourceLocation(domain, path), defines);
    }

    public static String loadShaderSource(ResourceLocation resourceLocation, IShaderDefinesInjector... defines) {
        try {
            final InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation)
                    .getInputStream();
            return loadShaderSource(inputStream, defines);
        } catch (Exception e) {
            GTNHLib.LOG.error("Could not load shader file!", e);
        }

        return null;
    }

    public static String loadShaderSource(InputStream inputStream, IShaderDefinesInjector... defines) {
        try {
            final StringBuilder code = new StringBuilder();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line);
                code.append('\n');
                if (line.startsWith("#version") && defines != null) {
                    for (IShaderDefinesInjector writer : defines) {
                        writer.writeDefines(code);
                    }
                }
            }
            reader.close();

            return code.toString();
        } catch (Exception e) {
            GTNHLib.LOG.error("Could not load shader file!", e);
        }
        return null;
    }

    // ONLY WORKS IN DEV ENV
    private static String loadShaderSource(Path path, IShaderDefinesInjector[] defines) {
        try {
            return loadShaderSource(Files.newInputStream(path), defines);
        } catch (IOException e) {
            GTNHLib.LOG.error("Could not load shader file: " + path, e);
        }
        return null;
    }


    public int getProgram() {
        return this.program;
    }

    public void use() {
        GL20.glUseProgram(this.program);
    }

    public static void unbind() {
        GL20.glUseProgram(0);
    }

    public static void clear() {
        unbind();
    }

    public final int getUniformLocation(String name) {
        final int index = GL20.glGetUniformLocation(this.program, name);

        if (index < 0) {
            GTNHLib.LOG.error("No uniform exists with name: " + name, new IllegalStateException());
        }

        return index;
    }

    public final int getAttribLocation(String name) {
        final int index = GL20.glGetAttribLocation(this.program, name);

        if (index < 0) {
            GTNHLib.LOG.error("No attribute exists with name: " + name, new IllegalStateException());
        }

        return index;
    }

    public final void bindTextureSlot(String sampler2DName, int index) {
        GL20.glUniform1i(this.getUniformLocation(sampler2DName), index);
    }

    public final void bindTextureSlots(String... sampler2DNames) {
        GL20.glUseProgram(this.program);
        for (int i = 0; i < sampler2DNames.length; i++) {
            bindTextureSlot(sampler2DNames[i], i);
        }
        clear();
    }

    @Deprecated // For clarity, use delete() instead
    public void close() {
        delete();
    }

    public void delete() {
        GL20.glDeleteProgram(program);
    }

    public final void glUniform2f(int location, Vector2f vec2) {
        GL20.glUniform2f(location, vec2.x, vec2.y);
    }

    /**
     * Only uploads the uniform variable if {@code last != vec2} and automatically updates the value of {@code last}.
     */
    public final void glUniform2f(int location, Vector2f vec2, Vector2f last) {
        if (last.x != vec2.x || last.y != vec2.y) {
            glUniform2f(location, vec2);
            last.set(vec2);
        }
    }

    public final void glUniform3f(int location, Vector3f vec3) {
        GL20.glUniform3f(location, vec3.x, vec3.y, vec3.z);
    }

    /**
     * Only uploads the uniform variable if {@code last != vec3} and automatically updates the value of {@code last}.
     */
    public final void glUniform3f(int location, Vector3f vec3, Vector3f last) {
        if (last.x != vec3.x || last.y != vec3.y || last.z != vec3.z) {
            glUniform3f(location, vec3);
            last.set(vec3);
        }
    }
}
