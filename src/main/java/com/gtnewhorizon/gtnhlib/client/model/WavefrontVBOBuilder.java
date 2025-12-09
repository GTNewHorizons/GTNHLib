package com.gtnewhorizon.gtnhlib.client.model;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.Vertex;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.joml.Math.fma;

/**
 *  Wavefront Object importer
 *  Based heavily off of the specifications found at http://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
public final class WavefrontVBOBuilder {
    public List<Vertex> vertices = new ArrayList<>();
    public List<Vector2f> textureCoordinates = new ArrayList<>();
    private final String fileName;
    public final VertexBuffer vao;

    private static final int FORMAT_V_VT_VN = 0;
    private static final int FORMAT_V_VT = 1;
    private static final int FORMAT_V_VN = 2;
    private static final int FORMAT_V = 3;

    private int format = -1;
    private Vertex[] vertexArr;
    private Vector2f[] texArr;

    private final String[] tokenBuffer = new String[4];
    private final String[] subTokenBuffer = new String[3];

    private boolean hasNormals;
    private boolean hasTex;

    public static VertexBuffer compileToVBO(ResourceLocation resource) {
        return compileToVBO(resource, DefaultVertexFormat.POSITION_TEXTURE_NORMAL);
    }


    public static VertexBuffer compileToVBO(ResourceLocation resource, VertexFormat format) {
        return new WavefrontVBOBuilder(resource, format).vao;
    }

    private WavefrontVBOBuilder(ResourceLocation resource, VertexFormat format) throws ModelFormatException {
        this.fileName = resource.toString();

        try {
            final IResource res = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            vao = loadObjModel(res.getInputStream(), format);
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        }
    }

    private VertexBuffer loadObjModel(InputStream inputStream, VertexFormat format) throws ModelFormatException {
        for (VertexFormatElement element : format.getElements()) {
            if (element == DefaultVertexFormat.TEXTURE_ELEMENT) {
                hasTex = true;
            } else if (element == DefaultVertexFormat.NORMAL_ELEMENT) {
                hasNormals = true;
            }
        }

        final CapturingTessellator tessellator = TessellatorManager.startCapturingAndGet();

        BufferedReader reader = null;

        String currentLine;
        int lineCount = 0;

        try {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            final String[] tokens = tokenBuffer;

            while ((currentLine = reader.readLine()) != null) {
                lineCount++;

                if (currentLine.isEmpty()) continue;
                normalizeWhitespace(currentLine, sb);
                if (sb.length() == 0) continue;
                currentLine = sb.toString();

                final char first = currentLine.charAt(0);

                if (first == 'v') {
                    final char second = currentLine.charAt(1);
                    if (second == ' ') {
                        vertices.add(parseVertex(currentLine, lineCount, tokens));
                    } else if (second == 't') {
                        if (hasTex) {
                            textureCoordinates.add(parseTextureCoordinate(currentLine, lineCount, tokens));
                        }
                    }
                    // vn is never used
                } else if (first == 'f') {
                    tessellateFace(tessellator, currentLine, lineCount);
                }
                // groups are not supported here; every group gets merged into 1.
            }
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {}

            try {
                inputStream.close();
            } catch (IOException ignored) {}
        }

        return TessellatorManager.stopCapturingToVAO(format);
    }

    private static Vertex parseVertex(String line, int lineCount, String[] tokens) throws ModelFormatException {
        final int length = fastSplit(line, ' ', 2, tokens);

        try {
            if (length == 3) {
                return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            } else {
                return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));

            }
        } catch (NumberFormatException e) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
        }
    }

    private static Vector2f parseTextureCoordinate(String line, int lineCount, String[] tokens) throws ModelFormatException {
        fastSplit(line, ' ', 3, tokens);

        try {
            return new Vector2f(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]));
        } catch (NumberFormatException e) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
        }
    }

    private void tessellateFace(CapturingTessellator tessellator, String line, int lineCount) throws ModelFormatException {
        final String[] tokens = tokenBuffer;
        final int length = fastSplit(line, ' ', 2, tokens);

        // Start rendering & figure out the format
        if (format == -1) {
            if (length == 3) {
                tessellator.startDrawing(GL11.GL_TRIANGLES);
            } else if (length == 4) {
                tessellator.startDrawing(GL11.GL_QUADS);
            } else {
                throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
            }
            this.vertexArr = new Vertex[length];
            this.texArr = new Vector2f[length];
            final String token = tokens[0]; // "4/2/1" or "4//1" or "4"
            int slashCount = 0;
            for (int i = 0; i < token.length(); i++) {
                if (token.charAt(i) == '/') {
                    slashCount++;
                }
            }

            if (slashCount == 0) {
                format = FORMAT_V;
            } else if (slashCount == 1) {
                format = FORMAT_V_VT;
            } else {
                if (token.contains("//")) {
                    format = FORMAT_V_VN;
                } else {
                    format = FORMAT_V_VT_VN;
                }
            }
        }


        final Vertex[] vertices = this.vertexArr;
        final Vector2f[] textureCoordinates = this.texArr;

        switch (format) {
            case FORMAT_V_VT_VN -> {
                // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
                String[] subTokens = subTokenBuffer;
                for (int i = 0; i < length; i++) {
                    fastSplit(tokens[i], '/', 0, subTokens);

                    vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    if (hasTex) {
                        textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                    }
                }
            }
            case FORMAT_V_VT -> {
                // f v1/vt1 v2/vt2 v3/vt3 ...
                String[] subTokens = subTokenBuffer;
                for (int i = 0; i < length; i++) {
                    fastSplit(tokens[i], '/', 0, subTokens);

                    vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                    if (hasTex) {
                        textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                    }
                }
            }
            case FORMAT_V_VN -> {
                // f v1//vn1 v2//vn2 v3//vn3 ...
                for (int i = 0; i < length; i++) {
                    final String token = tokens[i];
                    final int index = token.indexOf("//");

                    vertices[i] = this.vertices.get(Integer.parseInt(token.substring(0, index)) - 1);
                }
            }
            case FORMAT_V -> {
                // f v1 v2 v3 ...
                for (int i = 0; i < length; ++i) {
                    vertices[i] = this.vertices.get(Integer.parseInt(tokens[i]) - 1);
                }
            }
        }

        if (hasNormals) {
            final float x0 = vertices[1].x - vertices[0].x;
            final float y0 = vertices[1].y - vertices[0].y;
            final float z0 = vertices[1].z - vertices[0].z;
            final float x1 = vertices[2].x - vertices[0].x;
            final float y1 = vertices[2].y - vertices[0].y;
            final float z1 = vertices[2].z - vertices[0].z;

            final float nx = y0 * z1 - z0 * y1;
            final float ny = z0 * x1 - x0 * z1;
            final float nz = x0 * y1 - y0 * x1;

            final float scalar = 1.0f / (float) Math.sqrt(fma(nx, nx, fma(ny, ny, nz * nz)));
            tessellator.setNormal(nx * scalar, ny * scalar, nz * scalar);
        }

        if (hasTex) {
            final float textureOffset = 0.0005F;

            float averageU = 0F;
            float averageV = 0F;

            for (Vector2f textureCoordinate : textureCoordinates) {
                averageU += textureCoordinate.x;
                averageV += textureCoordinate.y;
            }

            averageU = averageU / textureCoordinates.length;
            averageV = averageV / textureCoordinates.length;

            for (int i = 0; i < vertices.length; i++) {
                final Vector2f tex = textureCoordinates[i];

                final float offsetU = textureOffset * Math.copySign(1f, averageU - tex.x);
                final float offsetV = textureOffset * Math.copySign(1f, averageV - tex.y);

                tessellator.addVertexWithUV(vertices[i].x, vertices[i].y, vertices[i].z, tex.x + offsetU, tex.y + offsetV);
            }
        } else {
            for (Vertex vertex : vertices) {
                tessellator.addVertex(vertex.x, vertex.y, vertex.z);
            }
        }
    }

    private static int fastSplit(String s, char sep, int i, String[] out) {
        int count = 0;
        int start = i;
        for (; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                out[count++] = s.substring(start, i);
                start = i + 1;
            }
        }
        out[count] = s.substring(start);
        return count + 1;
    }

    private static void normalizeWhitespace(String input, StringBuilder output) {
        output.setLength(0); // Reuse buffer
        boolean inSpace = false;
        boolean seenNonSpace = false;

        final int length = input.length();

        for (int i = 0; i < length; i++) {
            final char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                if (seenNonSpace && !inSpace) {
                    output.append(' ');
                    inSpace = true;
                }
            } else {
                output.append(c);
                inSpace = false;
                seenNonSpace = true;
            }
        }

        // Remove trailing space if present
        final int resultLength = output.length();
        if (resultLength > 0 && output.charAt(resultLength - 1) == ' ') {
            output.setLength(resultLength - 1);
        }
    }
}
