package com.gtnewhorizon.gtnhlib.transformation;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.transformation.TransformationRequestPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Gateway class for transformations. Use methods in this class to register your transformations and to request/perform
 * player transformations.
 */
public class TransformationController {

    private static final Map<String, ITransformation> TRANSFORMATION_MAP = new Object2ObjectOpenHashMap<>();

    /**
     * Get a registered transformation
     *
     * @param id The ID of the transformation to get
     * @return the transformation, or null if it cannot be found
     */
    @Nullable
    public static ITransformation getTransformationById(String id) {
        return TRANSFORMATION_MAP.get(id);
    }

    /**
     * Registers the transformation with the game. {@link ITransformation#getId() The transformation's ID} must be
     * unique. It is recommended to prefix the ID with your mod's name to avoid collisions.
     *
     * @param transformation The new transformation to add
     */
    @SuppressWarnings("unused")
    public static void registerTransformation(@NotNull ITransformation transformation) {
        final String id = transformation.getId();
        if (TRANSFORMATION_MAP.containsKey(id)) {
            GTNHLib.LOG.error(
                    "Transformation ID {} is already registered. It is recommended to prefix transformation IDs with your mod ID.",
                    id);
        } else {
            TRANSFORMATION_MAP.put(id, transformation);
        }
    }

    /**
     * Convenience method for the client to request a transformation.
     *
     * @param transformation The new form to assume
     */
    @SuppressWarnings("unused")
    @SideOnly(Side.CLIENT)
    public static void transformRequest(final ITransformation transformation) {
        NetworkHandler.instance
                .sendToServer(new TransformationRequestPacket(transformation.getId(), TransformationAction.TRANSFORM));
    }

    /**
     * Convenience method for the client to request to untransform.
     */
    @SuppressWarnings("unused")
    @SideOnly(Side.CLIENT)
    public static void untransformRequest() {
        NetworkHandler.instance.sendToServer(TransformationRequestPacket.getUntransformPacket());
    }

    /**
     * Convenience method for the client to request to toggle their transformation.
     *
     * @param transformation The form to toggle on and off
     */
    @SuppressWarnings("unused")
    @SideOnly(Side.CLIENT)
    public static void toggleRequest(final ITransformation transformation) {
        NetworkHandler.instance
                .sendToServer(new TransformationRequestPacket(transformation.getId(), TransformationAction.TOGGLE));
    }

    /**
     * Convenience method for the server to announce a transformation. This method will send a server-to-client packet
     * to announce the transformation.
     *
     * @param player         The player to transform
     * @param transformation The new transformation to assume
     */
    @SuppressWarnings("unused")
    @SideOnly(Side.SERVER)
    public static void transformServerSide(@NotNull final EntityPlayerMP player,
            @Nullable final ITransformation transformation) {
        final TransformationProperty prop = TransformationProperty.get(player);
        if (prop != null) {
            prop.setTransformation(transformation);
        }
    }
}
