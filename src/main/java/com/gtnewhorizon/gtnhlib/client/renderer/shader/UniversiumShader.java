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
public final class UniversiumShader extends ShaderProgram {

    private static UniversiumShader instance;

    private final TextureAtlas textureAtlas;

    private boolean inventoryRenderPass = false;
    private boolean renderInInventory;
    private boolean lastInventoryRender;

    private final int location_lightlevel;
    public final float[] LIGHT_LEVEL = new float[3];

    private final int location_time;

    private final int location_yaw;

    private final int location_pitch;

    private final int location_externalScale;

    private final int location_opacity;
    private float lastOpacity = -1;
    public static float cosmicOpacity = 1.0f;

    private final int location_starColorMultiplier;
    private float lastStarColorMultiplier = -1;
    private float starColorMultiplier = 1;

    private final int location_bgColor;
    private float bgColorR;
    private float bgColorG;
    private float bgColorB;
    private boolean useCustomColor;

    private final int location_cosmicuvs;

    private UniversiumShader() {
        super(GTNHLib.RESOURCE_DOMAIN, "shaders/avaritia/universium.vert", "shaders/avaritia/universium.frag");
        super.use();

        location_lightlevel = getUniformLocation("lightlevel");
        location_time = getUniformLocation("time");
        location_yaw = getUniformLocation("yaw");
        location_pitch = getUniformLocation("pitch");
        location_externalScale = getUniformLocation("externalScale");
        int location_lightmix = getUniformLocation("lightmix");
        location_opacity = getUniformLocation("opacity");
        location_starColorMultiplier = getUniformLocation("starColorMultiplier");
        location_bgColor = getUniformLocation("bgColor");
        location_cosmicuvs = getUniformLocation("cosmicuvs");

        GL20.glUniform1f(location_lightmix, 0.2f);

        bindTextureSlot("texture0", 0);
        bindTextureSlot("cosmicTexture", 2);

        unbind();

        MinecraftForge.EVENT_BUS.register(this);

        this.textureAtlas = TextureAtlas.createTextureAtlas(GTNHLib.RESOURCE_DOMAIN, "textures/avaritia/cosmic", 10);
    }

    public static UniversiumShader getInstance() {
        return instance;
    }

    public static void init() {
        instance = new UniversiumShader();
    }

    @SubscribeEvent
    public void makeCosmicStuffLessDumbInGUIs(GuiScreenEvent.DrawScreenEvent.Pre event) {
        inventoryRenderPass = true;
    }

    @SubscribeEvent
    public void finishMakingCosmicStuffLessDumbInGUIs(GuiScreenEvent.DrawScreenEvent.Post event) {
        inventoryRenderPass = false;
    }

    @Override
    public void use() {
        super.use();

        final float time = (System.currentTimeMillis() % 600_000) / 50f;
        GL20.glUniform1f(location_time, time);

        final boolean shouldRenderInventory = inventoryRenderPass || renderInInventory;

        if (shouldRenderInventory) {
            if (!lastInventoryRender) {
                GL20.glUniform1f(location_yaw, 0);
                GL20.glUniform1f(location_pitch, 0);
                GL20.glUniform1f(location_externalScale, 25);
                lastInventoryRender = true;
            }
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            float yaw = (float) ((mc.thePlayer.rotationYaw * 2 * Math.PI) / 360.0);
            float pitch = -(float) ((mc.thePlayer.rotationPitch * 2 * Math.PI) / 360.0);
            GL20.glUniform1f(location_yaw, yaw);

            GL20.glUniform1f(location_pitch, pitch);

            if (lastInventoryRender) {
                GL20.glUniform1f(location_externalScale, 1);
                lastInventoryRender = false;
            }
        }

        GL20.glUniform3f(location_lightlevel, LIGHT_LEVEL[0], LIGHT_LEVEL[1], LIGHT_LEVEL[2]);

        if (lastOpacity != cosmicOpacity) {
            GL20.glUniform1f(location_opacity, cosmicOpacity);

            lastOpacity = cosmicOpacity;
            cosmicOpacity = 1.0f;
        }

        if (lastStarColorMultiplier != starColorMultiplier) {
            GL20.glUniform1f(location_starColorMultiplier, starColorMultiplier);

            lastStarColorMultiplier = starColorMultiplier;
            starColorMultiplier = 1;
        }

        if (useCustomColor) {
            GL20.glUniform3f(location_bgColor, bgColorR, bgColorG, bgColorB);
            useCustomColor = false;
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

    public UniversiumShader setRenderInInventory() {
        renderInInventory = true;
        return this;
    }

    public UniversiumShader setBackgroundColor(float r, float g, float b) {
        bgColorR = r;
        bgColorG = g;
        bgColorB = b;
        useCustomColor = true;
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
                ((lightColour >> 16) & 0xFF) / 255.0f,
                ((lightColour >> 8) & 0xFF) / 255.0f,
                ((lightColour) & 0xFF) / 255.0f);
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
