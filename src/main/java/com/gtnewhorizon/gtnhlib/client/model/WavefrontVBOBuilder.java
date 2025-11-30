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
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.TextureCoordinate;
import net.minecraftforge.client.model.obj.Vertex;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *  Wavefront Object importer
 *  Based heavily off of the specifications found at http://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
public final class WavefrontVBOBuilder {
    private static Pattern vertexPattern = Pattern.compile("^v( -?\\d+(?:\\.\\d+)?){3,4}$");
    private static Pattern textureCoordinatePattern = Pattern.compile("^vt( -?\\d+(?:\\.\\d+)?){2,3}$");
    private static Pattern face_V_VT_VN_Pattern = Pattern.compile("^f( \\d+/\\d+/\\d+){3,4}$");
    private static Pattern face_V_VT_Pattern = Pattern.compile("^f( \\d+/\\d+){3,4}$");
    private static Pattern face_V_VN_Pattern = Pattern.compile("^f( \\d+//\\d+){3,4}$");
    private static Pattern face_V_Pattern = Pattern.compile("^f( \\d+){3,4}$");

    public ArrayList<Vertex> vertices = new ArrayList<>();
    public ArrayList<TextureCoordinate> textureCoordinates = new ArrayList<>();
    private final String fileName;
    public final VertexBuffer vao;

    private final Vector3f tempVec1 = new Vector3f();
    private final Vector3f tempVec2 = new Vector3f();
    private final Vector3f faceNormal = new Vector3f();

    private boolean hasNormals;
    private boolean hasTex;

    private boolean isDrawing;

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

            while ((currentLine = reader.readLine()) != null) {
                lineCount++;

                if (currentLine.isEmpty()) continue;
                currentLine = normalizeWhitespace(currentLine, sb);
                if (currentLine.isEmpty()) continue;

                final char first = currentLine.charAt(0);


                if (first == 'v') {
                    if (currentLine.length() < 3) continue;
                    final char second = currentLine.charAt(1);
                    if (second == ' ') {
                        Vertex vertex = parseVertex(currentLine, lineCount);
                        if (vertex != null) {
                            vertices.add(vertex);
                        }
                    } else if (second == 't') {
                        if (hasTex) {
                            TextureCoordinate textureCoordinate = parseTextureCoordinate(currentLine, lineCount);
                            if (textureCoordinate != null) {
                                textureCoordinates.add(textureCoordinate);
                            }
                        }
                    }
                    // vn is never used
                    continue;
                }

                if (first == 'f') {
                    tessellateFace(tessellator, currentLine, lineCount);
                }
            }
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // hush
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                // hush
            }
        }

        return TessellatorManager.stopCapturingToVAO(format);

    }

    private Vertex parseVertex(String line, int lineCount) throws ModelFormatException {
        Vertex vertex = null;

        if (isValidVertexLine(line)) {
            line = line.substring(line.indexOf(' ') + 1);
            String[] tokens = line.split(" ");

            try {
                if (tokens.length == 2) {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
                } else if (tokens.length == 3) {
                    return new Vertex(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
                }
            } catch (NumberFormatException e) {
                throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
            }
        } else {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return vertex;
    }

    private TextureCoordinate parseTextureCoordinate(String line, int lineCount) throws ModelFormatException {
        TextureCoordinate textureCoordinate = null;

        if (isValidTextureCoordinateLine(line)) {
            line = line.substring(line.indexOf(' ') + 1);
            String[] tokens = line.split(" ");

            try {
                if (tokens.length == 2)
                    return new TextureCoordinate(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]));
                else if (tokens.length == 3)
                    return new TextureCoordinate(Float.parseFloat(tokens[0]), 1 - Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]));
            } catch (NumberFormatException e) {
                throw new ModelFormatException(String.format("Number formatting error at line %d", lineCount), e);
            }
        } else {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        return textureCoordinate;
    }

    private void tessellateFace(CapturingTessellator tessellator, String line, int lineCount) throws ModelFormatException {

        String trimmedLine = line.substring(line.indexOf(' ') + 1);
        String[] tokens = trimmedLine.split(" ");

        if (tokens.length == 3) {
            if (!isDrawing) {
                TessellatorManager.get().startDrawing(GL11.GL_TRIANGLES);
                isDrawing = true;
            }
        } else if (tokens.length == 4) {
            if (!isDrawing) {
                TessellatorManager.get().startDrawing(GL11.GL_QUADS);
                isDrawing = true;
            }
        } else {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        final Vertex[] vertices = new Vertex[tokens.length];
        final TextureCoordinate[] textureCoordinates = new TextureCoordinate[tokens.length];

        // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
        if (isValidFace_V_VT_VN_Line(line)) {
            for (int i = 0; i < tokens.length; ++i) {
                String[] subTokens = tokens[i].split("/");

                vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                if (hasTex) {
                    textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                }
            }
        }
        // f v1/vt1 v2/vt2 v3/vt3 ...
        else if (isValidFace_V_VT_Line(line)) {
            for (int i = 0; i < tokens.length; ++i) {
                String[] subTokens = tokens[i].split("/");

                vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
                if (hasTex) {
                    textureCoordinates[i] = this.textureCoordinates.get(Integer.parseInt(subTokens[1]) - 1);
                }
            }
        }
        // f v1//vn1 v2//vn2 v3//vn3 ...
        else if (isValidFace_V_VN_Line(line)) {
            for (int i = 0; i < tokens.length; ++i) {
                String[] subTokens = tokens[i].split("//");

                vertices[i] = this.vertices.get(Integer.parseInt(subTokens[0]) - 1);
            }
        }
        // f v1 v2 v3 ...
        else if (isValidFace_V_Line(line)) {
            for (int i = 0; i < tokens.length; ++i) {
                vertices[i] = this.vertices.get(Integer.parseInt(tokens[i]) - 1);
            }
        } else {
            throw new ModelFormatException("Error parsing entry ('" + line + "'" + ", line " + lineCount + ") in file '" + fileName + "' - Incorrect format");
        }

        if (hasNormals) {
            // Calculate face normal
            tempVec1.set(vertices[1].x - vertices[0].x, vertices[1].y - vertices[0].y, vertices[1].z - vertices[0].z);
            tempVec2.set(vertices[2].x - vertices[0].x, vertices[2].y - vertices[0].y, vertices[2].z - vertices[0].z);
            tempVec2.cross(tempVec1, faceNormal).normalize();

            tessellator.setNormal(faceNormal.x, faceNormal.y, faceNormal.z);
        }

        if (hasTex) {
            final float textureOffset = 0.0005F;

            float averageU = 0F;
            float averageV = 0F;

            for (TextureCoordinate textureCoordinate : textureCoordinates) {
                averageU += textureCoordinate.u;
                averageV += textureCoordinate.v;
            }

            averageU = averageU / textureCoordinates.length;
            averageV = averageV / textureCoordinates.length;

            for (int i = 0; i < vertices.length; ++i) {

                float offsetU = textureOffset;
                float offsetV = textureOffset;

                if (textureCoordinates[i].u > averageU) {
                    offsetU = -offsetU;
                }
                if (textureCoordinates[i].v > averageV) {
                    offsetV = -offsetV;
                }

                tessellator.addVertexWithUV(vertices[i].x, vertices[i].y, vertices[i].z, textureCoordinates[i].u + offsetU, textureCoordinates[i].v + offsetV);
            }
        } else {
            for (Vertex vertex : vertices) {
                tessellator.addVertex(vertex.x, vertex.y, vertex.z);
            }
        }
    }

    /***
     * Verifies that the given line from the model file is a valid vertex
     * @param line the line being validated
     * @return true if the line is a valid vertex, false otherwise
     */
    private static boolean isValidVertexLine(String line) {
        return vertexPattern.matcher(line).matches();
    }

    /***
     * Verifies that the given line from the model file is a valid texture coordinate
     * @param line the line being validated
     * @return true if the line is a valid texture coordinate, false otherwise
     */
    private static boolean isValidTextureCoordinateLine(String line) {
        return textureCoordinatePattern.matcher(line).matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices, texture coordinates, and vertex normals
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1/vt1/vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_VT_VN_Line(String line) {
        return face_V_VT_VN_Pattern.matcher(line).matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices and texture coordinates
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1/vt1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_VT_Line(String line) {
        return face_V_VT_Pattern.matcher(line).matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by vertices and vertex normals
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1//vn1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_VN_Line(String line) {
        return face_V_VN_Pattern.matcher(line).matches();
    }

    /***
     * Verifies that the given line from the model file is a valid face that is described by only vertices
     * @param line the line being validated
     * @return true if the line is a valid face that matches the format "f v1 ..." (with a minimum of 3 points in the face, and a maximum of 4), false otherwise
     */
    private static boolean isValidFace_V_Line(String line) {
        return  face_V_Pattern.matcher(line).matches();
    }

    private static String normalizeWhitespace(String input, StringBuilder output) {
        output.setLength(0); // Reuse buffer
        boolean inSpace = false;
        boolean seenNonSpace = false;

        final int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);

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
        int resultLength = output.length();
        if (resultLength > 0 && output.charAt(resultLength - 1) == ' ') {
            output.setLength(resultLength - 1);
        }

        return output.toString();
    }
}
