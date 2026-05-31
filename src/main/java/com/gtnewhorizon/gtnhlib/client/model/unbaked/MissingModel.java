package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.util.ArrayList;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Face;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

public class MissingModel extends JSONModel {

    private static final Object2ObjectArrayMap<String, String> TEXTURES = new Object2ObjectArrayMap<>();
    static {
        TEXTURES.put("missingno", "minecraft:missingno");
    }

    /// This has to be after [#TEXTURES] to make sure the former is initialized.
    public static final MissingModel MISSING_MODEL = new MissingModel();

    public MissingModel() {
        super(null, true, null, TEXTURES, new ArrayList<>());

        final var uv = new Vector4f(0, 0, 16, 16);
        final var faces = new ArrayList<Face>();
        faces.add(new Face(DOWN, uv, "missingno", DOWN, 0, 0));
        faces.add(new Face(UP, uv, "missingno", UP, 0, 0));
        faces.add(new Face(WEST, uv, "missingno", WEST, 0, 0));
        faces.add(new Face(EAST, uv, "missingno", EAST, 0, 0));
        faces.add(new Face(NORTH, uv, "missingno", NORTH, 0, 0));
        faces.add(new Face(SOUTH, uv, "missingno", SOUTH, 0, 0));

        elements.add(new ModelElement("cube", new Vector3f(), new Vector3f(1, 1, 1), NOOP, true, 0, faces));
    }
}
