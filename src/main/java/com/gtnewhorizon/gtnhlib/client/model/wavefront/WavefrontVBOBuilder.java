
package com.gtnewhorizon.gtnhlib.client.model.wavefront;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * Wavefront Object importer Based heavily off of the specifications found at
 * http://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
public final class WavefrontVBOBuilder {

    private final List<Vector3f> vertices = new ArrayList<>(64);
    private final List<Vector2f> textureCoordinates = new ArrayList<>(64);
    private final IVertexArrayObject vao;

    static final int FORMAT_V_VT_VN = 0;
    static final int FORMAT_V_VT = 1;
    static final int FORMAT_V_VN = 2;
    static final int FORMAT_V = 3;

    private int format = -1;
    private Vector3f[] vertexArr;
    private Vector2f[] texArr;

    private final String[] tokenBuffer = new String[4];
    private final String[] subTokenBuffer = new String[3];

    private boolean hasNormals;
    private boolean hasTex;

    public static IVertexArrayObject compileToVBO(ResourceLocation resource) {
        return compileToVBO(resource, DefaultVertexFormat.POSITION_TEXTURE_NORMAL);
    }

    public static IVertexArrayObject compileToVBO(ResourceLocation resource, VertexFormat format) {
        return new WavefrontVBOBuilder(resource, format).vao;
    }

    private WavefrontVBOBuilder(ResourceLocation resource, VertexFormat format) throws ModelFormatException {
        try {
            final IResource res = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            vao = loadObjModel(res.getInputStream(), format);
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        }
    }

    private IVertexArrayObject loadObjModel(InputStream inputStream, VertexFormat format) throws ModelFormatException {
        hasTex = format.hasTexture();
        hasNormals = format.hasNormals();

        final DirectTessellator tessellator = DirectTessellator.startCapturing();

        BufferedReader reader = null;

        String currentLine;
        int lineCount = 0;

        try {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(inputStream));

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
                        vertices.add(parseVertex(currentLine, lineCount, tokenBuffer));
                    } else if (second == 't') {
                        if (hasTex) {
                            textureCoordinates.add(parseTextureCoordinate(currentLine, lineCount, tokenBuffer));
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

        return DirectTessellator.stopCapturingToVBO(VertexBufferType.IMMUTABLE);
    }

    static Vector3f parseVertex(final String line, final int lineCount, final String[] tokens)
            throws ModelFormatException {
        final int length = fastSplit(line, ' ', 2, tokens);

        try {
            if (length == 3) {
                return new Vector3f(
                        Float.parseFloat(tokens[0]),
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2]));
            } else {
                return new Vector3f(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), 0);

            }
        } catch (NumberFormatException e) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
        }
    }

    static Vector3f parseNormal(final String line, final int lineCount, final String[] tokens)
            throws ModelFormatException {
        fastSplit(line, ' ', 3, tokens);

        try {
            return new Vector3f(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
        } catch (NumberFormatException e) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
        }
    }

    static Vector2f parseTextureCoordinate(final String line, final int lineCount, final String[] tokens)
            throws ModelFormatException {
        fastSplit(line, ' ', 3, tokens);

        try {
            return new Vector2f(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]));
        } catch (NumberFormatException e) {
            throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
        }
    }

    static String parseGroup(final String line) throws ModelFormatException {
        return line.substring(line.indexOf(' ') + 1);
    }

    private void tessellateFace(final Tessellator tessellator, final String line, final int lineCount)
            throws ModelFormatException {
        final String[] tokens = tokenBuffer;
        final int length = fastSplit(line, ' ', 2, tokens);

        // Start rendering & figure out the format
        if (format == -1) {
            if (length == 3) {
                tessellator.startDrawing(GL11.GL_TRIANGLES);
            } else if (length == 4) {
                tessellator.startDrawing(GL11.GL_QUADS);
            } else {
                throw new ModelFormatException(
                        "Error parsing entry ('" + line + "'" + ", line " + lineCount + ") - Incorrect format");
            }
            this.vertexArr = new Vector3f[length];
            this.texArr = new Vector2f[length];
            format = determineFormat(tokens[0]);
        }

        final Vector3f[] vertices = this.vertexArr;
        final Vector2f[] textureCoordinates = this.texArr;

        switch (format) {
            case FORMAT_V_VT_VN, FORMAT_V_VT -> {
                // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
                // OR f v1/vt1 v2/vt2 v3/vt3 ...
                // Forge's WavefrontObject ignores the normals and calculates them, so they are both treated the same
                String[] subTokens = subTokenBuffer;
                if (hasTex) {
                    for (int i = 0; i < length; i++) {
                        fastSplit(tokens[i], '/', 0, subTokens);

                        vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                        textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                    }
                } else {
                    for (int i = 0; i < length; i++) {
                        fastSplit(tokens[i], '/', 0, subTokens);

                        vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
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
                for (int i = 0; i < length; i++) {
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

            final float scalar = 1.0f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
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

                final float offsetU = averageU >= tex.x ? textureOffset : -textureOffset;
                final float offsetV = averageV >= tex.y ? textureOffset : -textureOffset;

                tessellator
                        .addVertexWithUV(vertices[i].x, vertices[i].y, vertices[i].z, tex.x + offsetU, tex.y + offsetV);
            }
        } else {
            for (Vector3f vertex : vertices) {
                tessellator.addVertex(vertex.x, vertex.y, vertex.z);
            }
        }
    }

    /**
     * Fast method to split a string with minimal object allocations
     *
     * @return the amount of splits
     */
    static int fastSplit(final String s, final char sep, int first, final String[] out) {
        int count = 0;
        int start = first;
        for (; first < s.length(); first++) {
            if (s.charAt(first) == sep) {
                out[count++] = s.substring(start, first);
                start = first + 1;
            }
        }
        out[count] = s.substring(start);
        return count + 1;
    }

    static void normalizeWhitespace(String input, StringBuilder output) {
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
        final int resultLength = output.length() - 1;
        if (output.charAt(resultLength) == ' ') {
            output.setLength(resultLength);
        }
    }

    // "4/2/1" or "4/2" or "4//1" or "4"
    static int determineFormat(String token) {
        int slashCount = 0;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == '/') {
                slashCount++;
            }
        }

        if (slashCount == 0) {
            return FORMAT_V;
        } else if (slashCount == 1) {
            return FORMAT_V_VT;
        } else {
            if (token.contains("//")) {
                return FORMAT_V_VN;
            } else {
                return FORMAT_V_VT_VN;
            }
        }
    }
}
