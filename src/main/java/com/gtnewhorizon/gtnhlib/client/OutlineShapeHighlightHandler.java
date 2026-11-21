package com.gtnewhorizon.gtnhlib.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.api.IBlockWithOutlineShape;
import com.gtnewhorizon.gtnhlib.api.OutlineShape;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.doubles.DoubleList;

/// Draws the selection outline of an [IBlockWithOutlineShape] as one merged shape.
@EventBusSubscriber(side = Side.CLIENT)
public class OutlineShapeHighlightHandler {

    /// Pulls the outline slightly towards the camera, so it doesn't flicker against the faces.
    private static final double TOWARDS_CAMERA = 0.999;

    /// The last shape outlined, so an identical new one reuses its lines.
    private static OutlineShape lastShape = OutlineShape.EMPTY;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        MovingObjectPosition target = event.target;
        if (event.subID != 0 || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        EntityPlayer player = event.player;
        World world = player.worldObj;
        Block block = world.getBlock(target.blockX, target.blockY, target.blockZ);
        if (!(block instanceof IBlockWithOutlineShape shapedBlock)) return;

        OutlineShape shape = shapedBlock.getOutlineShape(world, target.blockX, target.blockY, target.blockZ);
        // Nothing to outline: leave it to vanilla.
        if (shape == null || shape.isEmpty()) return;
        if (!shape.hasSameBoxes(lastShape)) lastShape = shape;
        DoubleList lines = lastShape.getLines();
        if (lines.isEmpty()) return;

        float partialTicks = event.partialTicks;
        double originX = target.blockX - (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks);
        double originY = target.blockY - (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks);
        double originZ = target.blockZ - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);

        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        for (int i = 0; i < lines.size(); i += 3) {
            tessellator.addVertex(
                    (originX + lines.getDouble(i)) * TOWARDS_CAMERA,
                    (originY + lines.getDouble(i + 1)) * TOWARDS_CAMERA,
                    (originZ + lines.getDouble(i + 2)) * TOWARDS_CAMERA);
        }
        tessellator.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        event.setCanceled(true);
    }
}
