package net.susu609.royalarena.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.bot.BotPersonality;
import net.susu609.royalarena.bot.BotSettings;

/**
 * Gameplay settings, file {@code config/royalarena-common.toml} (dev
 * environment: {@code run-client1/config/} for single player,
 * {@code run-server/config/} for the dedicated server). Read every tick, so
 * saving the file changes a running match.
 */
public final class ArenaCommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Troops.").push("troops");
    }

    public static final ForgeConfigSpec.DoubleValue SPEED_MULTIPLIER = BUILDER
            .comment("Multiplies the walking speed of every troop. " +
                    "1.0 = the card stats.")
            .defineInRange("speedMultiplier", 1.0D, 0.1D, 5.0D);

    public static final ForgeConfigSpec.BooleanValue TRIBES = BUILDER
            .comment("Tribe bonuses: a deck with 2, 4, 6 or 8 cards of one " +
                    "mob tribe makes those troops stronger (docs/TRIBES.md).")
            .define("tribes", true);

    static {
        BUILDER.pop();
        BUILDER.comment("The bot in solo matches.").push("bot");
    }

    public static final ForgeConfigSpec.IntValue ATTACK_MIN_ELIXIR = BUILDER
            .comment("Elixir the bot saves before it attacks. Lower = " +
                    "attacks sooner and more often (harder).")
            .defineInRange("attackMinElixir",
                    BotBrain.ATTACK_MIN_TENTHS / 10, 1, 10);

    public static final ForgeConfigSpec.IntValue THINK_EVERY_TICKS = BUILDER
            .comment("How often the bot looks at the board (20 ticks = 1 s). " +
                    "Lower = reacts faster.")
            .defineInRange("thinkEveryTicks", BotBrain.THINK_EVERY_TICKS, 1,
                    100);

    public static final ForgeConfigSpec.IntValue MIN_GAP_TICKS = BUILDER
            .comment("Fewest ticks between two bot plays.")
            .defineInRange("minGapTicks", BotBrain.MIN_GAP_TICKS, 0, 400);

    public static final ForgeConfigSpec.ConfigValue<String> PERSONALITY =
            BUILDER.comment("How the bot plays: random (a new one every " +
                            "match), tank_push, rush, counter or spell_siege.")
                    .define("personality", "random", value ->
                            value instanceof String id &&
                                    (id.trim().equalsIgnoreCase("random") ||
                                            BotPersonality.byId(id) != null));

    static {
        BUILDER.pop();
        BUILDER.comment("The arena world (the royalarena:arena dimension).")
                .push("arenaWorld");
    }

    public static final ForgeConfigSpec.BooleanValue PURGE_ARENA_WORLD_ON_START =
            BUILDER.comment("Delete the arena dimension's whole save folder " +
                            "(dimensions/royalarena/arena) every time the " +
                            "server starts (and when it stops), so it is " +
                            "always brand new. Only that folder of this " +
                            "world is ever touched. Without it, arenas left " +
                            "over from earlier matches are still cleared at " +
                            "start.")
                    .define("purgeArenaWorldOnStart", false);

    static {
        BUILDER.pop();
        BUILDER.comment("Debugging aids.").push("debug");
    }

    public static final ForgeConfigSpec.BooleanValue SHOW_ROUTES = BUILDER
            .comment("Draw particles from each walking troop to the point " +
                    "it is walking to.")
            .define("showRoutes", false);

    public static final ForgeConfigSpec.BooleanValue LOG_MOVEMENT = BUILDER
            .comment("Log, once a second per troop, where it is, where it " +
                    "wants to go and how far it really moved; warns when a " +
                    "troop that should walk does not move.")
            .define("logMovement", false);

    static {
        BUILDER.pop();
        BUILDER.comment("Match statistics.").push("stats");
    }

    public static final ForgeConfigSpec.BooleanValue RECORD_MATCHES = BUILDER
            .comment("Add one line per finished match to " +
                    "royalarena/matches.jsonl in the game folder: decks, " +
                    "result, and per card plays, elixir and damage. " +
                    "Players appear only as an anonymous code; the file " +
                    "stays on this computer.")
            .define("recordMatches", true);

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ArenaCommonConfig() {
    }

    /** The configured personality, or null to draw one per match. */
    public static BotPersonality botPersonality() {
        return BotPersonality.byId(PERSONALITY.get());
    }

    public static BotSettings botSettings() {
        return new BotSettings(THINK_EVERY_TICKS.get(), MIN_GAP_TICKS.get(),
                ATTACK_MIN_ELIXIR.get() * 10);
    }
}
