package com.gtnewhorizon.gtnhlib.client.title;

import java.util.Random;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Screen-space cinematic "completion" effects in 5 escalating tiers, drawn with 2D Tessellator primitives only (no
 * GLSL, so it never fights Iris/shaderpacks). Driven by seconds-since-shown so each tier plays a short timeline. All
 * alpha is multiplied by the title's fade and flashes are capped, with a shape cue paired to every colour for
 * accessibility.
 */
@SideOnly(Side.CLIENT)
public class TitleEffectSystem {

    public static final int EFFECT_NONE = 0;
    // 1 and 2 (spark, burst) were removed; numbers kept so 3-6 stay stable
    public static final int EFFECT_HYPERSPACE = 3;
    public static final int EFFECT_SINGULARITY = 4;
    public static final int EFFECT_WARP = 5;
    public static final int EFFECT_LIGHTSPEED = 6;

    private static final Random rand = new Random();
    private static final int N = 220;
    private static final float[] angle = new float[N];
    private static final float[] seed = new float[N];

    private static int tier = 0;
    private static float cx, cy, sw, sh, maxR;
    private static boolean spawned = false;

    // Per-frame feedback the renderer applies to the whole title block, so text + icon shake/punch with the effect.
    private static float shakeX = 0F, shakeY = 0F, scalePulse = 1F;

    public static float getShakeX() {
        return shakeX;
    }

    public static float getShakeY() {
        return shakeY;
    }

    public static float getScalePulse() {
        return scalePulse;
    }

    public static void spawn(int effectTier, float centerX, float centerY, int screenW, int screenH) {
        tier = effectTier;
        cx = centerX;
        cy = centerY;
        sw = screenW;
        sh = screenH;
        maxR = (float) Math.sqrt((double) screenW * screenW + (double) screenH * screenH) * 0.5F;
        spawned = tier > 0;
        if (!spawned) return;
        for (int i = 0; i < N; i++) {
            angle[i] = (float) (rand.nextFloat() * Math.PI * 2.0);
            seed[i] = rand.nextFloat();
        }
    }

    public static void clear() {
        tier = 0;
        spawned = false;
    }

    /**
     * Driven by the notification lifecycle so the effect builds during fade-in, sustains through the stay, and climaxes
     * on fade-out.
     *
     * @param t01       progress over the whole notification, 0 at appear -> 1 at gone.
     * @param fInEnd    fraction of the lifetime where fade-in ends.
     * @param fOutStart fraction where fade-out begins.
     * @param ga        the title's current fade alpha, 0..1.
     */
    public static void render(float t01, float fInEnd, float fOutStart, float ga) {
        shakeX = 0F;
        shakeY = 0F;
        scalePulse = 1F;
        if (!spawned || tier <= 0 || ga <= 0F) return;

        float build = ramp(t01, 0F, fInEnd);
        float sustain = clamp01((t01 - fInEnd) / Math.max(0.0001F, fOutStart - fInEnd));
        float climax = ramp(t01, fOutStart, 1F);

        // the "jump": shake + scale-punch right as the title starts leaving, for the heavy tiers
        float jump = 0F;
        float shakeAmp = 18F, pulseAmp = 0.18F;
        if (tier == EFFECT_WARP) {
            jump = bell(climax, 0.18F, 0.18F);
        } else if (tier == EFFECT_SINGULARITY) {
            jump = bell(climax, 0.12F, 0.12F);
        } else if (tier == EFFECT_LIGHTSPEED) {
            // the "punch it" lunge happens at the snap, right as the title settles in
            jump = bell(sustain, 0.06F, 0.12F);
            shakeAmp = 9F;
            pulseAmp = 0.3F;
        }
        if (jump > 0F) {
            shakeX = (rand.nextFloat() - 0.5F) * shakeAmp * jump;
            shakeY = (rand.nextFloat() - 0.5F) * shakeAmp * jump;
            scalePulse = 1F + pulseAmp * jump;
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPushMatrix();
        GL11.glTranslatef(shakeX, shakeY, 0F);

        switch (tier) {
            case EFFECT_HYPERSPACE -> renderHyperspace(t01, build, sustain, climax, ga);
            case EFFECT_SINGULARITY -> renderSingularity(t01, build, sustain, climax, ga);
            case EFFECT_WARP -> renderWarp(t01, build, sustain, climax, ga);
            case EFFECT_LIGHTSPEED -> renderLightspeed(t01, build, sustain, climax, ga);
            default -> {}
        }

        GL11.glPopMatrix();
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    // ---- tiers ----

    private static void renderHyperspace(float t01, float build, float sustain, float climax, float ga) {
        float on = Math.min(1F, build * 2F + sustain) * (1F - climax);
        normalBlend();
        drawQuad(0, 0, sw, sh, 0.01F, 0.03F, 0.12F, 0.4F * Math.min(1F, build + sustain) * (1F - climax * 0.6F) * ga);

        additive();
        beginQuads();
        float swirl = t01 * 1.2F;
        for (int i = 0; i < 160; i++) {
            float speed = 0.6F + 1.2F * seed[i];
            float phase = (t01 * speed + seed[i]) % 1F;
            float ang = angle[i] + swirl + phase * 0.5F;
            float rIn = phase * maxR * 1.2F;
            float rOut = rIn + lerp(80F, 360F, phase);
            // bright across most of the streak; only the very ends taper
            float vis = clamp01(phase * 6F) * clamp01((1F - phase) * 4F);
            float al = vis * on * ga;
            if (al <= 0F) continue;
            streakAt(cx, cy, ang, rIn, rOut, 3.5F, 0.7F, 0.85F, 1F, 0F, al);
        }
        end();

        // collapse flash as it releases
        float flash = bell(climax, 0.2F, 0.2F) * 0.35F * ga;
        if (flash > 0F) {
            additive();
            drawGlow(cx, cy, maxR, 0.6F, 0.8F, 1F, flash);
        }
    }

    private static void renderSingularity(float t01, float build, float sustain, float climax, float ga) {
        normalBlend();
        drawQuad(0, 0, sw, sh, 0.02F, 0F, 0.04F, 0.6F * Math.min(1F, build + sustain) * (1F - climax * 0.5F) * ga);

        // accretion: matter spirals INWARD through build + sustain
        float spin = t01 * 2F;
        float on = Math.min(1F, build * 2F + sustain) * (1F - climax);
        additive();
        beginQuads();
        for (int i = 0; i < 130; i++) {
            float speed = 0.4F + 0.8F * seed[i];
            float phase = 1F - ((t01 * speed + seed[i]) % 1F); // 1 at rim -> 0 at core
            float rOut = phase * maxR;
            float rIn = Math.min(maxR, rOut + lerp(40F, 120F, 1F - phase));
            float al = (1F - phase) * (1F - phase) * 0.85F * on * ga;
            if (al <= 0F) continue;
            streakAt(cx, cy, angle[i] + spin, rIn, rOut, 3F, 0.6F, 0.35F, 1F, 0F, al);
        }
        end();
        additive();
        drawGlow(cx, cy, lerp(40F, 120F, sustain), 1F, 0.6F, 1F, 0.3F * on * ga);

        // implosion flash + outward blast on the climax
        float flash = bell(climax, 0.15F, 0.15F) * 0.42F * ga;
        if (flash > 0F) {
            additive();
            drawGlow(cx, cy, maxR * 0.9F, 1F, 0.75F, 1F, flash);
        }
        float blast = ramp(climax, 0.15F, 0.7F);
        if (blast > 0F && blast < 1F) {
            additive();
            beginQuads();
            for (int i = 0; i < 140; i++) {
                float rIn = blast * maxR * (0.4F + 0.6F * seed[i]);
                float rOut = rIn + lerp(40F, 120F, blast);
                float al = (1F - blast) * 0.9F * ga;
                if (al <= 0F) continue;
                streakAt(cx, cy, angle[i], rIn, rOut, 3F, 0.75F, 0.3F, 1F, 0F, al);
            }
            end();
            float rr = lerp(20F, maxR, blast);
            drawRing(cx - 8F, cy, rr, 3F, 1F, 0.2F, 0.2F, (1F - blast) * 0.6F * ga);
            drawRing(cx + 8F, cy, rr, 3F, 0.3F, 0.5F, 1F, (1F - blast) * 0.6F * ga);
        }
    }

    private static void renderWarp(float t01, float build, float sustain, float climax, float ga) {
        // darken to deep space so the stretching stars blaze
        normalBlend();
        drawQuad(
                0,
                0,
                sw,
                sh,
                0.01F,
                0.02F,
                0.08F,
                0.62F * Math.min(1F, build * 1.5F + sustain) * (1F - climax * 0.4F) * ga);

        additive();
        beginQuads();
        for (int i = 0; i < 200; i++) {
            float speed = 0.5F + 1.0F * seed[i];
            float phase = (t01 * speed * (1F + climax * 2F) + seed[i]) % 1F;
            float r = phase * maxR * 1.4F;
            // points (charge) -> long streaks (warp) -> longest (the jump)
            float lenFactor = 0.15F + 0.85F * build + climax * 1.2F;
            float rIn = Math.max(0F, r - lerp(8F, 360F, phase) * lenFactor);
            float bright = (0.6F + 0.4F * seed[i]) * Math.min(1F, build * 2.5F) * ga;
            if (bright <= 0F) continue;
            streakAt(cx, cy, angle[i], rIn, r, lerp(2.5F, 4.5F, phase), 0.8F, 0.9F, 1F, 0.1F * bright, bright);
        }
        end();

        // the jump: bright flash + shockwave (shake + scale-punch are applied in render())
        float flash = bell(climax, 0.18F, 0.18F) * 0.45F * ga;
        if (flash > 0F) {
            additive();
            drawQuad(0, 0, sw, sh, 0.85F, 0.92F, 1F, flash);
        }
        float ringP = ramp(climax, 0.05F, 0.5F);
        if (ringP > 0F && ringP < 1F) {
            additive();
            drawRing(cx, cy, lerp(20F, maxR * 1.2F, ringP), 5F, 0.8F, 0.9F, 1F, (1F - ringP) * 0.8F * ga);
        }
        additive();
        drawGlow(cx, cy, maxR * 0.9F, 0.2F, 0.45F, 1F, 0.12F * sustain * (1F - climax) * ga);
    }

    private static void renderLightspeed(float t01, float build, float sustain, float climax, float ga) {
        // deep space backdrop
        normalBlend();
        drawQuad(
                0,
                0,
                sw,
                sh,
                0F,
                0.01F,
                0.05F,
                0.66F * Math.min(1F, build * 1.5F + sustain) * (1F - climax * 0.4F) * ga);

        // stars sit as points during the charge, then SNAP into long streaks the instant it settles
        float snap = clamp01(sustain / 0.12F);
        float lenFactor = 0.04F + 0.96F * snap + climax * 0.8F;
        additive();
        beginQuads();
        for (int i = 0; i < 220; i++) {
            float speed = 0.6F + 1.0F * seed[i];
            float phase = (t01 * speed * (1F + snap + climax * 1.5F) + seed[i]) % 1F;
            float r = phase * maxR * 1.5F;
            float rIn = Math.max(0F, r - lerp(6F, 440F, phase) * lenFactor);
            float bright = (0.7F + 0.3F * seed[i]) * Math.min(1F, build * 2F + 0.25F) * (1F - climax * 0.3F) * ga;
            if (bright <= 0F) continue;
            streakAt(cx, cy, angle[i], rIn, r, lerp(1.8F, 4F, phase), 0.95F, 0.97F, 1F, 0.12F * bright, bright);
        }
        end();

        // the white snap-flash at the punch
        float flash = bell(sustain, 0.05F, 0.1F) * 0.5F * ga;
        if (flash > 0F) {
            additive();
            drawQuad(0, 0, sw, sh, 1F, 1F, 1F, flash);
        }
    }

    // ---- primitives ----

    private static void additive() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    private static void normalBlend() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void beginQuads() {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
    }

    private static void end() {
        Tessellator.instance.draw();
    }

    /** Radial streak quad with a per-end alpha gradient. Call between beginQuads()/end(). */
    private static void streakAt(float ox, float oy, float ang, float rIn, float rOut, float hw, float r, float g,
            float b, float aIn, float aOut) {
        Tessellator t = Tessellator.instance;
        float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
        float px = -dy, py = dx;
        float ix = ox + dx * rIn, iy = oy + dy * rIn;
        float oxx = ox + dx * rOut, oyy = oy + dy * rOut;
        t.setColorRGBA_F(r, g, b, aIn);
        t.addVertex(ix + px * hw, iy + py * hw, 0);
        t.addVertex(ix - px * hw, iy - py * hw, 0);
        t.setColorRGBA_F(r, g, b, aOut);
        t.addVertex(oxx - px * hw, oyy - py * hw, 0);
        t.addVertex(oxx + px * hw, oyy + py * hw, 0);
    }

    private static void drawQuad(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        if (a <= 0F) return;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(r, g, b, a);
        t.addVertex(x1, y2, 0);
        t.addVertex(x2, y2, 0);
        t.addVertex(x2, y1, 0);
        t.addVertex(x1, y1, 0);
        t.draw();
    }

    private static void drawRing(float ox, float oy, float radius, float thickness, float r, float g, float b,
            float a) {
        if (a <= 0F || radius <= 0F) return;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(r, g, b, a);
        int segs = 48;
        float inner = radius - thickness;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * Math.PI * 2.0 / segs);
            float a1 = (float) ((i + 1) * Math.PI * 2.0 / segs);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            t.addVertex(ox + c0 * inner, oy + s0 * inner, 0);
            t.addVertex(ox + c0 * radius, oy + s0 * radius, 0);
            t.addVertex(ox + c1 * radius, oy + s1 * radius, 0);
            t.addVertex(ox + c1 * inner, oy + s1 * inner, 0);
        }
        t.draw();
    }

    private static void drawGlow(float ox, float oy, float radius, float r, float g, float b, float aCenter) {
        if (aCenter <= 0F || radius <= 0F) return;
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLE_FAN);
        t.setColorRGBA_F(r, g, b, aCenter);
        t.addVertex(ox, oy, 0);
        t.setColorRGBA_F(r, g, b, 0F);
        int segs = 32;
        for (int i = 0; i <= segs; i++) {
            float a = (float) (i * Math.PI * 2.0 / segs);
            t.addVertex(ox + (float) Math.cos(a) * radius, oy + (float) Math.sin(a) * radius, 0);
        }
        t.draw();
    }

    // ---- timeline helpers ----

    private static float clamp01(float x) {
        return x < 0F ? 0F : (x > 1F ? 1F : x);
    }

    private static float ramp(float t, float t0, float t1) {
        return clamp01((t - t0) / (t1 - t0));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Triangular pulse peaking at {@code center} with half-width {@code half}; 0 outside. */
    private static float bell(float t, float center, float half) {
        float d = Math.abs(t - center) / half;
        return d >= 1F ? 0F : 1F - d;
    }
}
