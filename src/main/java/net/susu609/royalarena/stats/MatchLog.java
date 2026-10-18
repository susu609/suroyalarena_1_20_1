package net.susu609.royalarena.stats;

import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatStats;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchRecord;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * One finished match as one line of JSON for the match log
 * ({@code royalarena/matches.jsonl}, docs/MATCH_LOG.md): who played which
 * deck, how it ended, and per card how often it was played, the elixir
 * spent and what it did. Players appear only as a short anonymous code.
 */
public final class MatchLog {
    /** Bumped when a field changes meaning or goes away. */
    public static final int FORMAT = 1;

    private MatchLog() {
    }

    /**
     * One side of the match: a player (anonymous code) or the bot, with the
     * bot's personality id (null for a player).
     */
    public record Side(String kind, String player, String personality) {
        public static Side bot() {
            return new Side("bot", null, null);
        }

        public static Side bot(String personality) {
            return new Side("bot", null, personality);
        }

        public static Side player(UUID id) {
            return new Side("player", anonymous(id), null);
        }
    }

    /**
     * The JSON line for a finished match. {@code mode} is "bot" or "pvp",
     * {@code mod} the mod version, {@code startedMillis} when it began.
     */
    public static String line(MatchInstance match, CombatStats stats,
            Side blue, Side red, String mode, String mod, long startedMillis)
    {
        MatchRecord record = match.record();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("v", FORMAT);
        m.put("mod", mod);
        m.put("id", match.matchId().toString().substring(0, 8));
        m.put("started", Instant.ofEpochMilli(startedMillis).toString());
        m.put("mode", mode);
        m.put("seconds", round(record.battleTicks() / 20.0D));
        m.put("overtime", match.overtime());
        MatchRecord.EndReason reason = record.endReason();
        m.put("end", reason == null ? null : reason.id());
        TeamSide winner = match.winner();
        m.put("winner", winner == null ? "draw" : side(winner));
        Map<String, Object> crowns = new LinkedHashMap<>();
        crowns.put("blue", match.crowns(TeamSide.BLUE));
        crowns.put("red", match.crowns(TeamSide.RED));
        m.put("crowns", crowns);
        Map<String, Object> towers = new LinkedHashMap<>();
        for (TowerId id : TowerId.values())
            towers.put(id.name().toLowerCase(Locale.ROOT),
                    match.tower(id).health());
        m.put("towers", towers);
        m.put("blue", side(match, stats, TeamSide.BLUE, blue));
        m.put("red", side(match, stats, TeamSide.RED, red));
        return Json.write(m);
    }

    private static Map<String, Object> side(MatchInstance match,
            CombatStats stats, TeamSide side, Side who)
    {
        MatchRecord record = match.record();
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("kind", who.kind());
        s.put("player", who.player());
        if (who.personality() != null)
            s.put("personality", who.personality());
        List<Object> deck = new ArrayList<>();
        Map<String, Object> cards = new LinkedHashMap<>();
        for (BattleCard card : match.player(side).deck().cards()) {
            deck.add(card.id());
            MatchRecord.Plays plays = record.plays(side, card);
            CombatStats.CardStats did = stats.card(side, card);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("plays", plays.count());
            c.put("elixir", plays.elixir());
            c.put("firstPlay", plays.firstTick() < 0 ? null :
                    round(plays.firstTick() / 20.0D));
            c.put("damage", round(did.damage()));
            c.put("towerDamage", round(did.towerDamage()));
            c.put("kills", did.kills());
            c.put("healing", round(did.healing()));
            cards.put(card.id(), c);
        }
        s.put("deck", deck);
        s.put("fullElixirSeconds",
                round(record.fullElixirTicks(side) / 20.0D));
        s.put("elixir", new ArrayList<Object>(record.elixirSamples(side)));
        s.put("towerShots", round(stats.towerShots(side)));
        s.put("cards", cards);
        return s;
    }

    private static String side(TeamSide side) {
        return side.name().toLowerCase(Locale.ROOT);
    }

    /**
     * A short code standing for a player: the same player always gets the
     * same code, but the code does not show who it is.
     */
    public static String anonymous(UUID id) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    ("royalarena:" + id).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 4; i++)
                out.append(String.format("%02x", hash[i]));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    private static double round(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }
}
