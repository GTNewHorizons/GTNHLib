package com.gtnewhorizon.gtnhlib.client.renderer.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.GTNHLib;

@SuppressWarnings("unused")
public class ShaderProgram implements AutoCloseable {

    protected int program;

    public ShaderProgram(String domain, String vertShaderFilename, String fragShaderFilename) {
        int program;
        try {
            program = createProgram(domain, vertShaderFilename, fragShaderFilename);
        } catch (Exception e) {
            GTNHLib.LOG.error("Could not initialize shader program!", e);
            program = 0;
        }
        this.program = program;
    }

    // ONLY WORKS IN DEV ENV
    protected void reload(Path vertexFile, Path fragmentFile) {
        int program;
        try {
            program = createProgramFromPath(vertexFile, fragmentFile);
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

    private static int createProgram(String domain, String vertShaderFilename, String fragShaderFilename) {
        if (!OpenGlHelper.shadersSupported) {
            return 0;
        }

        final int program = GL20.glCreateProgram();

        final int vertShader = loadAndCompileShader(program, domain, vertShaderFilename, GL20.GL_VERTEX_SHADER);
        final int fragShader = loadAndCompileShader(program, domain, fragShaderFilename, GL20.GL_FRAGMENT_SHADER);

        return linkValidateShader(program, vertShader, fragShader);
    }

    private static int loadAndCompileShader(int program, String domain, String filename, int shaderType) {
        if (filename == null) {
            return 0;
        }

        final int shader = GL20.glCreateShader(shaderType);

        if (shader == 0) {
            GTNHLib.LOG.error(
                    "Could not create shader of type {} from {}: {}",
                    shaderType,
                    filename,
                    getProgramLogInfo(program));
            return 0;
        }

        final String code = loadFile(new ResourceLocation(domain, filename));
        if (code == null) {
            GL20.glDeleteShader(shader);
            return 0;
        }

        GL20.glShaderSource(shader, code);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GTNHLib.LOG.error("Could not compile shader {}: {}", filename, getShaderLogInfo(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private static int linkValidateShader(int program, int vertShader, int fragShader) {

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

    // ONLY WORKS IN DEV ENV
    private static int createProgramFromPath(Path vertexPath, Path fragmentPath) {
        if (!OpenGlHelper.shadersSupported) {
            return 0;
        }

        final int program = GL20.glCreateProgram();

        final int vertShader = loadAndCompileShader(program, vertexPath, GL20.GL_VERTEX_SHADER);
        final int fragShader = loadAndCompileShader(program, fragmentPath, GL20.GL_FRAGMENT_SHADER);

        return linkValidateShader(program, vertShader, fragShader);
    }

    // ONLY WORKS IN DEV ENV
    private static int loadAndCompileShader(int program, Path path, int shaderType) {
        final int shader = GL20.glCreateShader(shaderType);

        final String code = loadFileFromPath(path);
        if (code == null) {
            GL20.glDeleteShader(shader);
            return 0;
        }

        GL20.glShaderSource(shader, code);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GTNHLib.LOG.error("Could not compile shader {}: {}", path.toString(), getShaderLogInfo(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private static String loadFile(ResourceLocation resourceLocation) {
        try {
            final StringBuilder code = new StringBuilder();
            final InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation)
                    .getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line);
                code.append('\n');
            }
            reader.close();

            return code.toString();
        } catch (Exception e) {
            GTNHLib.LOG.error("Could not load shader file!", e);
        }

        return null;
    }

    private static String loadFileFromPath(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            StringBuilder code = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line).append('\n');
            }
            return code.toString();
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

    public int getUniformLocation(String name) {
        final int index = GL20.glGetUniformLocation(this.program, name);

        if (index < 0) {
            throw new NullPointerException("No uniform exists with name: " + name);
        }

        return index;
    }

    public int getAttribLocation(String name) {
        final int index = GL20.glGetAttribLocation(this.program, name);

        if (index < 0) {
            throw new NullPointerException("No attribute exists with name: " + name);
        }

        return index;
    }

    public void bindTextureSlot(String sampler2DName, int index) {
        GL20.glUniform1i(this.getUniformLocation(sampler2DName), index);
    }

    public void close() {
        GL20.glDeleteProgram(program);
    }
}
