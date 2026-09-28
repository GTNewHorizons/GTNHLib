package com.gtnewhorizon.gtnhlib.debugworld;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The "GTNH Debug" option in the world type button of the create world screen.
 */
public class DebugWorldType extends WorldType {

    /** Looks diagonally across the grid, which starts at 1/1 and grows towards positive x and z. */
    private static final float SPAWN_YAW = -45.0F;
    /** World time when the sun is highest, the brightest moment of the day. */
    private static final long MIDDAY = 6000L;

    private static DebugWorldType instance;

    private DebugWorldType() {
        super("gtnhlib_debug");
    }

    public static void register() {
        if (instance != null) return;
        instance = new DebugWorldType();
        MinecraftForge.EVENT_BUS.register(instance);
        FMLCommonHandler.instance().bus().register(instance);
    }

    public static boolean isDebugWorld(World world) {
        return instance != null && world.getWorldInfo().getTerrainType() == instance;
    }

    /** Shown on the world type button as is, since there is no translation for it. */
    @Override
    @SideOnly(Side.CLIENT)
    public String getTranslateName() {
        return "GTNH Debug";
    }

    @Override
    public WorldChunkManager getChunkManager(World world) {
        // A single biome everywhere, so grass and leaves have the same colour across the whole grid
        return new WorldChunkManagerHell(BiomeGenBase.plains, 0.5F);
    }

    @Override
    public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new DebugChunkProvider(world);
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return DebugWorldLayout.GRID_Y;
    }

    @Override
    public double getHorizon(World world) {
        return 0.0D;
    }

    @Override
    public boolean hasVoidParticles(boolean flag) {
        return false;
    }

    @Override
    public double voidFadeMagnitude() {
        return 1.0D;
    }

    /** Spawn every player at the same spot, right at the corner of the grid. */
    @Override
    public int getSpawnFuzz() {
        return 1;
    }

    /**
     * Only fires once, when the world is first created. Vanilla would search for a grass block to spawn on, which never
     * exists here. This is also where the world gets its starting game rules, which players can still change later with
     * /gamerule.
     */
    @SubscribeEvent
    public void onCreateSpawnPosition(WorldEvent.CreateSpawnPosition event) {
        if (!isDebugWorld(event.world)) return;
        event.world.getWorldInfo().setSpawnPosition(0, DebugWorldLayout.GRID_Y + 3, 0);

        GameRules gameRules = event.world.getGameRules();
        gameRules.setOrCreateGameRule("doDaylightCycle", "false");
        gameRules.setOrCreateGameRule("doMobSpawning", "false");
        event.world.getWorldInfo().setWorldTime(MIDDAY);

        event.setCanceled(true);
    }

    /**
     * Keeps the grid close to how it was generated. Right-clicking a block still does what the block does (like opening
     * a machine GUI), but the held item is never used, so nothing can be placed into the grid. Left-clicking (breaking)
     * is blocked completely.
     */
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isDebugWorld(event.world)) return;

        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            event.useItem = Event.Result.DENY;
        } else if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isDebugWorld(event.world)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        prepareDebugPlayer(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        prepareDebugPlayer(event.player);
    }

    /**
     * The world is an empty void, so there is nothing to stand on. Like the vanilla debug world (which uses spectator
     * mode, not available in 1.7.10), players are put in creative mode and set flying above the grid.
     */
    private static void prepareDebugPlayer(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP playerMP) || !isDebugWorld(playerMP.worldObj)) return;

        playerMP.setGameType(WorldSettings.GameType.CREATIVE);
        playerMP.capabilities.isFlying = true;
        playerMP.sendPlayerAbilities();

        // Vanilla places new players on the highest solid block below the spawn point, which in a void is nothing
        if (playerMP.posY < DebugWorldLayout.GRID_Y) {
            ChunkCoordinates spawn = playerMP.worldObj.getSpawnPoint();
            playerMP.playerNetServerHandler
                    .setPlayerLocation(spawn.posX + 0.5D, spawn.posY, spawn.posZ + 0.5D, SPAWN_YAW, 0.0F);
        }
    }
}
