package com.gtnewhorizon.gtnhlib.client.renderer.shader;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.mixins.early.EntityRendererAccessor;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

/// This was largely copied from avaritia
@EventBusSubscriber
public class UniverseShader extends ShaderProgram {

    public static final UniverseShader INSTANCE = new UniverseShader();

    private UniverseShader() {
        super(GTNHLib.MODID, "shaders/universe/universe.vert", "shaders/universe/universe.frag");
    }

    private static final int COSMIC_COUNT = 10;
    public static final String[] COSMIC_TEXTURES = new String[COSMIC_COUNT];

    static {
        for (int i = 0; i < COSMIC_COUNT; i++) {
            COSMIC_TEXTURES[i] = GTNHLib.MODID + ":universe/cosmic" + i;
        }
    }

    public static final FloatBuffer COSMIC_UVS = BufferUtils.createFloatBuffer(4 * COSMIC_TEXTURES.length);
    public static final IIcon[] COSMIC_ICONS = new IIcon[COSMIC_TEXTURES.length];

    public static final float[] LIGHT_LEVEL = new float[3];

    @SubscribeEvent
    public static void letsMakeAQuilt(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 1) {
            return;
        }

        for (int i = 0; i < COSMIC_TEXTURES.length; i++) {
            IIcon icon = event.map.registerIcon(COSMIC_TEXTURES[i]);
            COSMIC_ICONS[i] = icon;
        }
    }

    @SubscribeEvent
    public static void pushTheCosmicFancinessToTheLimit(RenderTickEvent event) {
        if (event.phase == Phase.START) {
            for (IIcon icon : COSMIC_ICONS) {
                COSMIC_UVS.put(icon.getMinU());
                COSMIC_UVS.put(icon.getMinV());
                COSMIC_UVS.put(icon.getMaxU());
                COSMIC_UVS.put(icon.getMaxV());
            }

            COSMIC_UVS.flip();
        }
    }

    public static boolean inventoryRender = false;
    public static float cosmicOpacity = 1.0f;

    @SubscribeEvent
    public static void makeCosmicStuffLessDumbInGUIs(GuiScreenEvent.DrawScreenEvent.Pre event) {
        inventoryRender = true;
    }

    @SubscribeEvent
    public static void finishMakingCosmicStuffLessDumbInGUIs(GuiScreenEvent.DrawScreenEvent.Post event) {
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

        int time2 = INSTANCE.getUniformLocation("time2");
        GL20.glUniform1f(time2, mc.thePlayer.ticksExisted);

        int x = INSTANCE.getUniformLocation("yaw");
        GL20.glUniform1f(x, yaw);

        int z = INSTANCE.getUniformLocation("pitch");
        GL20.glUniform1f(z, pitch);

        int l = INSTANCE.getUniformLocation("lightlevel");
        GL20.glUniform3f(l, LIGHT_LEVEL[0], LIGHT_LEVEL[1], LIGHT_LEVEL[2]);

        int lightmix = INSTANCE.getUniformLocation("lightmix");
        GL20.glUniform1f(lightmix, 0.2f);

        int uvs = INSTANCE.getUniformLocation("cosmicuvs");
        GL20.glUniformMatrix2(uvs, false, COSMIC_UVS);

        int s = INSTANCE.getUniformLocation("externalScale");
        GL20.glUniform1f(s, scale);

        int o = INSTANCE.getUniformLocation("opacity");
        GL20.glUniform1f(o, cosmicOpacity);
    }

    public static void setLightFromLocation(World world, int x, int y, int z) {
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

    public static void setLightLevel(float level) {
        setLightLevel(level, level, level);
    }

    public static void setLightLevel(float r, float g, float b) {
        LIGHT_LEVEL[0] = Math.max(0.0f, Math.min(1.0f, r));
        LIGHT_LEVEL[1] = Math.max(0.0f, Math.min(1.0f, g));
        LIGHT_LEVEL[2] = Math.max(0.0f, Math.min(1.0f, b));
    }
}
