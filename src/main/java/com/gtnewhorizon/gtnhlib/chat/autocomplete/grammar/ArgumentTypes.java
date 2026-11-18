package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.data.Lazy;

/// The argument types used by the built-in command grammars.
public final class ArgumentTypes {

    /// Purely numeric names are valid, but rare. Treating them as numbers lets coordinate overloads be told apart from
    /// player overloads.
    private static final Pattern PLAYER = Pattern.compile("(?!-?\\d+$)[A-Za-z0-9_]{1,16}|@[par](\\[[^\\]\\s]*])?");
    private static final Pattern COORDINATE = Pattern.compile("~|~?-?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern EFFECT = Pattern.compile("[A-Za-z0-9_.]+");
    private static final Pattern REGISTRY_NAME = Pattern.compile("[A-Za-z0-9_.:|-]+");
    private static final Pattern STAT = Pattern.compile("\\*|[A-Za-z0-9_.:-]+");
    private static final Pattern WORD = Pattern.compile("\\S+");

    /// Registries don't change once the game has started, so they're sorted once, on first use.
    private static final Lazy<List<String>> ITEM_NAMES = new Lazy<>(() -> getSortedKeys(Item.itemRegistry.getKeys()));
    private static final Lazy<List<String>> BLOCK_NAMES = new Lazy<>(
            () -> getSortedKeys(Block.blockRegistry.getKeys()));
    private static final Lazy<List<String>> ENTITY_NAMES = new Lazy<>(() -> getSortedKeys(EntityList.func_151515_b()));

    private static final List<String> SELECTORS = Arrays.asList("@p", "@a", "@r");

    /// How far away a block can be and still be suggested as a coordinate.
    private static final double TARGET_REACH = 5;

    /// posY is the eye position on the client but the foot position on the server, so the eye height is measured from
    /// the bounding box instead, which is the same on both sides.
    private static final double PLAYER_EYE_HEIGHT = 1.62;

    private ArgumentTypes() {}

    public static ArgumentType literal(String word) {
        return new ArgumentType(Pattern.compile(Pattern.quote(word)), sender -> Collections.singletonList(word), true);
    }

    public static ArgumentType player() {
        return new ArgumentType(PLAYER, ArgumentTypes::suggestPlayers, false);
    }

    /// @param axis 0 for x, 1 for y, 2 for z. Suggestions fill in this axis and every axis after it.
    public static ArgumentType coordinate(int axis) {
        return new ArgumentType(COORDINATE, sender -> suggestCoordinates(sender, axis), false);
    }

    public static ArgumentType integer() {
        return new ArgumentType(INTEGER, sender -> Collections.emptyList(), false);
    }

    public static ArgumentType effect() {
        return new ArgumentType(EFFECT, sender -> suggestEffects(), false);
    }

    public static ArgumentType word() {
        return new ArgumentType(WORD, sender -> Collections.emptyList(), false);
    }

    public static ArgumentType number() {
        return new ArgumentType(NUMBER, sender -> Collections.emptyList(), false);
    }

    /// A single relative or absolute coordinate, for commands that take a lone axis (such as x and z without a y).
    public static ArgumentType relativeCoordinate() {
        return new ArgumentType(COORDINATE, sender -> Collections.singletonList("~"), false);
    }

    /// One of a fixed set of values, ignoring case. Unlike a literal, this is a value rather than a subcommand, so it
    /// doesn't win over other arguments and is shown as `<name>` in the hint.
    public static ArgumentType oneOf(String... values) {
        return pattern(Arrays.stream(values).map(Pattern::quote).collect(Collectors.joining("|")), values);
    }

    /// A value matching the regex (ignoring case), with a fixed list of suggestions. Use this when a command accepts
    /// shorthands that shouldn't be suggested, such as `c` for `creative`.
    public static ArgumentType pattern(String regex, String... suggestions) {
        List<String> suggestionList = Arrays.asList(suggestions);

        return new ArgumentType(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), sender -> suggestionList, false);
    }

    public static ArgumentType bool() {
        return oneOf("true", "false");
    }

    public static ArgumentType gameMode() {
        return pattern("survival|creative|adventure|[sca]|[0-2]", "survival", "creative", "adventure");
    }

    public static ArgumentType difficulty() {
        return pattern("peaceful|easy|normal|hard|[penh]|[0-3]", "peaceful", "easy", "normal", "hard");
    }

    public static ArgumentType item() {
        return new ArgumentType(REGISTRY_NAME, sender -> ITEM_NAMES.get(), false);
    }

    public static ArgumentType block() {
        return new ArgumentType(REGISTRY_NAME, sender -> BLOCK_NAMES.get(), false);
    }

    public static ArgumentType entity() {
        return new ArgumentType(REGISTRY_NAME, sender -> ENTITY_NAMES.get(), false);
    }

    public static ArgumentType stat() {
        return new ArgumentType(STAT, sender -> getStatIds(), false);
    }

    public static ArgumentType gameRule() {
        return new ArgumentType(
                WORD,
                sender -> Arrays.asList(sender.getEntityWorld().getGameRules().getRules()),
                false);
    }

    /// Objectives on the sender's scoreboard. The client only knows the objectives the server has sent it.
    public static ArgumentType objective() {
        return new ArgumentType(WORD, ArgumentTypes::suggestObjectives, false);
    }

    /// A score holder: a player or fake player that has a score on any objective.
    public static ArgumentType scoreHolder() {
        return new ArgumentType(
                WORD,
                sender -> new ArrayList<>(sender.getEntityWorld().getScoreboard().getObjectiveNames()),
                false);
    }

    /// Teams on the sender's scoreboard. The client only knows the teams the server has sent it.
    public static ArgumentType team() {
        return new ArgumentType(
                WORD,
                sender -> new ArrayList<>(sender.getEntityWorld().getScoreboard().getTeamNames()),
                false);
    }

    public static ArgumentType criteria() {
        return new ArgumentType(WORD, sender -> new ArrayList<>(IScoreObjectiveCriteria.field_96643_a.keySet()), false);
    }

    public static ArgumentType color() {
        return new ArgumentType(WORD, sender -> new ArrayList<>(EnumChatFormatting.getValidValues(true, false)), false);
    }

    private static List<String> suggestPlayers(ICommandSender sender) {
        List<String> names = new ArrayList<>();

        MinecraftServer server = MinecraftServer.getServer();

        // the server knows every online player, while the client only knows the players it has loaded
        if (server != null && !sender.getEntityWorld().isRemote) {
            names.addAll(Arrays.asList(server.getAllUsernames()));
        } else {
            for (Object player : sender.getEntityWorld().playerEntities) {
                names.add(((EntityPlayer) player).getCommandSenderName());
            }
        }

        names.addAll(SELECTORS);

        return names;
    }

    private static List<String> suggestCoordinates(ICommandSender sender, int axis) {
        List<String> suggestions = new ArrayList<>(2);

        suggestions.add(String.join(" ", Collections.nCopies(3 - axis, "~")));

        int[] target = getTargetedBlock(sender);

        if (target != null) {
            suggestions
                    .add(Arrays.stream(target, axis, 3).mapToObj(Integer::toString).collect(Collectors.joining(" ")));
        }

        return suggestions;
    }

    /// The x, y, z of the block the sender is looking at, or null when it isn't a player or isn't looking at a block.
    @Nullable
    private static int[] getTargetedBlock(ICommandSender sender) {
        if (!(sender instanceof EntityPlayer player)) return null;

        Vec3 eye = Vec3.createVectorHelper(player.posX, player.boundingBox.minY + PLAYER_EYE_HEIGHT, player.posZ);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = eye.addVector(look.xCoord * TARGET_REACH, look.yCoord * TARGET_REACH, look.zCoord * TARGET_REACH);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eye, end);

        if (hit == null || hit.typeOfHit != MovingObjectType.BLOCK) return null;

        return new int[] { hit.blockX, hit.blockY, hit.blockZ };
    }

    /// Registry keys are untyped in 1.7.10, but are always strings. They're stored in hash maps, so they're sorted here
    /// to give the popup a stable, alphabetical order.
    ///
    /// Assumption: called after every mod has registered its content, since the result is cached.
    private static List<String> getSortedKeys(Set<?> keys) {
        return Collections.unmodifiableList(keys.stream().map(Object::toString).sorted().collect(Collectors.toList()));
    }

    /// Despite its MCP name, Scoreboard.getObjectiveNames() returns the score holders, not the objectives.
    private static List<String> suggestObjectives(ICommandSender sender) {
        List<String> names = new ArrayList<>();

        for (Object objective : sender.getEntityWorld().getScoreboard().getScoreObjectives()) {
            names.add(((ScoreObjective) objective).getName());
        }

        return names;
    }

    private static List<String> getStatIds() {
        List<String> ids = new ArrayList<>();

        for (Object stat : StatList.allStats) {
            ids.add(((StatBase) stat).statId);
        }

        return ids;
    }

    /// Names only work in /effect when the potion name mixin is applied, otherwise only numeric ids are accepted.
    private static List<String> suggestEffects() {
        return Arrays.stream(Potion.potionTypes).filter(Objects::nonNull).map(potion -> Integer.toString(potion.id))
                .collect(Collectors.toList());
    }
}
