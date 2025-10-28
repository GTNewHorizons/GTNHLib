package com.gtnewhorizon.gtnhlib.blockstate.init;

import static com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry.registerProperty;
import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.lang.reflect.Type;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.TransformableProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.VectorTransformableProperty;
import com.gtnewhorizon.gtnhlib.blockstate.mixin.BlockSkullExt;
import com.gtnewhorizon.gtnhlib.blockstate.properties.AxisBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.AxisBlockProperty.AbstractAxisBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.AxisBlockProperty.Meta;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.BooleanBlockProperty.FlagBooleanBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.FloatBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.IntegerBlockProperty;
import com.gtnewhorizon.gtnhlib.geometry.Axis;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;
import com.gtnewhorizon.gtnhlib.geometry.VectorTransform;

class VanillaBlockProperties {

    static void initVanilla() {
        registerProperty(BlockRotatedPillar.class, AxisBlockProperty.axis(0b1100, axis -> switch (axis) {
            case X -> 0b100;
            case Z -> 0b1000;
            default -> 0;
        }, meta -> switch (meta) {
            case 0b100 -> Axis.X;
            case 0b1000 -> Axis.Z;
            default -> Axis.Y;
        }));

        registerProperty(BlockQuartz.class, new AbstractAxisBlockProperty("axis") {

            @Override
            public boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta,
                    @Nullable TileEntity tile) {
                return meta >= 2 && meta <= 4;
            }

            @Override
            public int getMeta(Axis value, int existing) {
                if (existing < 2 || existing > 4) return existing;

                return switch (value) {
                    case X -> 3;
                    case Y -> 2;
                    case Z -> 4;
                    case UNKNOWN -> 2;
                };
            }

            @Override
            public Axis getValue(int meta) {
                return switch (meta) {
                    case 3 -> Axis.X;
                    case 2 -> Axis.Y;
                    case 4 -> Axis.Z;
                    default -> Axis.UNKNOWN;
                };
            }
        });

        class RailModeProperty implements MetaBlockProperty<RailMode> {

            private final boolean canTurn;

            public RailModeProperty(boolean canTurn) {
                this.canTurn = canTurn;
            }

            @Override
            public String getName() {
                return "mode";
            }

            @Override
            public Type getType() {
                return RailMode.class;
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, OnlyNeedsMeta, WorldMutable, Config -> true;
                    default -> false;
                };
            }

            @Override
            public RailMode getValue(int meta) {
                return getRailMode(meta, canTurn);
            }

            public int getMeta(RailMode value, int existing) {
                return getRailMeta(value, getRailDirection(existing, canTurn), canTurn, false);
            }

            @Override
            public String stringify(RailMode value) {
                return value.toString();
            }

            @Override
            public RailMode parse(String text) throws InvalidPropertyTextException {
                return RailMode.parse(text);
            }
        }

        registerProperty(Blocks.rail, new RailModeProperty(true));

        registerProperty(
                Blocks.rail,
                DirectionBlockProperty.facing(
                        (dir, existing) -> getRailMeta(getRailMode(existing, true), dir, true, false),
                        meta -> getRailDirection(meta, true)));

        registerProperty(
                Arrays.asList(Blocks.golden_rail, Blocks.detector_rail, Blocks.activator_rail),
                new RailModeProperty(false));

        registerProperty(
                Arrays.asList(Blocks.golden_rail, Blocks.detector_rail, Blocks.activator_rail),
                DirectionBlockProperty.facing(
                        (dir, existing) -> getRailMeta(
                                getRailMode(existing, false),
                                dir,
                                false,
                                isRailPowered(existing)),
                        meta -> getRailDirection(meta, false)));

        BooleanBlockProperty powered = BooleanBlockProperty.flag("powered", 0b1000);

        registerProperty(Arrays.asList(Blocks.golden_rail, Blocks.detector_rail, Blocks.activator_rail), powered);

        registerProperty(BlockButton.class, powered);
        registerProperty(BlockButton.class, DirectionBlockProperty.facing(0b111, 3, 4, 1, 2, 0, 5));

        registerProperty(BlockTorch.class, DirectionBlockProperty.facing(-1, 3, 4, 1, 2, 0, 5));
        registerProperty(
                Blocks.redstone_torch,
                BooleanBlockProperty.blocks("powered", Blocks.unlit_redstone_torch, Blocks.redstone_torch));
        registerProperty(
                Blocks.unlit_redstone_torch,
                BooleanBlockProperty.blocks("powered", Blocks.unlit_redstone_torch, Blocks.redstone_torch));

        registerProperty(Blocks.lever, powered);
        registerProperty(Blocks.lever, new Meta() {

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, OnlyNeedsMeta, WorldMutable, Config -> true;
                    default -> false;
                };
            }

            @Override
            public boolean isValidAxis(Axis value) {
                return value == Axis.X || value == Axis.Z;
            }

            // 0 = on ceiling, X
            // 7 = on ceiling, Z
            // 6 = on floor, X
            // 5 = on floor, Z

            private static final int CEILING_X = 0, CEILING_Z = 7, FLOOR_X = 6, FLOOR_Z = 5;

            @Override
            public Axis getValue(int meta) {
                return switch (meta & 0b111) {
                    case CEILING_X, FLOOR_X -> Axis.X;
                    case CEILING_Z, FLOOR_Z -> Axis.Z;
                    default -> Axis.UNKNOWN;
                };
            }

            @Override
            public int getMeta(Axis value, int meta) {
                int power = meta & 0b1000;
                meta &= 0b111;

                boolean ceiling = meta == CEILING_X || meta == CEILING_Z;

                switch (getValue(meta)) {
                    case X -> meta = ceiling ? CEILING_X : FLOOR_X;
                    case Z -> meta = ceiling ? CEILING_Z : FLOOR_Z;
                }

                return meta | power;
            }
        });

        registerProperty(
                Blocks.lever,
                DirectionBlockProperty.facing((dir, existing) -> (existing & 0b1000) | switch (dir) {
                case NORTH -> 3;
                case SOUTH -> 4;
                case WEST -> 1;
                case EAST -> 2;
                case UP -> ((existing & 0b111) == 7) ? 7 : 0;
                case DOWN -> ((existing & 0b111) == 6) ? 6 : 5;
                default -> 0;
                }, meta -> switch (meta & 0b111) {
                case 3 -> NORTH;
                case 4 -> SOUTH;
                case 1 -> WEST;
                case 2 -> EAST;
                case 0, 7 -> UP;
                case 5, 6 -> DOWN;
                default -> UNKNOWN;
                }));

        registerProperty(BlockPistonBase.class, powered);
        registerProperty(BlockPistonBase.class, DirectionBlockProperty.facing(0b111, 3, 4, 1, 2, 0, 5));
        registerProperty(BlockPistonExtension.class, DirectionBlockProperty.facing(0b111, 3, 4, 1, 2, 0, 5));

        class SlabTopProperty extends FlagBooleanBlockProperty implements TransformableProperty<Boolean> {

            public SlabTopProperty() {
                super("top", 0b1000);
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, OnlyNeedsMeta, WorldMutable, Config, Transformable -> true;
                    default -> false;
                };
            }

            @Override
            public @NotNull Boolean transform(Boolean value, DirectionTransform transform) {
                ForgeDirection dir = transform.apply(value ? UP : DOWN);

                if (dir == UP) return true;
                if (dir == DOWN) return false;

                return value;
            }

            @Override
            public boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta,
                    @Nullable TileEntity tile) {
                return (block instanceof BlockSlab slab) && !slab.isOpaqueCube();
            }
        }

        registerProperty(BlockSlab.class, new SlabTopProperty());

        registerProperty(BlockStairs.class, DirectionBlockProperty.facing(0b11, dir -> switch (dir) {
            case EAST -> 0;
            case WEST -> 1;
            case SOUTH -> 2;
            case NORTH -> 3;
            default -> 0;
        }, meta -> switch (meta) {
            case 0 -> EAST;
            case 1 -> WEST;
            case 2 -> SOUTH;
            case 3 -> NORTH;
            default -> NORTH;
        }));

        registerProperty(BlockStairs.class, DirectionBlockProperty.facing(0b100, dir -> switch (dir) {
            case UP -> 0;
            case DOWN -> 0b100;
            default -> 0;
        }, meta -> switch (meta) {
            case 0 -> UP;
            case 0b100 -> DOWN;
            default -> UP;
        }).setName("up"));

        registerProperty(BlockChest.class, DirectionBlockProperty.facing(0b111, 2, 3, 4, 5, -1, -1));

        registerProperty(BlockAnvil.class, DirectionBlockProperty.facing(0b11, dir -> switch (dir) {
            case WEST -> 0;
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 3;
            default -> 0;
        }, meta -> switch (meta) {
            case 0 -> WEST;
            case 1 -> NORTH;
            case 2 -> EAST;
            case 3 -> SOUTH;
            default -> NORTH;
        }));

        registerProperty(
                BlockAnvil.class,
                IntegerBlockProperty.meta("damage", 0b1100, 2)
                        .map(Arrays.asList("undamaged", "slightly_damaged", "very_damaged", "broken")));

        registerProperty(Blocks.redstone_wire, IntegerBlockProperty.meta("power", 0b1111, 0));

        registerProperty(BlockFurnace.class, DirectionBlockProperty.facing());
        registerProperty(BlockFurnace.class, BooleanBlockProperty.blocks("lit", Blocks.furnace, Blocks.lit_furnace));

        class StandingSignRotationProperty
                implements FloatBlockProperty, MetaBlockProperty<Float>, VectorTransformableProperty<Float> {

            @Override
            public String getName() {
                return "rotation";
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, OnlyNeedsMeta, WorldMutable, Config, VectorTransformable -> true;
                    default -> false;
                };
            }

            @Override
            public boolean needsExisting() {
                return false;
            }

            @Override
            public int getMeta(Float value, int existing) {
                return (Math.round(value * 16f / 360f) % 16 + 16) % 16;
            }

            @Override
            public Float getValue(int meta) {
                return meta * 360f / 16f;
            }

            @Override
            public @NotNull Float transform(Float value, VectorTransform transform) {
                Vector3f v = new Vector3f(0, 0, 1).rotateAxis(value * (float) Math.PI * 2f / 360f, 0, 1, 0);

                transform.transform(v);

                float rotation = MathHelper.floor_double(Math.atan2(v.x, v.z) * 360d / Math.PI / 2d + 0.5);
                rotation = (rotation % 360 + 360) % 360;

                return rotation;
            }
        }

        registerProperty(Blocks.wall_sign, DirectionBlockProperty.facing());
        registerProperty(Blocks.standing_sign, new StandingSignRotationProperty());
        registerProperty(TileEntitySign.class, new BlockProperty<String>() {

            @Override
            public String getName() {
                return "text";
            }

            @Override
            public Type getType() {
                return String.class;
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, WorldMutable, Config -> true;
                    default -> false;
                };
            }

            @Override
            public boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta,
                    @Nullable TileEntity tile) {
                return tile instanceof TileEntitySign;
            }

            @Override
            public String getValue(IBlockAccess world, int x, int y, int z) {
                if (!(world.getTileEntity(x, y, z) instanceof TileEntitySign sign)) return "";

                return String.join("\n", sign.signText);
            }

            @Override
            public void setValue(World world, int x, int y, int z, String value) {
                if (!(world.getTileEntity(x, y, z) instanceof TileEntitySign sign)) return;

                String[] text = value.split("\n");
                sign.signText = new String[4];

                for (int i = 0; i < 4; i++) {
                    String line = BlockProperty.getIndexSafe(text, i);

                    if (line == null) line = "";
                    if (line.length() > 15) line = line.substring(0, 15);

                    sign.signText[i] = line;
                }

                sign.markDirty();
                world.markBlockForUpdate(x, y, z);
            }

            @Override
            public String stringify(String value) {
                return value;
            }

            @Override
            public String parse(String text) {
                return text;
            }
        });

        registerProperty(BlockDoor.class, new DirectionBlockProperty() {

            @Override
            public String getName() {
                return "facing";
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, WorldMutable, Config, Transformable -> true;
                    default -> false;
                };
            }

            @Override
            public boolean isValidDirection(ForgeDirection value) {
                return value.offsetY == 0;
            }

            @Override
            public ForgeDirection getValue(IBlockAccess world, int x, int y, int z) {
                int meta = world.getBlockMetadata(x, y, z);
                if (meta == 8) {
                    y--;
                    if (!(world.getBlock(x, y, z) instanceof BlockDoor)) return UNKNOWN;
                    meta = world.getBlockMetadata(x, y, z);
                }
                return switch (meta & 0b11) {
                    case 0 -> WEST;
                    case 1 -> NORTH;
                    case 2 -> EAST;
                    case 3 -> SOUTH;
                    default -> NORTH;
                };
            }

            @Override
            public void setValue(World world, int x, int y, int z, ForgeDirection value) {
                int meta = world.getBlockMetadata(x, y, z);
                if (meta == 8) {
                    y--;
                    if (!(world.getBlock(x, y, z) instanceof BlockDoor)) return;
                    meta = world.getBlockMetadata(x, y, z);
                }

                meta &= ~0b11;
                meta |= switch (value) {
                    case WEST -> 0;
                    case NORTH -> 1;
                    case EAST -> 2;
                    case SOUTH -> 3;
                    default -> 1;
                };

                world.setBlockMetadataWithNotify(x, y, z, meta, 2);
            }
        });
        registerProperty(BlockDoor.class, new BooleanBlockProperty() {

            @Override
            public String getName() {
                return "open";
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, WorldMutable, Config -> true;
                    default -> false;
                };
            }

            @Override
            public Boolean getValue(IBlockAccess world, int x, int y, int z) {
                int meta = world.getBlockMetadata(x, y, z);
                if (meta == 8) {
                    y--;
                    if (!(world.getBlock(x, y, z) instanceof BlockDoor)) return false;
                    meta = world.getBlockMetadata(x, y, z);
                }
                return meta >= 4;
            }

            @Override
            public void setValue(World world, int x, int y, int z, Boolean value) {
                int meta = world.getBlockMetadata(x, y, z);
                if (meta == 8) {
                    y--;
                    if (!(world.getBlock(x, y, z) instanceof BlockDoor)) return;
                    meta = world.getBlockMetadata(x, y, z);
                }

                meta &= ~0b100;
                if (value) meta |= 0b100;

                world.setBlockMetadataWithNotify(x, y, z, meta, 2);
            }
        });

        registerProperty(BlockLadder.class, DirectionBlockProperty.facing());

        registerProperty(BlockBasePressurePlate.class, BooleanBlockProperty.flag("powered", 0b1));

        registerProperty(BlockPumpkin.class, DirectionBlockProperty.facing(0b11, 2, 0, 1, 3, -1, -1));

        registerProperty(
                Arrays.asList(Blocks.unpowered_repeater, Blocks.powered_repeater),
                BooleanBlockProperty.blocks("powered", Blocks.unpowered_repeater, Blocks.powered_repeater));
        registerProperty(
                Arrays.asList(Blocks.unpowered_repeater, Blocks.powered_repeater),
                DirectionBlockProperty.facing(0b11, 0, 2, 3, 1, -1, -1));
        registerProperty(
                Arrays.asList(Blocks.unpowered_repeater, Blocks.powered_repeater),
                IntegerBlockProperty.meta("delay", 0b1100, 2));

        registerProperty(BlockTrapDoor.class, DirectionBlockProperty.facing(0b11, 0, 1, 2, 3, -1, -1));
        registerProperty(BlockTrapDoor.class, BooleanBlockProperty.flag("open", 0b100));
        registerProperty(
                BlockTrapDoor.class,
                DirectionBlockProperty.facing(0b1000, -1, -1, -1, -1, 0b1000, 0).setName("up"));

        registerProperty(
                BlockRedstoneLight.class,
                BooleanBlockProperty.blocks("powered", Blocks.redstone_lamp, Blocks.lit_redstone_lamp));

        registerProperty(Blocks.tripwire_hook, DirectionBlockProperty.facing(0b11, 0, 2, 3, 1, -1, -1));
        registerProperty(Blocks.tripwire_hook, powered);
        registerProperty(Blocks.tripwire_hook, BooleanBlockProperty.flag("connected", 0b100));

        class SkullRotationProperty implements IntegerBlockProperty, VectorTransformableProperty<Integer> {

            @Override
            public String getName() {
                return "rotation";
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, WorldMutable, Config, VectorTransformable -> true;
                    default -> false;
                };
            }

            @Override
            public Integer getValue(IBlockAccess world, int x, int y, int z) {
                if (!(world.getTileEntity(x, y, z) instanceof BlockSkullExt skull)) return 0;

                return skull.gtnhlib$getRotation() * 360 / 16;
            }

            @Override
            public void setValue(World world, int x, int y, int z, Integer value) {
                if (!(world.getTileEntity(x, y, z) instanceof BlockSkullExt skull)) return;

                skull.gtnhlib$setRotation(value * 16 / 360);

                world.markBlockForUpdate(x, y, z);
            }

            @Override
            public @NotNull Integer transform(Integer value, VectorTransform transform) {
                Vector3f v = new Vector3f(0, 0, 1).rotateAxis(value * (float) Math.PI * 2f / 360f, 0, 1, 0);

                transform.transform(v);

                int rotation = MathHelper.floor_double(Math.atan2(v.x, v.z) * 360d / Math.PI / 2d + 0.5);

                return (rotation % 360 + 360) % 360;
            }
        }

        registerProperty(TileEntitySkull.class, new SkullRotationProperty());

        registerProperty(BlockDispenser.class, DirectionBlockProperty.facing());

        registerProperty(
                Arrays.asList(Blocks.unpowered_comparator, Blocks.powered_comparator),
                BooleanBlockProperty.flag("powered", 0b1000));
        registerProperty(
                Arrays.asList(Blocks.unpowered_comparator, Blocks.powered_comparator),
                DirectionBlockProperty.facing(0b11, 0, 2, 3, 1, -1, -1));
        registerProperty(
                Arrays.asList(Blocks.unpowered_comparator, Blocks.powered_comparator),
                IntegerBlockProperty.meta("mode", 0b100, 2).map(Arrays.asList("comparator", "subtractor")));

        registerProperty(BlockHopper.class, DirectionBlockProperty.facing());

        registerProperty(BlockFenceGate.class, DirectionBlockProperty.facing(0b11, 2, 0, 1, 3, -1, -1));
        registerProperty(BlockFenceGate.class, BooleanBlockProperty.flag("open", 0b100));
    }

    public enum RailMode {

        NONE,
        ASCENDING,
        TURNED;

        public String toString() {
            return switch (this) {
                case NONE -> "none";
                case ASCENDING -> "ascending";
                case TURNED -> "turned";
            };
        }

        public static RailMode parse(String name) throws InvalidPropertyTextException {
            if ("turned".equals(name)) return TURNED;
            if ("ascending".equals(name)) return ASCENDING;
            if ("none".equals(name)) return NONE;
            throw new InvalidPropertyTextException("Illegal rail mode: '" + name + "'");
        }
    }

    private static RailMode getRailMode(int meta, boolean canTurn) {
        return canTurn && meta >= 6 ? RailMode.TURNED : meta >= 2 ? RailMode.ASCENDING : RailMode.NONE;
    }

    private static ForgeDirection getRailDirection(int meta, boolean canTurn) {
        if (canTurn) {
            return switch (meta) {
                case 0, 4, 6 -> NORTH;
                case 1, 3, 9 -> WEST;
                case 2, 7 -> EAST;
                case 5, 8 -> SOUTH;
                default -> NORTH;
            };
        } else {
            return switch (meta) {
                case 0, 4 -> NORTH;
                case 1, 3 -> WEST;
                case 2 -> EAST;
                case 5 -> SOUTH;
                default -> NORTH;
            };
        }
    }

    private static boolean isRailPowered(int meta) {
        return meta >= 8;
    }

    private static int getRailMeta(RailMode mode, ForgeDirection dir, boolean canTurn, boolean powered) {
        if (canTurn && mode == RailMode.TURNED) {
            return switch (dir) {
                case NORTH -> 6;
                case WEST -> 9;
                case EAST -> 7;
                case SOUTH -> 8;
                default -> 0;
            };
        }

        if (mode == RailMode.ASCENDING) {
            return switch (dir) {
                case NORTH -> 4;
                case WEST -> 3;
                case EAST -> 2;
                case SOUTH -> 5;
                default -> 0;
            };
        }

        return switch (dir) {
            case NORTH, SOUTH -> 0;
            case EAST, WEST -> 1;
            default -> 0;
        };
    }

}
