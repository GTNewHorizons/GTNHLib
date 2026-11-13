package com.gtnewhorizon.gtnhlib.test.block;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import com.gtnewhorizons.angelica.glsm.profiling.Tracy;
import com.gtnewhorizons.angelica.zoom.Zoom;

// A cube map of the world around each hole's centre, for bent rays that leave the screen. The hole doesn't move, so
// faces are refreshed slowly, one at a time, rather than every frame.
final class BlackHoleCapture {

    private static final long FORGET_AFTER = 10_000_000_000L;
    private static final long VISIBLE_FOR = 250_000_000L;
    // GL's cube face order.
    private static final float[] YAW = { -90, 90, 0, 0, 0, 180 };
    private static final float[] PITCH = { 0, 0, -90, 90, 0, 0 };
    private static final Tracy.ZoneId ZONE = Tracy.zoneId("blackhole capture", Tracy.COLOR_CLIENT);

    private final Map<TileEntity, Cube> cubes = new HashMap<>();
    private long lastCapture;
    private int size = BlackHoleSettings.captureSize;
    private EntityPig camera;
    private boolean enabled = true;
    private final ArrayDeque<Integer> queries = new ArrayDeque<>();

    Cube cube(TileEntity te) {
        return cubes.computeIfAbsent(te, tile -> new Cube());
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean toggle() {
        return enabled = !enabled;
    }

    // Must run outside the world render, since it renders the world itself.
    void tick(float partialTicks, BlackHoleProfile profile) {
        collectTimes(profile);
        final Minecraft mc = Minecraft.getMinecraft();
        final long now = System.nanoTime();
        final boolean resized = size != BlackHoleSettings.captureSize;
        size = BlackHoleSettings.captureSize;
        for (Iterator<Map.Entry<TileEntity, Cube>> it = cubes.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<TileEntity, Cube> entry = it.next();
            final TileEntity te = entry.getKey();
            final Cube cube = entry.getValue();
            if (te.isInvalid() || te.getWorldObj() != mc.theWorld || now - cube.lastDrawn > FORGET_AFTER) {
                cube.delete();
                it.remove();
            } else if (resized || now - cube.lastSeen > FORGET_AFTER) {
                cube.deleteTextures();
            }
        }
        if (!enabled || mc.theWorld == null || mc.skipRenderWorld) return;
        // Zoom would squeeze the wrong angle into a face.
        if (Zoom.getCurrentZoom() > 1.0001f) return;

        TileEntity dueHole = null;
        Cube due = null;
        for (Map.Entry<TileEntity, Cube> entry : cubes.entrySet()) {
            final Cube cube = entry.getValue();
            if (now - cube.lastSeen > VISIBLE_FOR) continue;
            if (due == null || cube.rank() < due.rank()) {
                due = cube;
                dueHole = entry.getKey();
            }
        }
        if (due == null) return;
        if (due.filled == 6 && now - lastCapture < 1e9 / BlackHoleSettings.captureRate) return;
        lastCapture = now;
        due.refreshed = now;

        Tracy.beginZone(ZONE);
        capture(mc, partialTicks, due, dueHole.xCoord + 0.5, dueHole.yCoord + 0.5, dueHole.zCoord + 0.5, profile);
        Tracy.endZone();
    }

    private void capture(Minecraft mc, float partialTicks, Cube cube, double x, double y, double z,
            BlackHoleProfile profile) {
        final int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        if (cube.texture == 0) cube.create(size);
        final int face = cube.next;
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, cube.framebuffer);
        cube.attach(face);

        final EntityLivingBase view = mc.renderViewEntity;
        if (camera == null || camera.worldObj != mc.theWorld) camera = new EntityPig(mc.theWorld);
        camera.yOffset = view.yOffset;
        camera.rotationYaw = camera.prevRotationYaw = YAW[face];
        camera.rotationPitch = camera.prevRotationPitch = PITCH[face];
        // The game pulls a first-person camera 0.1 blocks back along its view; undo that so every face starts at the
        // hole's centre.
        final Vec3 look = camera.getLook(1f);
        camera.posX = camera.prevPosX = camera.lastTickPosX = x + 0.1 * look.xCoord;
        camera.posY = camera.prevPosY = camera.lastTickPosY = y + 0.1 * look.yCoord;
        camera.posZ = camera.prevPosZ = camera.lastTickPosZ = z + 0.1 * look.zCoord;

        final int width = mc.displayWidth;
        final int height = mc.displayHeight;
        final Framebuffer main = mc.getFramebuffer();
        final int mainObject = main.framebufferObject;
        final int mainWidth = main.framebufferWidth;
        final int mainHeight = main.framebufferHeight;
        final int query = profile != null ? GL15.glGenQueries() : 0;
        if (query != 0) GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
        final long start = System.nanoTime();
        try {
            mc.renderViewEntity = camera;
            mc.displayWidth = size;
            mc.displayHeight = size;
            // Vanilla's debug camera mode: a 90 degree view without the hand, entities or tile entities.
            mc.entityRenderer.debugViewDirection = 1;
            // Angelica binds the main framebuffer when the world render starts, so point it at the cube face.
            main.framebufferObject = cube.framebuffer;
            main.framebufferWidth = size;
            main.framebufferHeight = size;
            mc.entityRenderer.renderWorld(partialTicks, 0L);
        } finally {
            if (query != 0) {
                GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
                queries.add(query);
            }
            main.framebufferObject = mainObject;
            main.framebufferWidth = mainWidth;
            main.framebufferHeight = mainHeight;
            mc.entityRenderer.debugViewDirection = 0;
            mc.renderViewEntity = view;
            mc.displayWidth = width;
            mc.displayHeight = height;
            FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
        }
        cube.next = (face + 1) % 6;
        cube.filled = Math.min(cube.filled + 1, 6);

        if (profile != null) profile.capture(System.nanoTime() - start);
    }

    private void collectTimes(BlackHoleProfile profile) {
        while (!queries.isEmpty()
                && GL15.glGetQueryObjecti(queries.peek(), GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE) {
            final int query = queries.pop();
            if (profile != null) profile.captureGpu(GL33.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT));
            GL15.glDeleteQueries(query);
        }
    }

    static final class Cube {

        int texture;
        private int framebuffer;
        private int depth;
        private int next;
        private int filled;
        private long refreshed;
        private int query;
        private boolean pending;
        private long lastSeen;
        long lastDrawn;

        // Unfinished cubes first, then the least recently refreshed.
        private long rank() {
            return filled < 6 ? Long.MIN_VALUE + filled : refreshed;
        }

        // Starts an occlusion query around the hole's draw unless the last one is still in flight, reading that one
        // without waiting. Returns whether it started one; the caller ends it.
        boolean ask() {
            if (pending && GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE) {
                if (GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT) != 0) lastSeen = System.nanoTime();
                pending = false;
            }
            if (pending) return false;
            if (query == 0) query = GL15.glGenQueries();
            GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, query);
            pending = true;
            return true;
        }

        private void create(int size) {
            final int previousTexture = GL11.glGetInteger(GL13.GL_TEXTURE_BINDING_CUBE_MAP);
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texture);
            for (int face = 0; face < 6; face++) {
                GL11.glTexImage2D(
                        GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                        0,
                        GL11.GL_RGB8,
                        size,
                        size,
                        0,
                        GL11.GL_RGB,
                        GL11.GL_UNSIGNED_BYTE,
                        (ByteBuffer) null);
            }
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, previousTexture);

            framebuffer = FBO.glGenFramebuffers();
            FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            depth = FBO.glGenRenderbuffers();
            FBO.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depth);
            FBO.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, size, size);
            FBO.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depth);
            GL11.glClearColor(0, 0, 0, 0);
            for (int face = 0; face < 6; face++) {
                attach(face);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            }
        }

        private void deleteTextures() {
            if (texture == 0) return;
            GL11.glDeleteTextures(texture);
            FBO.glDeleteFramebuffers(framebuffer);
            FBO.glDeleteRenderbuffers(depth);
            texture = 0;
            next = 0;
            filled = 0;
        }

        private void delete() {
            deleteTextures();
            if (query != 0) GL15.glDeleteQueries(query);
        }

        private void attach(int face) {
            FBO.glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER,
                    GL30.GL_COLOR_ATTACHMENT0,
                    GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                    texture,
                    0);
        }
    }
}
