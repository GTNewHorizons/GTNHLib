package com.gtnewhorizon.gtnhlib.worldgen.structure.core;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

import org.joml.Vector3i;
import org.joml.primitives.AABBf;

import com.gtnewhorizon.gtnhlib.commands.CommandArgumentParser;
import com.gtnewhorizon.gtnhlib.util.VoxelAABB;
import com.gtnewhorizon.gtnhlib.visualization.BoxVisualizer;
import com.gtnewhorizon.gtnhlib.visualization.VisualizedBox;
import com.gtnewhorizon.gtnhlib.worldgen.structure.core.StructurePiece.StructureSocket;
import lombok.SneakyThrows;

public class StructurePieceCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "structure-piece";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/structure-piece (player name) [scan|place] [name|path] [x] [y] [z] (for scan: [origin x] [origin y] [origin z])";
    }

    private final HashMap<EntityPlayerMP, PendingStructure> players = new HashMap<>();

    @SneakyThrows
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        CommandArgumentParser parser = new CommandArgumentParser(this, args, sender);

        EntityPlayerMP player = parser.getPlayerIfNeeded();

        switch (parser.nextString().toLowerCase()) {
            case "begin" -> {
                PendingStructure structure = new PendingStructure();

                structure.origin = new Vector3i(
                    MathHelper.floor_double(player.posX),
                    MathHelper.floor_double(player.posY),
                    MathHelper.floor_double(player.posZ));

                players.put(player, structure);

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Started scan"));
            }
            case "set-origin" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                structure.origin = parser.nextBlockPos();

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Set origin to " + structure.origin));
            }
            case "set-scan-a" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                Vector3i v = parser.nextBlockPos();

                if (structure.scan == null) structure.scan = new VoxelAABB(v, v);

                structure.scan.a = v;

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Set Scan A to " + structure.scan.a));
            }
            case "set-scan-b" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                Vector3i v = parser.nextBlockPos();

                if (structure.scan == null) structure.scan = new VoxelAABB(v, v);

                structure.scan.b = v;

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Set Scan B to " + structure.scan.b));
            }
            case "set-bounds-a" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                Vector3i v = parser.nextBlockPos();

                if (structure.bounds == null) structure.bounds = new VoxelAABB(v, v);

                structure.bounds.a = v;

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Set Bounds A to " + structure.bounds.a));
            }
            case "set-bounds-b" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                Vector3i v = parser.nextBlockPos();

                if (structure.bounds == null) structure.bounds = new VoxelAABB(v, v);

                structure.bounds.b = v;

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Set Bounds B to " + structure.scan.b));
            }
            case "add-socket" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                StructureSocket socket = new StructureSocket();

                String name = parser.nextString();

                socket.category = parser.nextString();
                socket.pos = parser.nextBlockPos();
                socket.forward = parser.nextDirection();

                structure.sockets.put(name, socket);

                structure.send(player);

                sender.addChatMessage(new ChatComponentText("Added Socket " + name + "/" + socket.category + " at " + socket.pos + "/" + socket.forward));
            }
            case "remove-socket" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                String name = parser.nextString();

                if (structure.sockets.remove(name) != null) {
                    sender.addChatMessage(new ChatComponentText("Removed Socket " + name));
                } else {
                    sender.addChatMessage(new ChatComponentText("Could not find Socket " + name));
                }

                structure.send(player);
            }
            case "save" -> {
                PendingStructure structure = players.get(player);

                if (structure == null) {
                    throw new WrongUsageException("No structure started");
                }

                String pieceName = parser.nextString();

                VoxelAABB scan = structure.scan.clone();
                scan.origin = new Vector3i(structure.origin);

                StructurePiece piece = StructurePiece.fromWorld(player.worldObj, scan);

                if (structure.bounds != null) {
                    piece.aabb = structure.bounds.clone();
                    piece.aabb.origin = new Vector3i(structure.origin);
                    piece.aabb.moveOrigin(new Vector3i());
                }

                piece.sockets = new ArrayList<>();

                for (var socket : structure.sockets.values()) {
                    StructureSocket socket2 = new StructureSocket();

                    socket2.pos = new Vector3i(socket.pos).sub(structure.origin);
                    socket2.category = socket.category;
                    socket2.connectsTo = new ArrayList<>(socket.connectsTo);
                    socket2.forward = socket.forward;

                    piece.sockets.add(socket2);
                }

                byte[] data;

                if (pieceName.endsWith("nbt")) {
                    data = CompressedStreamTools.compress(StructurePiece.saveNBT(piece));
                } else {
                    data = StructurePiece.GSON.toJson(StructurePiece.saveJson(piece)).getBytes(StandardCharsets.UTF_8);
                }

                Path outfile = Paths.get("structure-pieces", pieceName).toAbsolutePath();

                if (!Files.exists(outfile.getParent())) {
                    Files.createDirectories(outfile.getParent());
                }

                Files.write(outfile, data);

                sender.addChatMessage(new ChatComponentText("Generated " + outfile));
            }
            case "place" -> {
                String pieceName = parser.nextString();

                Vector3i pos = parser.nextBlockPos();

                StructurePiece piece = StructurePiece.load(pieceName);

                assert piece != null;
                piece.place(player.worldObj, pos, null);
            }
            default -> {
                throw new WrongUsageException(this.getCommandUsage(sender));
            }
        }
    }

    private static class PendingStructure {
        public VoxelAABB bounds, scan;
        public Vector3i origin;
        public final HashMap<String, StructureSocket> sockets = new HashMap<>();

        public void send(EntityPlayerMP player) {
            List<VisualizedBox> boxes = new ArrayList<>();

            if (origin != null) {
                boxes.add(new VisualizedBox(
                    new Color(220, 180, 40, 100),
                    new AABBf(
                        origin.x, origin.y, origin.z,
                        origin.x + 1, origin.y + 1, origin.z + 1)).expand(0.01f));
            }

            sockets.forEach((s, socket) -> {
                AABBf center = new AABBf(socket.pos.x - 0.1f, socket.pos.y - 0.1f, socket.pos.z - 0.1f, socket.pos.x + 0.1f, socket.pos.y + 0.1f, socket.pos.z + 0.1f);

                float dx = socket.forward.offsetX * 0.3f;
                float dy = socket.forward.offsetY * 0.3f;
                float dz = socket.forward.offsetZ * 0.3f;

                center.union(new AABBf(center).translate(dx, dy, dz)).translate(0.5f, 0.5f, 0.5f);

                boxes.add(new VisualizedBox(new Color(80, 220, 80, 100), center));
            });

            if (bounds != null) {
                boxes.add(new VisualizedBox(
                    new Color(200, 40, 40, 100),
                    bounds.getAABB()).expand(0.01f));
            }

            if (scan != null) {
                boxes.add(new VisualizedBox(
                    new Color(60, 60, 200, 100),
                    scan.getAABB()).expand(0.01f));
            }

            BoxVisualizer.sendBoxes(player, Duration.ofDays(7), boxes, true);
        }
    }
}
