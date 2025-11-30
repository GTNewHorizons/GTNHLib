package com.gtnewhorizon.gtnhlib.client.renderer.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.renderer.textures.TextureAtlas;
import com.gtnewhorizon.gtnhlib.mixins.early.EntityRendererAccessor;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

// This was largely copied from avaritia
public class UniversiumShader extends ShaderProgram {

    private static UniversiumShader instance;

    private final TextureAtlas textureAtlas;

    private final int location_lightlevel;
    public final float[] LIGHT_LEVEL = new float[3];

    private final int location_time;

    private final int location_yaw;

    private final int location_pitch;

    private final int location_externalScale;

    private final int location_lightmix;

    private final int location_opacity;
    public static float cosmicOpacity = 1.0f;

    private final int location_starColorMultiplier;
    private float starColorMultiplier = 1;

    private final int location_bgColor;
    private final float[] BG_COLORS = new float[] { -1, -1, -1 };

    private final int location_cosmicuvs;

    private UniversiumShader() {
        super(GTNHLib.RESOURCE_DOMAIN, "shaders/avaritia/universium.vert", "shaders/avaritia/universium.frag");
        super.use();
        bindTextureSlot("texture0", 0);
        bindTextureSlot("cosmicTexture", 2);
        clear();
        MinecraftForge.EVENT_BUS.register(this);

        this.textureAtlas = TextureAtlas
                .createTextureAtlas(GTNHLib.RESOURCE_DOMAIN, "textures/avaritia/cosmic", 10);

        location_lightlevel = getUniformLocation("lightlevel");
        location_time = getUniformLocation("time");
        location_yaw = getUniformLocation("yaw");
        location_pitch = getUniformLocation("pitch");
        location_externalScale = getUniformLocation("externalScale");
        location_lightmix = getUniformLocation("lightmix");
        location_opacity = getUniformLocation("opacity");
        location_starColorMultiplier = getUniformLocation("starColorMultiplier");
        location_bgColor = getUniformLocation("bgColor");
        location_cosmicuvs = getUniformLocation("cosmicuvs");
        // AutoShaderUpdater.getInstance().registerShaderReload(
        // this,
        // GregTech.resourceDomain,
        // "shaders/universium.vert.glsl", "shaders/universium.frag.glsl"
        // );
    }

    public static UniversiumShader getInstance() {
        if (instance == null) {
            instance = new UniversiumShader();
        }
        return instance;
    }

    public static void ensureLoaded() {
        getInstance();
    }

    public static boolean inventoryRender = false;

    @SubscribeEvent
    public void makeCosmicStuffLessDumbInGUIs(GuiScreenEvent.DrawScreenEvent.Pre event) {
        inventoryRender = true;
    }

    @SubscribeEvent
    public void finishMakingCosmicStuffLessDumbInGUIs(GuiScreenEvent.DrawScreenEvent.Post event) {
        inventoryRender = false;
    }

    @Override
    public void use() {
        super.use();

        Minecraft mc = Minecraft.getMinecraft();

        float yaw = 0;
        float pitch = 0;
        float scale = 1.0f;

        if (!inventoryRender) {
            yaw = (float) ((mc.thePlayer.rotationYaw * 2 * Math.PI) / 360.0);
            pitch = -(float) ((mc.thePlayer.rotationPitch * 2 * Math.PI) / 360.0);
        } else {
            scale = 25.0f;
        }

        final int time = mc.thePlayer.ticksExisted;
        GL20.glUniform1f(location_time, time);

        GL20.glUniform1f(location_yaw, yaw);

        GL20.glUniform1f(location_pitch, pitch);

        GL20.glUniform3f(location_lightlevel, LIGHT_LEVEL[0], LIGHT_LEVEL[1], LIGHT_LEVEL[2]);

        GL20.glUniform1f(location_lightmix, 0.2f);

        GL20.glUniform1f(location_externalScale, scale);

        GL20.glUniform1f(location_opacity, cosmicOpacity);

        GL20.glUniform1f(location_starColorMultiplier, starColorMultiplier);
        starColorMultiplier = 1;

        if (BG_COLORS[0] != -1) {
            GL20.glUniform3f(location_bgColor, BG_COLORS[0], BG_COLORS[1], BG_COLORS[2]);
            BG_COLORS[0] = -1;
        } else {
            final float pulse = (System.currentTimeMillis() % 20_000) / 20_000f;
            GL20.glUniform3f(
                    location_bgColor,
                    0.1f,
                    MathHelper.sin((float) (pulse * Math.PI * 2)) * 0.075f + 0.225f,
                    MathHelper.cos((float) (pulse * Math.PI * 2)) * 0.05f + 0.3f);
        }

        textureAtlas.uploadUVBuffer(location_cosmicuvs);

        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        textureAtlas.bindTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public UniversiumShader setStarColorMultiplier(float multiplier) {
        starColorMultiplier = multiplier;
        return this;
    }

    public UniversiumShader setBackgroundColor(float r, float g, float b) {
        BG_COLORS[0] = r;
        BG_COLORS[1] = g;
        BG_COLORS[2] = b;
        return this;
    }

    public UniversiumShader setLightFromLocation(World world, int x, int y, int z) {
        if (world == null) {
            return setLightLevel(1.0f);
        }

        int coord = world.getLightBrightnessForSkyBlocks(x, y, z, 0);

        int[] map = ((EntityRendererAccessor) Minecraft.getMinecraft().entityRenderer).getLightmapColors();

        if (map == null) {
            return setLightLevel(1.0f);
        }

        int mx = (coord % 65536) / 16;
        int my = (coord / 65536) / 16;

        int lightColour = map[Math.max(0, Math.min(map.length - 1, my * 16 + mx))];

        return setLightLevel(
                ((lightColour >> 16) & 0xFF) / 256.0f,
                ((lightColour >> 8) & 0xFF) / 256.0f,
                ((lightColour) & 0xFF) / 256.0f);
    }

    public UniversiumShader setLightLevel(float level) {
        return setLightLevel(level, level, level);
    }

    public UniversiumShader setLightLevel(float r, float g, float b) {
        LIGHT_LEVEL[0] = Math.max(0.0f, Math.min(1.0f, r));
        LIGHT_LEVEL[1] = Math.max(0.0f, Math.min(1.0f, g));
        LIGHT_LEVEL[2] = Math.max(0.0f, Math.min(1.0f, b));
        return this;
    }
}
