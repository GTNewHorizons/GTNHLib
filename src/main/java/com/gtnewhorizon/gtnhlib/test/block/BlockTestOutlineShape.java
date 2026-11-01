package com.gtnewhorizon.gtnhlib.test.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.api.IBlockWithOutlineShape;
import com.gtnewhorizon.gtnhlib.api.OutlineShape;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTestOutlineShape extends Block implements IBlockWithOutlineShape {

    private static int renderId;

    /// Drawn and collided with. The outline comes from the collision unless a subclass overrides it.
    private final List<AxisAlignedBB> boxes;
    private final int color;

    private BlockTestOutlineShape(List<AxisAlignedBB> boxes, int color) {
        super(Material.rock);
        this.boxes = boxes;
        this.color = color;
        setHardness(0.5F);
        setBlockTextureName("gtnhlib:white");
    }

    @SideOnly(Side.CLIENT)
    public static void registerRenderer() {
        renderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new Renderer());
    }

    public static void register() {
        // Flush boxes forming a cube: no seams.
        register("flush_halves", 0xE53935, px(0, 0, 0, 8, 16, 16), px(8, 0, 0, 16, 16, 16));
        // Boxes sharing only an edge, or a corner.
        register("diagonal_edge", 0xFDD835, px(0, 0, 0, 8, 8, 16), px(8, 8, 0, 16, 16, 16));
        register("corner_touch", 0x7CB342, px(0, 0, 0, 8, 8, 8), px(8, 8, 8, 16, 16, 16));
        // Overlapping boxes.
        register("cross", 0x00897B, px(6, 6, 0, 10, 10, 16), px(0, 6, 6, 16, 10, 10), px(6, 0, 6, 10, 16, 10));
        register("nested", 0x3949AB, px(0, 0, 0, 16, 16, 16), px(4, 4, 4, 12, 12, 12));
        // A ring with a hole through it.
        register(
                "frame",
                0xD81B60,
                px(0, 0, 0, 16, 16, 4),
                px(0, 0, 12, 16, 16, 16),
                px(0, 0, 4, 4, 16, 12),
                px(12, 0, 4, 16, 16, 12));
        // A hopper: the bowl's floor is a plane, hiding the middle piece's top edges.
        register(
                "hopper",
                0x757575,
                px(0, 10, 0, 2, 16, 16),
                px(14, 10, 0, 16, 16, 16),
                px(2, 10, 0, 14, 16, 2),
                px(2, 10, 14, 14, 16, 16),
                px(2, 10, 2, 14, 10, 14),
                px(4, 4, 4, 12, 10, 12),
                px(6, 0, 6, 10, 4, 10));
        // Degenerate sizes.
        register("zero_plate", 0x6D4C41, px(0, 8, 0, 16, 8, 16), px(7, 0, 7, 9, 8, 9));
        register("tiny", 0x546E7A, box(0.5, 0.5, 0.5, 0.505, 0.505, 0.505));
        // Many boxes: a checkerboard and a pixel sphere.
        register("checker", 0x1E88E5, checkerboard());
        register("sphere", 0xEC407A, sphere());
        // Grid lines that differ only by float rounding.
        register("precision", 0x5E35B1, box(0, 0, 0, 1.0 / 3.0, 1, 1), box((float) (1.0 / 3.0), 0, 0, 1, 0.5, 1));
        // No shape: vanilla's outline of the block's bounds.
        register("empty", 0xFFFFFF, Collections.emptyList(), OutlineShape.EMPTY).setBlockBounds(0, 0, 0, 1, 0.5F, 1);
        // Slopes, collided with as one-pixel steps.
        registerSloped("slope", 0x8E24AA, steps(0, i -> px(0, i, i, 16, i + 1, 16)), wedge(0));
        // A box and a slope as separate pieces: their shared faces cancel.
        List<AxisAlignedBB> slopeOnSlab = steps(8, i -> px(0, i, 2 * (i - 8), 16, i + 1, 16));
        slopeOnSlab.add(px(0, 0, 0, 16, 8, 16));
        List<double[]> slopeOnSlabFaces = new ArrayList<>(OutlineShape.faces(px(0, 0, 0, 16, 8, 16)));
        slopeOnSlabFaces.addAll(wedge(0.5));
        registerSloped("slope_on_slab", 0x43A047, slopeOnSlab, slopeOnSlabFaces);
        registerSloped(
                "pyramid",
                0x039BE5,
                steps(0, i -> px(i / 2.0, i, i / 2.0, 16 - i / 2.0, i + 1, 16 - i / 2.0)),
                Arrays.asList(
                        new double[] { 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1 },
                        new double[] { 0, 0, 0, 0.5, 1, 0.5, 1, 0, 0 },
                        new double[] { 1, 0, 0, 0.5, 1, 0.5, 1, 0, 1 },
                        new double[] { 1, 0, 1, 0.5, 1, 0.5, 0, 0, 1 },
                        new double[] { 0, 0, 1, 0.5, 1, 0.5, 0, 0, 0 }));
    }

    private static BlockTestOutlineShape register(String name, int color, AxisAlignedBB... boxes) {
        return register(name, color, Arrays.asList(boxes));
    }

    private static BlockTestOutlineShape register(String name, int color, List<AxisAlignedBB> boxes) {
        return register(name, new BlockTestOutlineShape(boxes, color));
    }

    private static BlockTestOutlineShape register(String name, int color, List<AxisAlignedBB> boxes,
            OutlineShape shape) {
        return register(name, new WithShape(boxes, shape, null, color));
    }

    /// Drawn and outlined from `faces`, collided with as `boxes`.
    private static void registerSloped(String name, int color, List<AxisAlignedBB> boxes, List<double[]> faces) {
        register(name, new WithShape(boxes, OutlineShape.fromFaces(faces), faces, color));
    }

    private static BlockTestOutlineShape register(String name, BlockTestOutlineShape block) {
        block.setBlockName("outline_test_" + name);
        GameRegistry.registerBlock(block, "outline_test_" + name);
        return block;
    }

    private static List<AxisAlignedBB> checkerboard() {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    if ((i + j + k) % 2 == 0) boxes.add(px(i * 4, j * 4, k * 4, i * 4 + 4, j * 4 + 4, k * 4 + 4));
                }
            }
        }
        return boxes;
    }

    /// A sphere filling the block, one box per pixel row and layer.
    private static List<AxisAlignedBB> sphere() {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                double dy = y + 0.5 - 8, dz = z + 0.5 - 8;
                int half = halfWidth(Math.sqrt(dy * dy + dz * dz));
                if (half > 0) boxes.add(px(8 - half, y, z, 8 + half, y + 1, z + 1));
            }
        }
        return boxes;
    }

    /// A wedge across the block from height `b` to the top, rising towards the south.
    private static List<double[]> wedge(double b) {
        return Arrays.asList(
                new double[] { 0, b, 0, 1, b, 0, 1, b, 1, 0, b, 1 },
                new double[] { 0, b, 1, 1, b, 1, 1, 1, 1, 0, 1, 1 },
                new double[] { 0, b, 0, 0, 1, 1, 1, 1, 1, 1, b, 0 },
                new double[] { 0, b, 0, 0, b, 1, 0, 1, 1 },
                new double[] { 1, b, 0, 1, 1, 1, 1, b, 1 });
    }

    /// One box per pixel layer, from layer `from` to the top.
    private static List<AxisAlignedBB> steps(int from, IntFunction<AxisAlignedBB> layer) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (int i = from; i < 16; i++) boxes.add(layer.apply(i));
        return boxes;
    }

    /// Half-width in pixels of a radius-8 circle at `distance` from its centre.
    private static int halfWidth(double distance) {
        int half = 0;
        while (half < 8 && (half + 0.5) * (half + 0.5) + distance * distance <= 64) half++;
        return half;
    }

    private static AxisAlignedBB px(double x1, double y1, double z1, double x2, double y2, double z2) {
        return box(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return AxisAlignedBB.getBoundingBox(x1, y1, z1, x2, y2, z2);
    }

    /// Boxes to render and collide with; an empty shape is the block's bounds.
    private List<AxisAlignedBB> getBoxesOrBounds() {
        return boxes.isEmpty() ? Collections.singletonList(box(minX, minY, minZ, maxX, maxY, maxZ)) : boxes;
    }

    /// An outline other than the collision, built once.
    private static class WithShape extends BlockTestOutlineShape {

        private final OutlineShape shape;
        /// Drawn instead of the boxes, if not null.
        private final List<double[]> faces;

        private WithShape(List<AxisAlignedBB> boxes, OutlineShape shape, List<double[]> faces, int color) {
            super(boxes, color);
            this.shape = shape;
            this.faces = faces;
        }

        @Override
        public OutlineShape getOutlineShape(World world, int x, int y, int z) {
            return shape;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list,
            Entity entity) {
        for (AxisAlignedBB box : getBoxesOrBounds()) {
            AxisAlignedBB placed = box.getOffsetBoundingBox(x, y, z);
            if (mask.intersectsWith(placed)) list.add(placed);
        }
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return renderId;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return color;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return color;
    }

    @SideOnly(Side.CLIENT)
    private static class Renderer implements ISimpleBlockRenderingHandler {

        @Override
        public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
            int color = block.getRenderColor(meta);
            Tessellator tessellator = Tessellator.instance;
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            if (block instanceof WithShape sloped && sloped.faces != null) {
                tessellator.startDrawingQuads();
                renderFaces(sloped, block.getIcon(0, meta), 0, 0, 0);
                tessellator.draw();
                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                return;
            }
            for (AxisAlignedBB box : ((BlockTestOutlineShape) block).getBoxesOrBounds()) {
                renderer.setRenderBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                tessellator.startDrawingQuads();
                tessellator.setColorOpaque_I(color);
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                renderer.renderFaceYNeg(block, 0, 0, 0, block.getIcon(0, meta));
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                renderer.renderFaceYPos(block, 0, 0, 0, block.getIcon(1, meta));
                tessellator.setNormal(0.0F, 0.0F, -1.0F);
                renderer.renderFaceZNeg(block, 0, 0, 0, block.getIcon(2, meta));
                tessellator.setNormal(0.0F, 0.0F, 1.0F);
                renderer.renderFaceZPos(block, 0, 0, 0, block.getIcon(3, meta));
                tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                renderer.renderFaceXNeg(block, 0, 0, 0, block.getIcon(4, meta));
                tessellator.setNormal(1.0F, 0.0F, 0.0F);
                renderer.renderFaceXPos(block, 0, 0, 0, block.getIcon(5, meta));
                tessellator.draw();
            }
            renderer.setRenderBoundsFromBlock(block);
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        }

        @Override
        public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
                RenderBlocks renderer) {
            if (block instanceof WithShape sloped && sloped.faces != null) {
                Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
                // The breaking cracks are drawn by rendering again with an override texture.
                IIcon icon = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture
                        : renderer.getBlockIconFromSide(block, 0);
                renderFaces(sloped, icon, x, y, z);
                return true;
            }
            boolean renderAllFaces = renderer.renderAllFaces;
            renderer.renderAllFaces = true;
            for (AxisAlignedBB box : ((BlockTestOutlineShape) block).getBoxesOrBounds()) {
                renderer.setRenderBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                renderer.renderStandardBlock(block, x, y, z);
            }
            renderer.setRenderBoundsFromBlock(block);
            renderer.renderAllFaces = renderAllFaces;
            return true;
        }

        /// Each face split into quads by repeating a corner, and shaded by which way it points.
        private static void renderFaces(WithShape block, IIcon icon, int x, int y, int z) {
            Tessellator tessellator = Tessellator.instance;
            int color = block.getRenderColor(0);
            float r = (color >> 16 & 255) / 255F, g = (color >> 8 & 255) / 255F, b = (color & 255) / 255F;
            for (double[] face : block.faces) {
                double ax = face[3] - face[0], ay = face[4] - face[1], az = face[5] - face[2];
                double bx = face[6] - face[3], by = face[7] - face[4], bz = face[8] - face[5];
                double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                nx /= length;
                ny /= length;
                nz /= length;
                float shade = (float) (0.6 * nx * nx + 0.8 * nz * nz + (ny > 0 ? 1 : 0.5) * ny * ny);
                tessellator.setColorOpaque_F(r * shade, g * shade, b * shade);
                tessellator.setNormal((float) nx, (float) ny, (float) nz);
                for (int k = 1; k < face.length / 3 - 1; k++) {
                    vertex(icon, face, 0, x, y, z, nx, ny);
                    vertex(icon, face, k, x, y, z, nx, ny);
                    vertex(icon, face, k + 1, x, y, z, nx, ny);
                    vertex(icon, face, k + 1, x, y, z, nx, ny);
                }
            }
        }

        private static void vertex(IIcon icon, double[] face, int i, int x, int y, int z, double nx, double ny) {
            double px = face[i * 3], py = face[i * 3 + 1], pz = face[i * 3 + 2];
            // Project the texture along the axis the face points along most.
            double u = Math.abs(nx) > 0.5 ? pz : px, v = Math.abs(ny) > 0.5 ? pz : py;
            Tessellator.instance.addVertexWithUV(
                    x + px,
                    y + py,
                    z + pz,
                    icon.getInterpolatedU(u * 16),
                    icon.getInterpolatedV((1 - v) * 16));
        }

        @Override
        public boolean shouldRender3DInInventory(int modelId) {
            return true;
        }

        @Override
        public int getRenderId() {
            return renderId;
        }
    }
}
