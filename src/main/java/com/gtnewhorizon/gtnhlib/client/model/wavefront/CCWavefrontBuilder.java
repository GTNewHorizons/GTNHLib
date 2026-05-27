package com.gtnewhorizon.gtnhlib.client.model.wavefront;

import static com.gtnewhorizon.gtnhlib.client.model.wavefront.WavefrontVBOBuilder.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.uv.UV;
import codechicken.lib.render.uv.UVTransformation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;

// Not fully implemented, currently lacking compat for color/lighting and non-baked transforms
public final class CCWavefrontBuilder {

    private final ResourceLocation wavefront;
    private final List<Consumer<Vector3>> vertexTransforms = new ArrayList<>();
    private final List<Consumer<UV>> uvTransforms = new ArrayList<>();
    private final List<Consumer<Vector3>> normalTransforms = new ArrayList<>();

    private final boolean isQuads;

    private String currentGroupObject;
    private CCBakedModel currentModel;

    private final String[] tokenBuffer = new String[4];
    private final String[] subTokenBuffer = new String[3];

    public CCWavefrontBuilder(ResourceLocation location, boolean isQuads, Transformation coordSystem) {
        this.wavefront = location;
        this.isQuads = isQuads;
        if (coordSystem != null) addTransform(coordSystem);
    }

    public CCWavefrontBuilder(ResourceLocation location, int drawMode, Transformation coordSystem) {
        this(location, drawMode == GL11.GL_QUADS, coordSystem);
    }

    public CCWavefrontBuilder addTransform(CCRenderState.IVertexOperation operation) {
        if (operation instanceof Transformation transformation) {
            vertexTransforms.add(transformation::apply);
            normalTransforms.add(transformation::applyN);
        } else if (operation instanceof UVTransformation transformation) {
            uvTransforms.add(transformation::apply);
        } else {
            throw new UnsupportedOperationException(
                    "IVertexOperation " + operation.getClass().getName() + " not supported!");
        }
        return this;
    }

    public Map<String, CCBakedModel> build() {
        BufferedReader reader = null;

        String currentLine;
        int lineCount = 0;

        final Map<String, CCBakedModel> groups = new HashMap<>();

        try (InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(wavefront)
                .getInputStream()) {

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
                        vertices.add(transformVertex(parseVertex(currentLine, lineCount, tokenBuffer)));
                    } else if (second == 't') {
                        texCoords.add(transformUV(parseTextureCoordinate(currentLine, lineCount, tokenBuffer)));
                    } else if (second == 'n') {
                        normals.add(transformNormal(parseNormal(currentLine, lineCount, tokenBuffer)));
                    }
                } else if (first == 'f') {
                    tessellateFace(currentLine, lineCount);
                } else if (first == 'g') {
                    String group = parseGroup(currentLine);
                    if (!group.equals(currentGroupObject)) {
                        currentModel = new CCBakedModel(isQuads);
                        currentGroupObject = group;
                        groups.put(currentGroupObject, currentModel);
                    }
                }
            }
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {}
        }

        return groups;
    }

    private final List<Vector3f> vertices = new ArrayList<>(64);
    private final List<Vector2f> texCoords = new ArrayList<>(64);
    private final List<Vector3f> normals = new ArrayList<>(64);

    private final Vector3 tmp = new Vector3();
    private final UV tmpUV = new UV();

    private FaceConsumer faceConsumer;

    private void tessellateFace(String line, int lineCount) throws ModelFormatException {
        final String[] tokens = tokenBuffer;
        final int length = fastSplit(line, ' ', 2, tokens);

        // Figure out the format
        if (faceConsumer == null) {
            final int format = determineFormat(tokens[0]);
            switch (format) {
                case FORMAT_V_VT_VN -> faceConsumer = this::parseVTN;
                case FORMAT_V_VT -> faceConsumer = this::parseVT;
                case FORMAT_V_VN -> faceConsumer = this::parseVN;
                case FORMAT_V -> faceConsumer = this::parseV;
                default -> throw new IllegalStateException("Illegal format " + format + " at line " + lineCount);
            }
        }

        if (length < 3) throw new IllegalStateException(
                "Expected face with at least 3 sides; found " + length + " at line " + lineCount);

        if (isQuads) {
            if (length == 4) {
                faceConsumer.parse(tokens, 0);
                faceConsumer.parse(tokens, 3);
                faceConsumer.parse(tokens, 2);
                faceConsumer.parse(tokens, 1);
            } else { // length = 3
                faceConsumer.parse(tokens, 0);
                faceConsumer.parse(tokens, 2);
                faceConsumer.parse(tokens, 1);
                faceConsumer.parse(tokens, 1);
            }
        } else {
            faceConsumer.parse(tokens, 0);
            faceConsumer.parse(tokens, 2);
            faceConsumer.parse(tokens, 1);
            if (length == 4) {
                faceConsumer.parse(tokens, 0);
                faceConsumer.parse(tokens, 3);
                faceConsumer.parse(tokens, 2);
            }
        }
    }

    private void parseVTN(String[] tokens, int i) {
        fastSplit(tokens[i], '/', 0, subTokenBuffer);

        currentModel.vertices.add(this.vertices.get(Integer.parseInt(subTokenBuffer[0]) - 1));
        currentModel.texCoords.add(this.texCoords.get(Integer.parseInt(subTokenBuffer[1]) - 1));
        currentModel.normals.add(this.normals.get(Integer.parseInt(subTokenBuffer[2]) - 1));
    }

    private void parseVT(String[] tokens, int i) {
        fastSplit(tokens[i], '/', 0, subTokenBuffer);

        currentModel.vertices.add(this.vertices.get(Integer.parseInt(subTokenBuffer[0]) - 1));
        currentModel.texCoords.add(this.texCoords.get(Integer.parseInt(subTokenBuffer[1]) - 1));
    }

    private void parseVN(String[] tokens, int i) {
        final String token = tokens[i];
        final int index = token.indexOf("//");

        currentModel.vertices.add(this.vertices.get(Integer.parseInt(token.substring(0, index)) - 1));
        currentModel.normals.add(this.normals.get(Integer.parseInt(token.substring(index + 2)) - 1));
    }

    private void parseV(String[] tokens, int i) {
        currentModel.vertices.add(this.vertices.get(Integer.parseInt(tokens[i]) - 1));
    }

    private Vector3f transformVertex(Vector3f vector) {
        if (vertexTransforms.isEmpty()) return vector;

        tmp.x = vector.x;
        tmp.y = vector.y;
        tmp.z = vector.z;
        for (Consumer<Vector3> transformation : vertexTransforms) {
            transformation.accept(tmp);
        }
        vector.x = (float) tmp.x;
        vector.y = (float) tmp.y;
        vector.z = (float) tmp.z;

        return vector;
    }

    private Vector2f transformUV(Vector2f vector) {
        if (uvTransforms.isEmpty()) return vector;

        tmpUV.u = vector.x;
        tmpUV.v = vector.y;
        for (Consumer<UV> transformation : uvTransforms) {
            transformation.accept(tmpUV);
        }
        vector.x = (float) tmpUV.u;
        vector.y = (float) tmpUV.v;

        return vector;
    }

    private Vector3f transformNormal(Vector3f vector) {
        if (normalTransforms.isEmpty()) return vector;

        tmp.x = vector.x;
        tmp.y = vector.y;
        tmp.z = vector.z;
        for (Consumer<Vector3> transformation : normalTransforms) {
            transformation.accept(tmp);
        }
        vector.x = (float) tmp.x;
        vector.y = (float) tmp.y;
        vector.z = (float) tmp.z;

        return vector;
    }

    private interface FaceConsumer {

        void parse(String[] tokens, int i);
    }
}
