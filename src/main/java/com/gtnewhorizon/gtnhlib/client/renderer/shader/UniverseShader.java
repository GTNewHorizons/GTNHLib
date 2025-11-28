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
import com.gtnewhorizons.angelica.glsm.debug.OpenGLDebugging;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

// This was largely copied from avaritia
public class UniverseShader extends ShaderProgram {

    private static UniverseShader instance;

    public final float[] LIGHT_LEVEL = new float[3];

    private TextureAtlas textureAtlas;

    private UniverseShader() {
        super(GTNHLib.RESOURCE_DOMAIN, "shaders/universe/universe.vert", "shaders/universe/universe.frag");
        super.use();
        bindTextureSlot("texture0", 0);
        bindTextureSlot("cosmicTexture", 1);
        clear();
        MinecraftForge.EVENT_BUS.register(this);

        this.textureAtlas = TextureAtlas
                .createTextureAtlas(GTNHLib.RESOURCE_DOMAIN, "textures/items/universe/cosmic", 10);

        // AutoShaderUpdater.getInstance().registerShaderReload(
        // this,
        // GregTech.resourceDomain,
        // "shaders/universe.vert.glsl", "shaders/universe.frag.glsl"
        // );
    }

    public static UniverseShader getInstance() {
        if (instance == null) {
            instance = new UniverseShader();
        }
        return instance;
    }

    public static void load() {
        getInstance();
    }

    public static boolean inventoryRender = false;
    public static float cosmicOpacity = 1.0f;

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
        int time2 = getUniformLocation("time2");
        GL20.glUniform1f(time2, time);

        int x = getUniformLocation("yaw");
        GL20.glUniform1f(x, yaw);

        int z = getUniformLocation("pitch");
        GL20.glUniform1f(z, pitch);

        int l = getUniformLocation("lightlevel");
        GL20.glUniform3f(l, LIGHT_LEVEL[0], LIGHT_LEVEL[1], LIGHT_LEVEL[2]);

        final int lightmix = getUniformLocation("lightmix");
        GL20.glUniform1f(lightmix, 0.2f);

        // COSMIC_UVS.clear();
        // for (int i = 0; i < COSMIC_COUNT; i++) {
        // COSMIC_UVS.put(i / (float) COSMIC_COUNT);
        // COSMIC_UVS.put(i / (float) COSMIC_COUNT);
        // COSMIC_UVS.put((i + 1) / (float) COSMIC_COUNT);
        // COSMIC_UVS.put((i + 1) / (float) COSMIC_COUNT);
        // }
        // COSMIC_UVS.flip();

        // int uvs = getUniformLocation("cosmicuvs");
        // GL20.glUniformMatrix2(uvs, false, COSMIC_UVS);

        int s = getUniformLocation("externalScale");
        GL20.glUniform1f(s, scale);

        int o = getUniformLocation("opacity");
        GL20.glUniform1f(o, cosmicOpacity);

        int h = getUniformLocation("starColorMultiplier");
        GL20.glUniform1f(h, 1f);

        int c = getUniformLocation("bgColor");
        final float pulse = (time % 400) / 400f;
        GL20.glUniform3f(
                c,
                0.1f,
                MathHelper.sin((float) (pulse * Math.PI * 2)) * 0.075f + 0.225f,
                MathHelper.cos((float) (pulse * Math.PI * 2)) * 0.05f + 0.3f);
        // this.textureAtlas = TextureAtlas.createTextureAtlas(GTNHLib.RESOURCE_DOMAIN,
        // "textures/items/universe/cosmic", 10);

        int v = getUniformLocation("cosmicuvs");
        textureAtlas.uploadUVBuffer(v);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        textureAtlas.bindTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        OpenGLDebugging.checkGLSM();
    }

    public void setStarColorMultiplier(float multiplier) {
        int h = getUniformLocation("starColorMultiplier");
        GL20.glUniform1f(h, multiplier);
    }

    public void setBackgroundColor(float r, float g, float b) {
        int c = getUniformLocation("bgColor");
        GL20.glUniform3f(c, r, g, b);
    }

    public void setLightFromLocation(World world, int x, int y, int z) {
        if (world == null) {
            setLightLevel(1.0f);
            return;
        }

        int coord = world.getLightBrightnessForSkyBlocks(x, y, z, 0);

        int[] map = ((EntityRendererAccessor) Minecraft.getMinecraft().entityRenderer).getLightmapColors();

        if (map == null) {
            setLightLevel(1.0f);
            return;
        }

        int mx = (coord % 65536) / 16;
        int my = (coord / 65536) / 16;

        int lightColour = map[Math.max(0, Math.min(map.length - 1, my * 16 + mx))];

        setLightLevel(
                ((lightColour >> 16) & 0xFF) / 256.0f,
                ((lightColour >> 8) & 0xFF) / 256.0f,
                ((lightColour) & 0xFF) / 256.0f);
    }

    public void setLightLevel(float level) {
        setLightLevel(level, level, level);
    }

    public void setLightLevel(float r, float g, float b) {
        LIGHT_LEVEL[0] = Math.max(0.0f, Math.min(1.0f, r));
        LIGHT_LEVEL[1] = Math.max(0.0f, Math.min(1.0f, g));
        LIGHT_LEVEL[2] = Math.max(0.0f, Math.min(1.0f, b));
    }
}
