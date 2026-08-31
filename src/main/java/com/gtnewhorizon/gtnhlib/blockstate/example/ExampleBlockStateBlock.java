package com.gtnewhorizon.gtnhlib.blockstate.example;

import java.lang.reflect.Type;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateImpl;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.FloatBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockState_IBAExt;
import com.gtnewhorizon.gtnhlib.blockstate.storage.NativeBlockStateAware;
import com.gtnewhorizon.gtnhlib.blockstate.storage.NativeBlockStates;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.tags.TagEvent.RegisterBlockTagsEvent;
import com.gtnewhorizon.gtnhlib.tags.TagEvent.RegisterEntityTagsEvent;
import com.gtnewhorizon.gtnhlib.tags.TagEvent.RegisterItemTagsEvent;
import com.gtnewhorizon.gtnhlib.util.DirectionUtil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Test block demonstrating native BlockState storage.
///
/// Metadata: bits 0-1 store horizontal facing (N=0, S=1, E=2, W=3). That's it —
/// heat lives entirely in extended block state, not in metadata.
///
/// When fire is placed in the facing direction, heat increases by 0.05/tick.
/// Without fire, heat decreases by 0.02/tick.
/// Color tints from cold blue → orange → white-hot based on the float heat value read
/// directly from extended state, including during chunk rendering (via MixinChunkCache_BlockStates).
public class ExampleBlockStateBlock extends Block implements NativeBlockStateAware {

    private static final int FACING_MASK = 0b0011;

    /// Horizontal facing (N/S/E/W) stored in metadata bits 0-1.
    public static final HorizontalFacingProperty FACING = new HorizontalFacingProperty();

    /// Heat level [0.0, 1.0] stored in extended block state.
    /// Has arbitrarily fine precision — far more than the 16 discrete values metadata could hold.
    public static final HeatProperty HEAT = new HeatProperty();

    public static final ExampleBlockStateBlock BLOCK_LIT = new ExampleBlockStateBlock(true);
    public static final ExampleBlockStateBlock BLOCK_UNLIT = new ExampleBlockStateBlock(false);

    private final boolean lit;

    public ExampleBlockStateBlock(boolean lit) {
        super(Material.rock);
        setHardness(1.5f);
        setLightLevel(lit ? 1f : 0f);
        this.lit = lit;
    }

    public static void register() {
        BLOCK_LIT.setBlockName("native_state_test_lit");
        GameRegistry.registerBlock(BLOCK_LIT, "native_state_test_lit");
        BLOCK_LIT.registerProperties();

        BLOCK_UNLIT.setBlockName("native_state_test");
        GameRegistry.registerBlock(BLOCK_UNLIT, "native_state_test");
        BLOCK_UNLIT.registerProperties();
    }

    public void registerProperties() {
        BlockPropertyRegistry.registerBlockItemProperty(this, FACING, ForgeDirection.NORTH);
        BlockPropertyRegistry.registerProperty(this, HEAT);
    }

    @Override
    public BlockState getDefaultState(@Nullable BlockStatePool pool) {
        BlockStateImpl state = new BlockStateImpl();
        state.setBlock(this);
        state.setPropertyValue(FACING, ForgeDirection.NORTH);
        state.setPropertyValue(HEAT, 0f);
        return state;
    }

    @Override
    public boolean isSameBlock(Block newBlock) {
        return newBlock == BLOCK_LIT || newBlock == BLOCK_UNLIT;
    }

    @Override
    public Block getBlockForState(BlockState state) {
        return state.getPropertyValue(HEAT) > 0.5F ? BLOCK_LIT : BLOCK_UNLIT;
    }

    @Override
    public int getMetaForState(BlockState state) {
        return state.getBlockMeta(0);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        world.scheduleBlockUpdate(x, y, z, this, 20);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        ForgeDirection dir = DirectionUtil.yawToDirection(placer.rotationYaw);
        world.setBlockMetadataWithNotify(x, y, z, encodeFacing(dir), 2);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) {
        if (!NativeBlockStates.isEnabled() || world.isRemote) return;

        ForgeDirection facing = decodeFacing(world.getBlockMetadata(x, y, z) & FACING_MASK);

        BlockState state = NativeBlockStates.getBlockState(null, world, x, y, z);
        if (state == null) return;

        Float heat = state.getPropertyValue(HEAT);
        if (heat == null) heat = 0f;

        boolean hasFire = world.getBlock(
            x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ) == Blocks.fire;
        float newHeat = Math.max(0f, Math.min(1f, heat + (hasFire ? 0.05f : -0.02f)));

        if (newHeat != heat) {
            state.setPropertyValue(HEAT, newHeat);
            NativeBlockStates.setBlockState(world, x, y, z, state, 3);
        }

        world.scheduleBlockUpdate(x, y, z, this, 20);
    }

    // --- Rendering ---

    @SideOnly(Side.CLIENT)
    @Override
    public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
        BlockStateStorage storage = BlockState_IBAExt.get(worldIn, x, y, z);
        if (storage == null) return COLD_COLOR;
        BlockState state = storage.getBlockStateUnsafe(x & 0xF, y & 0xF, z & 0xF);
        if (state == null) return COLD_COLOR;
        Float heat = state.getPropertyValue(HEAT);
        return heatToColor(heat != null ? heat : 0f);
    }

    @SideOnly(Side.CLIENT)
    private IIcon front, top;

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {
        this.blockIcon = reg.registerIcon("furnace_side");
        this.front = reg.registerIcon(lit ? "furnace_front_on" : "furnace_front_off");
        this.top = reg.registerIcon("furnace_top");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        ForgeDirection facing = FACING.getValue(meta);
        ForgeDirection side2 = ForgeDirection.getOrientation(side);

        if (side2 == facing) return front;
        if (side2.offsetY != 0) return top;
        return blockIcon;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(IBlockAccess worldIn, int x, int y, int z, int side) {
        ForgeDirection facing = FACING.getValue(worldIn, x, y, z);
        ForgeDirection side2 = ForgeDirection.getOrientation(side);

        if (side2 == facing) return front;
        if (side2.offsetY != 0) return top;
        return blockIcon;
    }

    private static final int COLD_COLOR = 0x4488FF;

    private static int heatToColor(float heat) {
        // 0.0 → cold blue (0x4488FF)
        // 0.5 → orange    (0xFF6600)
        // 1.0 → white-hot (0xFFFFFF)
        int r, g, b;
        if (heat < 0.5f) {
            float t = heat * 2f;
            r = lerp(0x44, 0xFF, t);
            g = lerp(0x88, 0x66, t);
            b = lerp(0xFF, 0x00, t);
        } else {
            float t = (heat - 0.5f) * 2f;
            r = 0xFF;
            g = lerp(0x66, 0xFF, t);
            b = lerp(0x00, 0xFF, t);
        }
        return r << 16 | g << 8 | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    // --- Meta helpers ---

    private static int encodeFacing(ForgeDirection dir) {
        return switch (dir) {
            case SOUTH -> 1;
            case EAST  -> 2;
            case WEST  -> 3;
            default    -> 0; // NORTH
        };
    }

    private static ForgeDirection decodeFacing(int bits) {
        return switch (bits) {
            case 1 -> ForgeDirection.SOUTH;
            case 2 -> ForgeDirection.EAST;
            case 3 -> ForgeDirection.WEST;
            default -> ForgeDirection.NORTH;
        };
    }

    // --- Property implementations ---

    private static final class HorizontalFacingProperty
            implements DirectionBlockProperty, MetaBlockProperty<ForgeDirection> {

        @Override
        public String getName() {
            return "facing";
        }

        @Override
        public boolean isValidDirection(ForgeDirection value) {
            return value == ForgeDirection.NORTH || value == ForgeDirection.SOUTH
                || value == ForgeDirection.EAST  || value == ForgeDirection.WEST;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, WorldMutable, OnlyNeedsMeta, SupportsStacks, StackMutable -> true;
                default -> false;
            };
        }

        @Override
        public boolean needsExisting() {
            return false; // facing occupies the only metadata bits; no preservation needed
        }

        @Override
        public int getMeta(ForgeDirection value, int existing) {
            return encodeFacing(value);
        }

        @Override
        public ForgeDirection getValue(int meta) {
            return decodeFacing(meta & FACING_MASK);
        }
    }

    private static final class HeatProperty implements FloatBlockProperty {

        @Override
        public String getName() {
            return "heat";
        }

        @Override
        public Type getType() {
            return float.class;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, WorldMutable -> true;
                default -> false;
            };
        }

        @Override
        public Float getValue(IBlockAccess world, int x, int y, int z) {
            BlockStateStorage storage = BlockState_IBAExt.get(world, x, y, z);
            if (storage == null) return 0f;
            BlockState state = storage.getBlockStateUnsafe(x & 0xF, y & 0xF, z & 0xF);
            if (state == null) return 0f;
            Float val = state.getPropertyValue(this);
            return val != null ? val : 0f;
        }

        @Override
        public void setValue(World world, int x, int y, int z, Float value) {
            if (!NativeBlockStates.isEnabled()) return;
            BlockState state = NativeBlockStates.getBlockState(null, world, x, y, z);
            if (state == null) return;
            state.setPropertyValue(this, value);
            NativeBlockStates.setBlockState(world, x, y, z, state, 3);
        }
    }
}
