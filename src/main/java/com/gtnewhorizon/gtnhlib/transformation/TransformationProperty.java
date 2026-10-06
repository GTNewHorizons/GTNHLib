package com.gtnewhorizon.gtnhlib.transformation;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.api.transformation.TransformationRegistry;
import com.gtnewhorizon.gtnhlib.mixins.early.AccessorEntity;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.transformation.TransformationAnnouncePacket;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

public class TransformationProperty implements IExtendedEntityProperties {

    public static final String BASE_KEY = "TransformationProperty";
    public static final String TRANSFORM_ID_KEY = "TransformID";

    private final EntityPlayer player;
    @Getter
    private ITransformation transformation = null;

    @SideOnly(Side.CLIENT)
    private EntityLivingBase renderEntity;

    public TransformationProperty(final EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void saveNBTData(final NBTTagCompound compound) {
        final NBTTagCompound tag = new NBTTagCompound();
        if (transformation != null) {
            tag.setString(TRANSFORM_ID_KEY, transformation.getId());
        }
        compound.setTag(BASE_KEY, tag);
    }

    @Override
    public void loadNBTData(final NBTTagCompound compound) {
        final NBTTagCompound tag = compound.getCompoundTag(BASE_KEY);
        if (tag.hasKey(TRANSFORM_ID_KEY)) {
            final ITransformation form = TransformationRegistry.getTransformation(tag.getString(TRANSFORM_ID_KEY));
            if (form != null) {
                transformation = form;
            }
        }
    }

    @Override
    public void init(final Entity entity, final World world) {}

    @Nullable
    public static TransformationProperty get(@Nullable final EntityPlayer player) {
        if (player != null && player.getExtendedProperties(BASE_KEY) instanceof final TransformationProperty prop) {
            return prop;
        }

        return null;
    }

    @Nullable
    public static ITransformation getTransformation(@Nullable final EntityPlayer player) {
        final TransformationProperty prop = get(player);
        if (prop != null) {
            return prop.getTransformation();
        }

        return null;
    }

    public void setTransformation(final ITransformation newForm) {
        if (transformation == newForm) {
            return;
        }

        if (transformation != null) {
            transformation.onUntransform(player);
        }

        transformation = newForm;

        if (player.worldObj.isRemote && renderEntity != null) {
            renderEntity.setDead();
            renderEntity.worldObj = null;
            renderEntity = null;
        }

        if (transformation != null) {
            transformation.onTransform(player);
            updatePlayerSize(transformation.getWidth(), transformation.getHeight());
        } else {
            updatePlayerSize(ITransformation.DEFAULT_WIDTH, ITransformation.DEFAULT_HEIGHT);
        }

        syncPlayerTransformation(player);
    }

    public void removeTransformation() {
        setTransformation(null);
    }

    @SideOnly(Side.CLIENT)
    public EntityLivingBase getRenderEntity() {
        if (transformation != null) {
            if (renderEntity == null) {
                renderEntity = transformation.getRenderEntity(player);
                if (renderEntity instanceof final EntityLiving living) {
                    living.tasks.taskEntries.clear();
                }
            }

            return renderEntity;
        }

        return null;
    }

    public void updatePlayerSize(final float width, final float height) {
        if (player.width != width && player.height != height) {
            ((AccessorEntity) player).invokeSetSize(width, height);

            final float halfWidth = width / 2.0f;
            player.boundingBox.minX = player.posX - halfWidth;
            player.boundingBox.maxX = player.posX + halfWidth;
            player.boundingBox.minZ = player.posZ - halfWidth;
            player.boundingBox.maxZ = player.posZ + halfWidth;

            player.boundingBox.maxY = player.boundingBox.minY + height;
        }
    }

    private static void syncPlayerTransformation(EntityPlayer player) {
        if (!player.worldObj.isRemote) {
            if (player.getExtendedProperties(BASE_KEY) instanceof final TransformationProperty prop) {
                final String id = prop.getTransformation() != null ? prop.getTransformation().getId() : "";
                final IPacket packet = new TransformationAnnouncePacket(player.getEntityId(), id);

                NetworkHandler.instance.sendToAllAround(packet, new NetworkRegistry.TargetPoint(
                    player.dimension,
                    player.posX,
                    player.posY,
                    player.posZ,
                    256d
                ));
            }
        }
    }
}
