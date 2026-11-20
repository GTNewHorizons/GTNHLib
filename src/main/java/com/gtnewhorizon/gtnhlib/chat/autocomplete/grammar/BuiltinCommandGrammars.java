package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.block;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.bool;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.color;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.coordinate;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.criteria;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.difficulty;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.enchant;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.entity;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.gameMode;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.gameRule;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.integer;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.item;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.number;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.objective;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.oneOf;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.pattern;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.player;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.relativeCoordinate;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.scoreHolder;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.stat;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.team;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.ArgumentTypes.word;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.choice;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.command;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.hintPriority;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.literal;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.optional;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.param;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.rest;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.seq;

/// Grammars for the vanilla commands, matching how 1.7.10 parses them (which doesn't always match their usage text).
public final class BuiltinCommandGrammars {

    private BuiltinCommandGrammars() {}

    public static void register(CommandGrammarDriver driver) {
        // commands without arguments are registered so that no other driver tries to suggest arguments for them
        driver.register(
                command(),
                "kill",
                "list",
                "seed",
                "publish",
                "save-all",
                "save-off",
                "save-on",
                "stop",
                "toggledownfall");

        driver.register(achievementGrammar(), "achievement");
        driver.register(command(param("player", player()), optional(rest("reason", word()))), "ban", "kick");
        driver.register(command(param("address|name", word()), optional(rest("reason", word()))), "ban-ip");
        driver.register(command(optional(param("list", oneOf("players", "ips")))), "banlist");
        driver.register(clearGrammar(), "clear");
        driver.register(command(param("action", oneOf("start", "stop"))), "debug");
        driver.register(command(param("mode", gameMode())), "defaultgamemode");
        driver.register(command(param("player", player())), "deop", "op", "testfor");
        driver.register(command(param("difficulty", difficulty())), "difficulty");
        driver.register(effectGrammar(), "effect");
        driver.register(
                command(
                        param("player", player()),
                        param("enchantment", enchant()),
                        optional(param("level", integer()))),
                "enchant");
        driver.register(command(param("mode", gameMode()), optional(param("player", player()))), "gamemode");
        driver.register(command(param("rule", gameRule()), optional(param("value", word()))), "gamerule");
        driver.register(giveGrammar(), "give");
        driver.register(command(optional(param("page|command", word()))), "help", "?");
        driver.register(command(rest("action", word())), "me");
        driver.register(command(param("name", word())), "pardon");
        driver.register(command(param("address", word())), "pardon-ip");
        driver.register(playSoundGrammar(), "playsound");
        driver.register(command(rest("message", word())), "say");
        driver.register(scoreboardGrammar(), "scoreboard");
        driver.register(setBlockGrammar(), "setblock");
        driver.register(command(param("minutes", integer())), "setidletimeout");
        driver.register(command(optional(xyz())), "setworldspawn");
        driver.register(command(optional(seq(param("player", player()), optional(xyz())))), "spawnpoint");
        driver.register(spreadPlayersGrammar(), "spreadplayers");
        driver.register(
                command(param("entity", entity()), optional(seq(xyz(), optional(rest("dataTag", word()))))),
                "summon");
        driver.register(command(param("player", player()), rest("message", word())), "tell", "w", "msg");
        driver.register(command(param("player", player()), rest("raw json message", word())), "tellraw");
        driver.register(testForBlockGrammar(), "testforblock");
        driver.register(timeGrammar(), "time");
        driver.register(teleportGrammar(), "tp");
        driver.register(
                command(param("weather", oneOf("clear", "rain", "thunder")), optional(param("seconds", integer()))),
                "weather");
        driver.register(whitelistGrammar(), "whitelist");
        driver.register(command(param("amount", pattern("-?\\d+[lL]?")), optional(param("player", player()))), "xp");
    }

    /// The x, y and z of a block position.
    private static GrammarNode xyz() {
        return seq(param("x", coordinate(0)), param("y", coordinate(1)), param("z", coordinate(2)));
    }

    /// `/achievement give <stat> [player]`, where the stat can be `*` for every achievement.
    private static CommandGrammar achievementGrammar() {
        return command(literal("give"), param("stat", stat()), optional(param("player", player())));
    }

    /// `/clear [player] [item] [data]`. The usage text says the player is required, but it defaults to the sender.
    private static CommandGrammar clearGrammar() {
        return command(
                optional(
                        seq(
                                param("player", player()),
                                optional(seq(param("item", item()), optional(param("data", integer())))))));
    }

    /// `/effect <player> clear` or `/effect <player> <effect> [seconds] [amplifier]`. The amplifier can only be given
    /// along with the duration. The hint shows the effect branch, since `clear` is suggested by the popup anyway.
    static CommandGrammar effectGrammar() {
        return command(
                param("player", player()),
                choice(
                        literal("clear"),
                        hintPriority(
                                1,
                                seq(
                                        param("effect", ArgumentTypes.effect()),
                                        optional(
                                                seq(
                                                        param("seconds", integer()),
                                                        optional(param("amplifier", integer()))))))));
    }

    /// `/give <player> <item> [amount] [data] [dataTag]`.
    private static CommandGrammar giveGrammar() {
        return command(
                param("player", player()),
                param("item", item()),
                optional(
                        seq(
                                param("amount", integer()),
                                optional(seq(param("data", integer()), optional(rest("dataTag", word())))))));
    }

    /// `/playsound <sound> <player> [x] [y] [z] [volume] [pitch] [minimumVolume]`. Each optional argument needs the
    /// ones
    /// before it.
    private static CommandGrammar playSoundGrammar() {
        return command(
                param("sound", word()),
                param("player", player()),
                optional(
                        seq(
                                xyz(),
                                optional(
                                        seq(
                                                param("volume", number()),
                                                optional(
                                                        seq(
                                                                param("pitch", number()),
                                                                optional(param("minimumVolume", number())))))))));
    }

    /// `/scoreboard <objectives|players|teams> ...`. Vanilla matches the subcommands ignoring case, but only lower
    /// case is recognised here.
    static CommandGrammar scoreboardGrammar() {
        GrammarNode objectives = seq(
                literal("objectives"),
                choice(
                        literal("list"),
                        seq(
                                literal("add"),
                                param("name", word()),
                                param("criteria", criteria()),
                                optional(rest("display name", word()))),
                        seq(literal("remove"), param("name", objective())),
                        seq(
                                literal("setdisplay"),
                                param("slot", oneOf("list", "sidebar", "belowName")),
                                optional(param("objective", objective())))));

        GrammarNode players = seq(
                literal("players"),
                choice(
                        seq(
                                literal("set"),
                                param("player", player()),
                                param("objective", objective()),
                                param("score", integer())),
                        seq(
                                literal("add"),
                                param("player", player()),
                                param("objective", objective()),
                                param("count", integer())),
                        seq(
                                literal("remove"),
                                param("player", player()),
                                param("objective", objective()),
                                param("count", integer())),
                        seq(literal("reset"), param("player", scoreHolder())),
                        seq(literal("list"), optional(param("player", scoreHolder())))));

        GrammarNode teams = seq(
                literal("teams"),
                choice(
                        seq(literal("list"), optional(param("team", team()))),
                        seq(literal("add"), param("name", word()), optional(rest("display name", word()))),
                        seq(literal("remove"), param("team", team())),
                        seq(literal("empty"), param("team", team())),
                        seq(literal("join"), param("team", team()), optional(rest("player", player()))),
                        seq(literal("leave"), optional(rest("player", player()))),
                        seq(
                                literal("option"),
                                param("team", team()),
                                choice(
                                        seq(literal("friendlyfire"), param("value", bool())),
                                        seq(literal("color"), param("value", color())),
                                        seq(literal("seeFriendlyInvisibles"), param("value", bool()))))));

        return command(choice(objectives, players, teams));
    }

    /// `/setblock <x> <y> <z> <block> [dataValue] [oldBlockHandling] [dataTag]`.
    private static CommandGrammar setBlockGrammar() {
        return command(
                xyz(),
                param("block", block()),
                optional(
                        seq(
                                param("dataValue", integer()),
                                optional(
                                        seq(
                                                param("oldBlockHandling", oneOf("replace", "destroy", "keep")),
                                                optional(rest("dataTag", word())))))));
    }

    /// `/spreadplayers <x> <z> <spreadDistance> <maxRange> <respectTeams> <player ...>`.
    private static CommandGrammar spreadPlayersGrammar() {
        return command(
                param("x", relativeCoordinate()),
                param("z", relativeCoordinate()),
                param("spreadDistance", number()),
                param("maxRange", number()),
                param("respectTeams", bool()),
                rest("player", player()));
    }

    /// `/testforblock <x> <y> <z> <block> [dataValue] [dataTag]`.
    private static CommandGrammar testForBlockGrammar() {
        return command(
                xyz(),
                param("block", block()),
                optional(seq(param("dataValue", integer()), optional(rest("dataTag", word())))));
    }

    /// `/time set <day|night|value>` or `/time add <value>`.
    private static CommandGrammar timeGrammar() {
        return command(
                choice(
                        seq(literal("set"), param("value", pattern("day|night|\\d+", "day", "night"))),
                        seq(literal("add"), param("value", integer()))));
    }

    /// `/tp [target] <destination>` or `/tp [target] <x> <y> <z>`. The hint shows the coordinate branch, since the
    /// destination branch is a single player name that the popup already suggests.
    static CommandGrammar teleportGrammar() {
        return command(
                optional(param("target", player())),
                choice(param("destination", player()), hintPriority(1, xyz())));
    }

    /// `/whitelist <on|off|list|reload>` or `/whitelist <add|remove> <player>`.
    private static CommandGrammar whitelistGrammar() {
        return command(
                choice(
                        literal("on"),
                        literal("off"),
                        literal("list"),
                        literal("reload"),
                        seq(literal("add"), param("player", player())),
                        seq(literal("remove"), param("player", player()))));
    }
}
