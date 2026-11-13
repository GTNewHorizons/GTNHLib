package com.gtnewhorizon.gtnhlib.test.block;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.AutoShaderUpdater;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizons.angelica.api.glstate.GLState;
import com.gtnewhorizons.angelica.glsm.profiling.Tracy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

// The tile entity render only records each hole. They're drawn after the world, farthest first, so the screen copy
// they bend already has everything in it.
public class RenderBlackHole extends TileEntitySpecialRenderer {

    // Starts a 10 second profile this many seconds after joining a world, when set.
    private static final int AUTO_PROFILE = Integer.getInteger("gtnhlib.blackhole.autoProfile", -1);
    private static final Tracy.ZoneId COPY_ZONE = Tracy.zoneId("blackhole copy", Tracy.COLOR_CLIENT);
    private static final Tracy.ZoneId DRAW_ZONE = Tracy.zoneId("blackhole draw", Tracy.COLOR_CLIENT);
    // Units 0 and 1 belong to the game. From here: scene, depth, capture, outside table, inside table.
    private static final int FIRST_UNIT = 2;
    private static final int MAX_OTHERS = 4;
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer OTHERS = BufferUtils.createFloatBuffer(MAX_OTHERS * 4);
    private static final Comparator<Hole> FARTHEST_FIRST = Comparator.comparingDouble((Hole hole) -> hole.distance)
            .reversed();

    private final List<Hole> holes = new ArrayList<>();
    private final ArrayDeque<Hole> spareHoles = new ArrayDeque<>();
    private final int[] savedTextures = new int[5];
    private final BlackHoleMesh mesh = new BlackHoleMesh();
    private final DeflectionTable outside = new DeflectionTable();
    private final DeflectionTable inside = new DeflectionTable();
    final BlackHoleCapture capture = new BlackHoleCapture();

    private ShaderProgram lens;
    private ShaderProgram table;
    private int uModelViewProjection;
    private int uCamera;
    private int uFogStart;
    private int uFogEnd;
    private int uFogColor;
    private int uBlocksPerUnit;
    private int uOthers;
    private int uOtherCount;
    private int uStart;
    private int uHorizon;
    private int uTableHorizon;
    private int uFadeStart;
    private int uFadeEnd;
    private int tablesFor = -1;

    private int sceneTexture;
    private int depthTexture;
    private int sceneWidth;
    private int sceneHeight;

    private BlackHoleProfile profile;
    private long inWorldSince;
    private final ArrayDeque<Timing> timings = new ArrayDeque<>();

    public RenderBlackHole() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void startProfile(int seconds) {
        profile = new BlackHoleProfile(seconds, capture.isEnabled());
        if (Tracy.ENABLED) {
            final String path = new File(
                    Minecraft.getMinecraft().mcDataDir,
                    "blackhole-" + System.currentTimeMillis() + ".tracy").getPath();
            final String error = Tracy.captureStart(path, seconds);
            GTNHLib.LOG.info(error == null ? "Tracy capture: " + path : "Tracy capture failed: " + error);
        }
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        final Hole hole = spareHoles.isEmpty() ? new Hole() : spareHoles.pop();
        hole.tile = te;
        hole.centre.set((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);
        hole.size = (float) BlackHoleSettings.radius;
        GLState.getModelViewMatrix(hole.view);
        GLState.getProjectionMatrix(hole.projection);
        if (GLState.isFogEnabled()) {
            hole.fogStart = GLState.getFogStart();
            hole.fogEnd = GLState.getFogEnd();
            GLState.getFogColor(hole.fogColor);
        } else {
            hole.fogStart = 0;
            hole.fogEnd = Float.MAX_VALUE;
        }
        holes.add(hole);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        final Minecraft mc = Minecraft.getMinecraft();
        if (AUTO_PROFILE >= 0 && inWorldSince >= 0) {
            final long now = System.nanoTime();
            if (mc.theWorld == null) inWorldSince = 0;
            else if (inWorldSince == 0) inWorldSince = now;
            else if (now - inWorldSince >= AUTO_PROFILE * 1_000_000_000L) {
                startProfile(10);
                inWorldSince = -1;
            }
        }
        collectTimings();
        if (profile != null && profile.frame(System.nanoTime())) {
            for (String line : profile.summary()) {
                GTNHLib.LOG.info(line);
                mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(line));
            }
            profile = null;
        }
        capture.tick(event.renderTickTime, profile);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (holes.isEmpty()) return;
        try {
            drawAll();
        } finally {
            for (int i = 0; i < holes.size(); i++) {
                holes.get(i).tile = null;
                spareHoles.push(holes.get(i));
            }
            holes.clear();
        }
    }

    private void drawAll() {
        final Minecraft mc = Minecraft.getMinecraft();
        final int width = mc.displayWidth;
        final int height = mc.displayHeight;
        for (int i = 0; i < holes.size(); i++) holes.get(i).place();
        holes.sort(FARTHEST_FIRST);

        final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        final boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final int[] textures = savedTextures;
        for (int i = 0; i < textures.length; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + i);
            textures[i] = GL11.glGetInteger(i == 2 ? GL13.GL_TEXTURE_BINDING_CUBE_MAP : GL11.GL_TEXTURE_BINDING_2D);
        }
        // The shader does its own depth test against the depth copy.
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_CULL_FACE);

        if (loadShaders()) {
            for (int i = 0; i < holes.size(); i++) draw(holes.get(i), i == 0, width, height);
        }

        for (int i = 0; i < textures.length; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + i);
            GL11.glBindTexture(i == 2 ? GL13.GL_TEXTURE_CUBE_MAP : GL11.GL_TEXTURE_2D, textures[i]);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        if (blend) GL11.glEnable(GL11.GL_BLEND);
        if (depthTest) GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(depthMask);
        if (!cull) GL11.glDisable(GL11.GL_CULL_FACE);
        GL20.glUseProgram(program);
    }

    private void draw(Hole hole, boolean first, int width, int height) {
        final Timing timing = profile != null ? new Timing(hole.distance) : null;

        Tracy.beginZone(COPY_ZONE);
        if (timing != null) timing.copy = beginTimer();
        if (first) copyScene(width, height);
        // Depth is only read under each hole, so only that part is copied.
        final int[] rect = hole.screenRect(width, height);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + 1);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, rect[0], rect[1], rect[0], rect[1], rect[2], rect[3]);
        if (timing != null) GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
        Tracy.endZone();

        Tracy.beginZone(DRAW_ZONE);
        if (timing != null) timing.lens = beginTimer();
        table.use();
        if (tablesFor != BlackHoleSettings.version) {
            GL20.glUniform1f(uTableHorizon, BlackHoleSettings.horizon);
            GL20.glUniform1f(uFadeStart, BlackHoleSettings.fadeStart);
            GL20.glUniform1f(uFadeEnd, BlackHoleSettings.fadeEnd);
            outside.invalidate();
            inside.invalidate();
            tablesFor = BlackHoleSettings.version;
        }
        outside.fill(1, uStart, mesh);
        final float start = hole.camera.length();
        if (start < 1) inside.fill(start, uStart, mesh);
        GL11.glViewport(0, 0, width, height);

        lens.use();
        MATRIX.clear();
        GL20.glUniformMatrix4(uModelViewProjection, false, hole.modelViewProjection.get(MATRIX));
        GL20.glUniform3f(uCamera, hole.camera.x, hole.camera.y, hole.camera.z);
        GL20.glUniform1f(uHorizon, BlackHoleSettings.horizon);
        GL20.glUniform1f(uFogStart, hole.fogStart);
        GL20.glUniform1f(uFogEnd, hole.fogEnd);
        GL20.glUniform3f(uFogColor, hole.fogColor.x, hole.fogColor.y, hole.fogColor.z);
        GL20.glUniform1f(uBlocksPerUnit, hole.size);
        int others = 0;
        OTHERS.clear();
        for (int i = 0; i < holes.size() && others < MAX_OTHERS; i++) {
            final Hole other = holes.get(i);
            if (other == hole) continue;
            OTHERS.put((other.centre.x - hole.centre.x) / hole.size).put((other.centre.y - hole.centre.y) / hole.size)
                    .put((other.centre.z - hole.centre.z) / hole.size).put(other.size / hole.size);
            others++;
        }
        OTHERS.flip();
        if (others > 0) GL20.glUniform4(uOthers, OTHERS);
        GL20.glUniform1i(uOtherCount, others);
        final BlackHoleCapture.Cube cube = capture.cube(hole.tile);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + 2);
        // Rays that pass through another hole still read this hole's capture.
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, cube.texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + 3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, outside.texture());
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + 4);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, inside.texture());

        cube.lastDrawn = System.nanoTime();
        final boolean asking = cube.ask();
        mesh.drawCube();
        if (asking) GL15.glEndQuery(GL33.GL_ANY_SAMPLES_PASSED);
        if (timing != null) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            timings.add(timing);
        }
        Tracy.endZone();
    }

    // Same formats as the game's framebuffer, so the copies don't convert.
    private void copyScene(int width, int height) {
        final boolean resized = width != sceneWidth || height != sceneHeight;
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT);
        if (sceneTexture == 0) sceneTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTexture);
        if (resized)
            createScreenTexture(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, GL11.GL_LINEAR, width, height);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + FIRST_UNIT + 1);
        if (depthTexture == 0) depthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        if (resized) {
            createScreenTexture(
                    GL30.GL_DEPTH24_STENCIL8,
                    GL30.GL_DEPTH_STENCIL,
                    GL30.GL_UNSIGNED_INT_24_8,
                    GL11.GL_NEAREST,
                    width,
                    height);
        }
        sceneWidth = width;
        sceneHeight = height;
    }

    private static void createScreenTexture(int internalFormat, int format, int type, int filter, int width,
            int height) {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private boolean loadShaders() {
        if (lens == null) {
            lens = load("blackhole", this::lensCompiled);
            table = load("table", this::tableCompiled);
        }
        return lens.getProgram() != 0 && table.getProgram() != 0;
    }

    private static ShaderProgram load(String name, Consumer<ShaderProgram> compiled) {
        final String vert = "shaders/blackhole/" + name + ".vert";
        final String frag = "shaders/blackhole/" + name + ".frag";
        final ShaderProgram program = new ShaderProgram(GTNHLib.RESOURCE_DOMAIN, vert, frag);
        if (program.getProgram() != 0) {
            program.use();
            compiled.accept(program);
        }
        if ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            AutoShaderUpdater.getInstance().registerShaderReload(
                    program,
                    GTNHLib.RESOURCE_DOMAIN,
                    vert,
                    frag,
                    (shader, v, f) -> compiled.accept(shader));
        }
        return program;
    }

    private void lensCompiled(ShaderProgram shader) {
        final int program = shader.getProgram();
        uModelViewProjection = GL20.glGetUniformLocation(program, "u_ModelViewProjection");
        uHorizon = GL20.glGetUniformLocation(program, "u_Horizon");
        uCamera = GL20.glGetUniformLocation(program, "u_Camera");
        uFogStart = GL20.glGetUniformLocation(program, "u_FogStart");
        uFogEnd = GL20.glGetUniformLocation(program, "u_FogEnd");
        uFogColor = GL20.glGetUniformLocation(program, "u_FogColor");
        uBlocksPerUnit = GL20.glGetUniformLocation(program, "u_BlocksPerUnit");
        uOthers = GL20.glGetUniformLocation(program, "u_Others");
        uOtherCount = GL20.glGetUniformLocation(program, "u_OtherCount");
        final String[] samplers = { "u_Scene", "u_SceneDepth", "u_Capture", "u_Outside", "u_Inside" };
        for (int i = 0; i < samplers.length; i++) {
            GL20.glUniform1i(GL20.glGetUniformLocation(program, samplers[i]), FIRST_UNIT + i);
        }
    }

    private void tableCompiled(ShaderProgram shader) {
        final int program = shader.getProgram();
        uStart = GL20.glGetUniformLocation(program, "u_Start");
        uTableHorizon = GL20.glGetUniformLocation(program, "u_Horizon");
        uFadeStart = GL20.glGetUniformLocation(program, "u_FadeStart");
        uFadeEnd = GL20.glGetUniformLocation(program, "u_FadeEnd");
        tablesFor = -1;
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "u_Width"), DeflectionTable.WIDTH);
        outside.invalidate();
        inside.invalidate();
    }

    private static int beginTimer() {
        final int query = GL15.glGenQueries();
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
        return query;
    }

    private void collectTimings() {
        while (!timings.isEmpty()
                && GL15.glGetQueryObjecti(timings.peek().lens, GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE) {
            final Timing timing = timings.pop();
            final long copy = GL33.glGetQueryObjectui64(timing.copy, GL15.GL_QUERY_RESULT);
            final long lens = GL33.glGetQueryObjectui64(timing.lens, GL15.GL_QUERY_RESULT);
            GL15.glDeleteQueries(timing.copy);
            GL15.glDeleteQueries(timing.lens);
            if (profile != null) profile.draw(timing.distance, copy, lens);
        }
    }

    private static final class Hole {

        TileEntity tile;
        // Relative to the camera.
        final Vector3f centre = new Vector3f();
        float size;
        final Matrix4f view = new Matrix4f();
        final Matrix4f projection = new Matrix4f();
        float fogStart;
        float fogEnd;
        final Vector4f fogColor = new Vector4f();

        // Set by place(). The hole's own space is centred on it, with the sphere's radius as one unit.
        final Matrix4f modelView = new Matrix4f();
        final Matrix4f modelViewProjection = new Matrix4f();
        private final Vector4f corner = new Vector4f();
        private final int[] rect = new int[4];
        final Vector3f camera = new Vector3f();
        double distance;

        void place() {
            modelView.set(view).translate(centre).scale(size);
            projection.mul(modelView, modelViewProjection);
            modelView.invert().transformPosition(camera.set(0, 0, 0));
            distance = centre.length();
        }

        // The cube's screen bounds in pixels as x, y, width, height; the whole screen if any corner is behind the
        // camera.
        int[] screenRect(int width, int height) {
            final int[] rect = this.rect;
            float minX = 1, minY = 1, maxX = -1, maxY = -1;
            for (int i = 0; i < 8; i++) {
                corner.set((i & 1) * 2 - 1, (i >> 1 & 1) * 2 - 1, (i >> 2 & 1) * 2 - 1, 1);
                modelViewProjection.transform(corner);
                if (corner.w <= 0) {
                    rect[0] = rect[1] = 0;
                    rect[2] = width;
                    rect[3] = height;
                    return rect;
                }
                minX = Math.min(minX, corner.x / corner.w);
                maxX = Math.max(maxX, corner.x / corner.w);
                minY = Math.min(minY, corner.y / corner.w);
                maxY = Math.max(maxY, corner.y / corner.w);
            }
            final int x0 = pixel(minX, width) - 1, y0 = pixel(minY, height) - 1;
            final int x1 = pixel(maxX, width) + 1, y1 = pixel(maxY, height) + 1;
            final int left = Math.max(x0, 0), bottom = Math.max(y0, 0);
            rect[0] = left;
            rect[1] = bottom;
            rect[2] = Math.max(Math.min(x1, width) - left, 0);
            rect[3] = Math.max(Math.min(y1, height) - bottom, 0);
            return rect;
        }

        private static int pixel(float ndc, int size) {
            return (int) Math.floor((ndc * 0.5f + 0.5f) * size);
        }
    }

    private static final class Timing {

        final double distance;
        int copy;
        int lens;

        Timing(double distance) {
            this.distance = distance;
        }
    }
}
