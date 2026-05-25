package com.gtnewhorizon.gtnhlib.client.title;

import static com.gtnewhorizon.gtnhlib.client.title.TitleRenderer.itemRender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.client.renderer.VertexTransformer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TitleParticleSystem {

    public static final int PARTICLE_NONE = 0;
    public static final int PARTICLE_CONFETTI = 1;
    public static final int PARTICLE_SPARKLE = 2;
    public static final int PARTICLE_FIREWORK = 3;
    public static final int PARTICLE_ITEM_CONFETTI = 4;

    private static final List<TitleParticle> particles = new ArrayList<>();
    private static final Random rand = new Random();

    private static final float[][] CONFETTI_COLORS = { { 1F, 0.2F, 0.2F }, { 0.2F, 1F, 0.2F }, { 0.3F, 0.3F, 1F },
            { 1F, 1F, 0.2F }, { 1F, 0.3F, 1F }, { 0.3F, 1F, 1F }, { 1F, 0.6F, 0.1F }, };

    public static void spawn(int effect, int cx, int cy, ItemStack icon) {
        if (!GTNHLibConfig.enableTitleParticles) return;

        ItemStack confettiItem = TitleAPI.getConfettiIcon();
        if (confettiItem == null) confettiItem = icon;
        int countOverride = TitleAPI.getParticleCount();

        switch (effect) {
            case PARTICLE_CONFETTI:
                spawnConfetti(cx, cy, countOverride >= 0 ? countOverride : 120);
                break;
            case PARTICLE_SPARKLE:
                spawnSparkle(cx, cy, countOverride >= 0 ? countOverride : 40);
                break;
            case PARTICLE_FIREWORK:
                spawnFirework(cx, cy, countOverride >= 0 ? countOverride : 150);
                break;
            case PARTICLE_ITEM_CONFETTI:
                spawnItemConfetti(cx, cy, confettiItem, countOverride >= 0 ? countOverride : 25);
                break;
        }
    }

    private static void spawnConfetti(int cx, int cy, int count) {
        for (int i = 0; i < count; i++) {
            TitleParticle p = new TitleParticle();
            float[] c = CONFETTI_COLORS[rand.nextInt(CONFETTI_COLORS.length)];
            p.r = c[0];
            p.g = c[1];
            p.b = c[2];
            p.x = cx + rand.nextFloat() * 300 - 150;
            p.y = cy + rand.nextFloat() * 40;
            p.vx = rand.nextFloat() * 12 - 6;
            p.vy = -(rand.nextFloat() * 10 + 4);
            p.size = rand.nextFloat() * 3 + 3;
            p.rotation = rand.nextFloat() * 360;
            p.rotationSpeed = rand.nextFloat() * 20 - 10;
            p.maxLifetime = 60 + rand.nextInt(60);
            p.lifetime = p.maxLifetime;
            particles.add(p);
        }
    }

    private static void spawnSparkle(int cx, int cy, int count) {
        for (int i = 0; i < count; i++) {
            TitleParticle p = new TitleParticle();
            float brightness = 0.8F + rand.nextFloat() * 0.2F;
            p.r = brightness;
            p.g = brightness * (0.8F + rand.nextFloat() * 0.2F);
            p.b = brightness * 0.4F;
            p.x = cx + rand.nextFloat() * 300 - 150;
            p.y = cy - 30 + rand.nextFloat() * 60;
            p.vx = rand.nextFloat() * 1.5F - 0.75F;
            p.vy = -(rand.nextFloat() * 1.5F + 0.3F);
            p.size = rand.nextFloat() * 2.5F + 1.5F;
            p.rotation = 45;
            p.rotationSpeed = 0;
            p.maxLifetime = 80 + rand.nextInt(60);
            p.lifetime = p.maxLifetime;
            particles.add(p);
        }
    }

    private static void spawnFirework(int cx, int cy, int count) {
        for (int i = 0; i < count; i++) {
            TitleParticle p = new TitleParticle();
            float[] c = CONFETTI_COLORS[rand.nextInt(CONFETTI_COLORS.length)];
            p.r = c[0];
            p.g = c[1];
            p.b = c[2];
            p.x = cx;
            p.y = cy - 30;
            float angle = (float) (rand.nextFloat() * Math.PI * 2);
            float speed = rand.nextFloat() * 8 + 3;
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed;
            p.size = rand.nextFloat() * 2.5F + 1.5F;
            p.rotation = rand.nextFloat() * 360;
            p.rotationSpeed = rand.nextFloat() * 15 - 7.5F;
            p.maxLifetime = 40 + rand.nextInt(40);
            p.lifetime = p.maxLifetime;
            particles.add(p);
        }
    }

    private static void spawnItemConfetti(int cx, int cy, ItemStack icon, int count) {
        if (icon == null) return;
        for (int i = 0; i < count; i++) {
            TitleParticle p = new TitleParticle();
            p.r = 1;
            p.g = 1;
            p.b = 1;
            p.x = cx + rand.nextFloat() * 300 - 150;
            p.y = cy + rand.nextFloat() * 100 - 40;
            p.vx = rand.nextFloat() * 2.5F - 1.25F;
            p.vy = -(rand.nextFloat() * 2.5F + 0.5F);
            p.size = 1;
            p.rotation = 0;
            p.rotationSpeed = 0;
            p.maxLifetime = 60 + rand.nextInt(40);
            p.lifetime = p.maxLifetime;
            p.itemIcon = icon.copy();
            particles.add(p);
        }
    }

    public static void tick() {
        Iterator<TitleParticle> it = particles.iterator();
        while (it.hasNext()) {
            TitleParticle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            if (p.itemIcon == null) {
                p.vy += 0.15F;
            }
            p.vx *= 0.98F;
            p.rotation += p.rotationSpeed;
            p.lifetime--;
            if (p.lifetime <= 0) {
                it.remove();
            }
        }
    }

    public static void render(float partialTicks) {
        if (particles.isEmpty()) return;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator t = Tessellator.instance;
        for (TitleParticle p : particles) {
            if (p.itemIcon != null) continue;
            float a = p.alpha();
            if (a <= 0) continue;

            float px = p.x + p.vx * partialTicks;
            float py = p.y + p.vy * partialTicks;
            final Matrix4f mat4f = VertexTransformer.resetIdentity();
            mat4f.translate(px, py, 0);
            mat4f.rotate((float) Math.toRadians(p.rotation + p.rotationSpeed * partialTicks), 0, 0, 1);

            if (!t.isDrawing) {
                t.startDrawingQuads();
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
            t.setColorRGBA_F(p.r, p.g, p.b, a);
            float s = p.size;
            VertexTransformer.addVertex(t, -s, -s, 0);
            VertexTransformer.addVertex(t, -s, s, 0);
            VertexTransformer.addVertex(t, s, s, 0);
            VertexTransformer.addVertex(t, s, -s, 0);
        }
        if (t.isDrawing) {
            t.draw();
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        Minecraft mc = Minecraft.getMinecraft();
        for (TitleParticle p : particles) {
            if (p.itemIcon == null) continue;
            float a = p.alpha();
            if (a <= 0) continue;

            GL11.glPushMatrix();
            float px = p.x + p.vx * partialTicks;
            float py = p.y + p.vy * partialTicks;
            float iconScale = 0.5F;
            GL11.glTranslatef(px - 8 * iconScale, py - 8 * iconScale, 0);
            GL11.glScalef(iconScale, iconScale, 1);
            GL11.glColor4f(1, 1, 1, a);

            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            FontRenderer font = p.itemIcon.getItem().getFontRenderer(p.itemIcon);
            if (font == null) font = mc.fontRenderer;

            try {
                itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), p.itemIcon, 0, 0);
            } catch (Exception ignored) {}

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            RenderHelper.disableStandardItemLighting();
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glPopMatrix();
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void clear() {
        particles.clear();
    }
}
