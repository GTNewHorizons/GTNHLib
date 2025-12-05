package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.joml.Vector3f;
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

    private boolean inventoryRenderPass;
    private boolean renderInInventory;
    private boolean lastInventoryRender;

    private final int location_lightlevel;
    private final Vector3f lightLevel = new Vector3f(1);
    private final Vector3f lastLightLevel = new Vector3f(-1);

    private final int location_time;

    private final int location_yaw;

    private final int location_pitch;

    private final int location_externalScale;

    private final int location_opacity;
    public static float cosmicOpacity = 1.0f;
    private float lastCosmicOpacity = -1;

    private final int location_starColorMultiplier;
    private final Vector3f starColorMultiplier = new Vector3f(MUL_R, MUL_G, MUL_B);
    private final Vector3f lastStarColorMultiplier = new Vector3f(-1);

    private final int location_starColorBase;
    private final Vector3f starColorBase = new Vector3f(BASE_R, BASE_G, BASE_B);
    private final Vector3f lastStarColorBase = new Vector3f(-1);

    private static final float MUL_R = 0.3f;
    private static final float MUL_G = 0.4f;
    private static final float MUL_B = 0.3f;

    private static final float BASE_R = 0.4f;
    private static final float BASE_G = 0.6f;
    private static final float BASE_B = 0.7f;


    private final int location_bgColor;
    private final Vector3f bgColor = new Vector3f();
    private final Vector3f lastBgColor = new Vector3f(-1);
    private boolean useCustomBGColor;

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
        location_starColorBase = getUniformLocation("starColorBase");

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
    // TODO This should probably get replaced by Uniform classes but i'm too lazy to implement those
    public void use() {
        super.use();

        final long sysTime = System.currentTimeMillis();
        final float time = (sysTime % 600_000) / 50f;
        GL20.glUniform1f(location_time, time);

        final boolean shouldRenderInventory = inventoryRenderPass || renderInInventory;
        if (shouldRenderInventory) {
            if (!lastInventoryRender) {
                // Setup inventory uniforms once
                GL20.glUniform1f(location_yaw, 0);
                GL20.glUniform1f(location_pitch, 0);
                GL20.glUniform1f(location_externalScale, 25);
                lastInventoryRender = true;
            }
            renderInInventory = false;
        } else {
            if (lastInventoryRender) {
                // Setup in-world uniforms once
                GL20.glUniform1f(location_externalScale, 1);
                lastInventoryRender = false;
            }
            Minecraft mc = Minecraft.getMinecraft();
            float yaw = (float) ((mc.thePlayer.rotationYaw * 2 * Math.PI) / 360.0);
            GL20.glUniform1f(location_yaw, yaw);

            float pitch = -(float) ((mc.thePlayer.rotationPitch * 2 * Math.PI) / 360.0);
            GL20.glUniform1f(location_pitch, pitch);
        }

        if (lastCosmicOpacity != cosmicOpacity) {
            GL20.glUniform1f(location_opacity, cosmicOpacity);

            lastCosmicOpacity = cosmicOpacity;
        }
        cosmicOpacity = 1.0f; // Reset back to default

        glUniform3f(location_starColorMultiplier, starColorMultiplier, lastStarColorMultiplier);
        glUniform3f(location_starColorBase, starColorBase, lastStarColorBase);
        // Reset back to default
        starColorMultiplier.set(MUL_R, MUL_G, MUL_B);
        starColorBase.set(BASE_R, BASE_G, BASE_B);

        glUniform3f(location_lightlevel, lastLightLevel, lightLevel);
        lightLevel.set(1, 1, 1); // Reset back to default

        if (!useCustomBGColor) {
            final float pulse = (sysTime % 20_000) / 20_000f;
            bgColor.set(0.1f,
                MathHelper.sin((float) (pulse * Math.PI * 2)) * 0.075f + 0.225f,
                MathHelper.cos((float) (pulse * Math.PI * 2)) * 0.05f + 0.3f);
        }
        glUniform3f(location_bgColor, bgColor, lastBgColor);
        useCustomBGColor = false;

        textureAtlas.uploadUVBuffer(location_cosmicuvs);

        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        textureAtlas.bindTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /**
     * The formula for the star color is the following: <br>
     * {@code starColor = random * mul + base}, <br>
     * where {@code random} is a variable calculated inside of the shader. <br>
     * If this method doesn't get called, the Shader will default to {@code mul = vec3(0.3, 0.4, 0.3)} and {@code base = vec3(0.4, 0.6, 0.7)}
     */
    public UniversiumShader setStarColor(float mulR, float mulG, float mulB, float baseR, float baseG, float baseB) {
        starColorMultiplier.set(mulR, mulG, mulB);
        starColorBase.set(baseR, baseG, baseB);
        return this;
    }

    public UniversiumShader setStarColor(float mul) {
        return setStarColor(MUL_R * mul, MUL_G * mul, MUL_B * mul, BASE_R * mul, BASE_G * mul, BASE_B * mul);
    }

    public UniversiumShader setRenderInInventory() {
        renderInInventory = true;
        return this;
    }

    public UniversiumShader setBackgroundColor(float r, float g, float b) {
        bgColor.set(r, g, b);
        useCustomBGColor = true;
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
        lightLevel.set(
            MathHelper.clamp_float(r, 0, 1),
            MathHelper.clamp_float(g, 0, 1),
            MathHelper.clamp_float(b, 0, 1)
        );
        return this;
    }
}
