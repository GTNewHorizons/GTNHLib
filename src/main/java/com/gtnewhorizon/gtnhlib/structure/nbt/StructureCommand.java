package com.gtnewhorizon.gtnhlib.structure.nbt;

import static com.gtnewhorizon.gtnhlib.util.CommandUtils.argument;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.error;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.literal;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.success;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.structure.nbt.StructureOrientation.Mirror;
import com.gtnewhorizon.gtnhlib.structure.nbt.StructureOrientation.Rotation;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

/**
 * {@code /structure save <name> <x1> <y1> <z1> <x2> <y2> <z2>} writes an area to {@code structures/<name>.nbt};
 * {@code /structure load <name> [rotation] [mirror]} places it at the sender's position;
 * {@code /structure marker <data>} sets the text of the looked-at {@link BlockStructureMarker}. Coordinates accept
 * {@code ~} and {@code ~n} relative to the sender.
 */
public final class StructureCommand {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]+(/[a-z0-9_-]+)*");
    private static final String[] COORDS = { "x1", "y1", "z1", "x2", "y2", "z2" };

    private StructureCommand() {}

    public static void register() {
        RequiredArgumentBuilder<ICommandSender, String> coords = argument(COORDS[5], StringArgumentType.word())
                .executes(StructureCommand::save);
        for (int i = COORDS.length - 2; i >= 0; i--) {
            coords = argument(COORDS[i], StringArgumentType.word()).then(coords);
        }

        RequiredArgumentBuilder<ICommandSender, String> mirror = argument("mirror", StringArgumentType.word())
                .suggests(suggest(Mirror.values())).executes(
                        ctx -> load(
                                ctx,
                                StringArgumentType.getString(ctx, "rotation"),
                                StringArgumentType.getString(ctx, "mirror")));
        RequiredArgumentBuilder<ICommandSender, String> rotation = argument("rotation", StringArgumentType.word())
                .suggests(suggest(Rotation.values()))
                .executes(ctx -> load(ctx, StringArgumentType.getString(ctx, "rotation"), Mirror.NONE.getName()))
                .then(mirror);
        RequiredArgumentBuilder<ICommandSender, String> name = argument("name", StringArgumentType.string())
                .executes(ctx -> load(ctx, Rotation.NONE.getName(), Mirror.NONE.getName())).then(rotation);

        BrigadierApi.getCommandDispatcher().register(
                literal("structure").requires(src -> src.canCommandSenderUseCommand(2, "structure"))
                        .then(literal("save").then(argument("name", StringArgumentType.string()).then(coords)))
                        .then(
                                literal("marker").then(
                                        argument("data", StringArgumentType.greedyString())
                                                .executes(StructureCommand::marker)))
                        .then(literal("load").then(name)));
    }

    private static SuggestionProvider<ICommandSender> suggest(Enum<?>[] values) {
        return (ctx, builder) -> {
            for (Enum<?> value : values) {
                String name = value.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    @Nullable
    private static <E extends Enum<E>> E parseEnum(E[] values, String text) {
        for (E value : values) {
            if (value.name().equalsIgnoreCase(text)) {
                return value;
            }
        }
        return null;
    }

    private static int save(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();
        String name = getName(ctx);
        if (!NAME.matcher(name).matches()) {
            return error(sender, "gtnhlib.chat.structure.error.name", name);
        }
        ChunkCoordinates origin = sender.getPlayerCoordinates();
        int[] base = { origin.posX, origin.posY, origin.posZ };
        int[] c = new int[COORDS.length];
        for (int i = 0; i < COORDS.length; i++) {
            String text = StringArgumentType.getString(ctx, COORDS[i]);
            Integer value = parseCoordinate(text, base[i % 3]);
            if (value == null) {
                return error(sender, "gtnhlib.chat.structure.error.coordinate", text);
            }
            c[i] = value;
        }
        File file = getFile(name);
        try {
            NBTTagCompound structure = StructureSaver.save(sender.getEntityWorld(), c[0], c[1], c[2], c[3], c[4], c[5]);
            StructureSaver.write(structure, file);
        } catch (IOException e) {
            return error(sender, "gtnhlib.chat.structure.error.write", file.getPath(), e.getMessage());
        } catch (RuntimeException e) {
            GTNHLib.LOG.error("Failed to save structure {}", name, e);
            return error(sender, "gtnhlib.chat.structure.error.exception", e.toString());
        }
        return success(sender, "gtnhlib.chat.structure.saved", name, file.getPath());
    }

    private static int load(CommandContext<ICommandSender> ctx, String rotationText, String mirrorText) {
        ICommandSender sender = ctx.getSource();
        String name = getName(ctx);
        if (!NAME.matcher(name).matches()) {
            return error(sender, "gtnhlib.chat.structure.error.name", name);
        }
        Rotation rotation = parseEnum(Rotation.values(), rotationText);
        if (rotation == null) {
            return error(sender, "gtnhlib.chat.structure.error.rotation", rotationText);
        }
        Mirror mirror = parseEnum(Mirror.values(), mirrorText);
        if (mirror == null) {
            return error(sender, "gtnhlib.chat.structure.error.mirror", mirrorText);
        }
        File file = getFile(name);
        if (!file.isFile()) {
            return error(sender, "gtnhlib.chat.structure.error.missing", file.getPath());
        }
        NBTTagCompound compound;
        try (InputStream stream = new FileInputStream(file)) {
            compound = CompressedStreamTools.readCompressed(stream);
        } catch (IOException e) {
            return error(sender, "gtnhlib.chat.structure.error.read", file.getPath(), e.getMessage());
        }
        try {
            NBTStructure structure = new NBTStructure(compound, file.getPath(), BlockStateConverter.DEFAULT_INSTANCE) {

                @Override
                public BlockStateContainer setStructureBlockAction(BlockPos pos, BlockStateContainer below, String data,
                        StructureOrientation orientation) {
                    if (BlockStructureMarker.INSTANCE == null) {
                        return super.setStructureBlockAction(pos, below, data, orientation);
                    }
                    BlockStateContainer marker = new BlockStateContainer(BlockStructureMarker.INSTANCE, 0);
                    NBTTagCompound nbt = new NBTTagCompound();
                    nbt.setString(TileEntityStructureMarker.TAG_DATA, data);
                    marker.setCompound(nbt);
                    return marker;
                }
            };
            ChunkCoordinates pos = sender.getPlayerCoordinates();
            structure.placeStructure(
                    sender.getEntityWorld(),
                    new Random(),
                    pos.posX,
                    pos.posY,
                    pos.posZ,
                    StructureOrientation.of(rotation, mirror));
        } catch (RuntimeException e) {
            GTNHLib.LOG.error("Failed to load structure {}", name, e);
            return error(sender, "gtnhlib.chat.structure.error.exception", e.toString());
        }
        return success(sender, "gtnhlib.chat.structure.loaded", name);
    }

    private static int marker(CommandContext<ICommandSender> ctx) {
        ICommandSender sender = ctx.getSource();
        if (!(sender instanceof EntityPlayerMP player)) {
            return error(sender, "gtnhlib.chat.structure.error.marker");
        }
        MovingObjectPosition hit = getLookedAtBlock(player);
        TileEntity tile = hit == null ? null : player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
        if (!(tile instanceof TileEntityStructureMarker marker)) {
            return error(sender, "gtnhlib.chat.structure.error.marker");
        }
        String data = StringArgumentType.getString(ctx, "data");
        marker.setData(data);
        return success(sender, "gtnhlib.chat.structure.marker.data", data);
    }

    private static MovingObjectPosition getLookedAtBlock(EntityPlayerMP player) {
        double reach = player.theItemInWorldManager.getBlockReachDistance();
        Vec3 eyes = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(1);
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyes, end, true);
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? hit : null;
    }

    private static String getName(CommandContext<ICommandSender> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return name.endsWith(".nbt") ? name.substring(0, name.length() - 4) : name;
    }

    private static Integer parseCoordinate(String text, int base) {
        boolean relative = text.startsWith("~");
        String number = relative ? text.substring(1) : text;
        if (number.isEmpty()) {
            return relative ? base : null;
        }
        try {
            int value = Integer.parseInt(number);
            return relative ? base + value : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static File getFile(String name) {
        return new File(MinecraftServer.getServer().getFile("structures"), name + ".nbt");
    }
}
