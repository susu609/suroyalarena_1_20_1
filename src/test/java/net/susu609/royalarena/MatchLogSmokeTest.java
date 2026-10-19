package net.susu609.royalarena;

import net.susu609.royalarena.bot.BotDeck;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatStats;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchRecord;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.stats.MatchLog;
import net.susu609.royalarena.tower.TowerId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Pure-Java checks for the match log: the engine credits each card with the
 * health it really took, and a whole bot match comes out as one valid JSON
 * line whose numbers agree with the match.
 */
public final class MatchLogSmokeTest {
    private MatchLogSmokeTest() {
    }

    public static void run() {
        damageIsCreditedToTheCard();
        aBotMatchIsOneConsistentLine();
        playersAreAnonymous();
        System.out.println("RoyalArena match log tests: PASS");
    }

    private static void damageIsCreditedToTheCard() {
        MatchInstance match = Sim.battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit ravager = engine.add(UUID.randomUUID(), TeamSide.RED,
                BattleCard.RAVAGER.stats(), 0.5D, 0.0D);
        engine.add(UUID.randomUUID(), TeamSide.RED, BattleCard.ZOMBIE.stats(),
                -1.0D, -1.5D);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREBALL, 0.0D, 0.0D);
        engine.tick();
        CombatStats.CardStats fireball =
                engine.stats().card(TeamSide.BLUE, BattleCard.FIREBALL);
        near(fireball.damage(), 20.0D + 25.0D,
                "Fireball: the Zombie's 20 (all it had) and 25 on the Ravager");
        check(fireball.kills() == 1, "and the Zombie counts as a kill");
        for (int i = 0; i < 80; i++)
            engine.tick();
        near(fireball.damage(), 45.0D + 4.5D,
                "the burn (1.5 a second for 3 s) is the Fireball's too");
        near(ravager.health(), BattleCard.RAVAGER.stats().health() - 29.5D,
                "the Ravager lost exactly that");
    }

    private static void aBotMatchIsOneConsistentLine() {
        Map<BattleCard, Integer> plays = new EnumMap<>(BattleCard.class);
        Sim.Outcome outcome = Sim.botMatch(BotDeck.random(new Random(11)),
                BotDeck.random(new Random(12)), 77L, plays);
        MatchInstance match = outcome.match();
        CombatStats stats = outcome.engine().stats();
        check(match.record().endReason() != null, "the match ended: " +
                match.record().endReason());
        String line = MatchLog.line(match, stats, MatchLog.Side.player(
                UUID.randomUUID()), MatchLog.Side.bot(), "bot", "test",
                1_700_000_000_000L);
        check(!line.contains("\n"), "one line");
        @SuppressWarnings("unchecked")
        Map<String, Object> log = (Map<String, Object>) MiniJson.parse(line);
        check(num(log, "v") == MatchLog.FORMAT, "format version");
        near(num(log, "seconds"), Math.round(match.record().battleTicks() /
                2.0D) / 10.0D, "seconds of battle");
        check(log.get("winner").equals(match.winner() == null ? "draw" :
                match.winner() == TeamSide.BLUE ? "blue" : "red"), "winner");
        check(log.get("end").equals(match.record().endReason().id()),
                "end reason");
        check(log.get("started").equals("2023-11-14T22:13:20Z"),
                "start time in UTC");

        int[] played = new int[BattleCard.values().length];
        for (TeamSide side : TeamSide.values()) {
            Map<String, Object> s = map(log, side == TeamSide.BLUE ? "blue" :
                    "red");
            check(((List<?>) s.get("deck")).size() == 8, "8 cards in the deck");
            check(((List<?>) s.get("elixir")).size() ==
                    1 + match.record().battleTicks() / MatchRecord.SAMPLE_TICKS,
                    "an elixir sample every 10 s");
            Map<String, Object> cards = map(s, "cards");
            double towerDamage = 0.0D;
            for (BattleCard card : match.player(side).deck().cards()) {
                Map<String, Object> c = map(cards, card.id());
                int n = (int) num(c, "plays");
                played[card.ordinal()] += n;
                check(num(c, "elixir") == n * card.cost(),
                        card.id() + ": elixir spent = plays x cost");
                check((n == 0) == (c.get("firstPlay") == null),
                        card.id() + ": first play only when played");
                towerDamage += stats.card(side, card).towerDamage();
            }
            // All damage to a side's towers comes from the other side's cards.
            double lost = 0.0D;
            for (TowerId id : TowerId.values()) {
                if (id.side() != side)
                    lost += id.kind().maxHealth() -
                            match.tower(id).remaining();
            }
            near(towerDamage, lost, side + " cards took exactly the tower " +
                    "health the enemy lost");
        }
        for (BattleCard card : BattleCard.values())
            check(played[card.ordinal()] == plays.getOrDefault(card, 0),
                    card.id() + " plays match the accepted deploys");
        Map<String, Object> blue = map(log, "blue");
        check("player".equals(blue.get("kind")) &&
                blue.get("player") instanceof String,
                "a player side has an anonymous code");
        Map<String, Object> red = map(log, "red");
        check("bot".equals(red.get("kind")) && red.get("player") == null,
                "the bot has none");
    }

    private static void playersAreAnonymous() {
        UUID id = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        String code = MatchLog.anonymous(id);
        check(code.matches("[0-9a-f]{8}"), "8 hex characters: " + code);
        check(code.equals(MatchLog.anonymous(id)), "the same every time");
        check(!code.equals(MatchLog.anonymous(UUID.randomUUID())),
                "different players, different codes");
        check(!id.toString().contains(code), "not a piece of the id");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> m, String key) {
        Object v = m.get(key);
        check(v instanceof Map, key + " is an object");
        return (Map<String, Object>) v;
    }

    private static double num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        check(v instanceof Double, key + " is a number: " + v);
        return (Double) v;
    }

    private static void near(double actual, double expected, String name) {
        check(Math.abs(actual - expected) < 1.0E-6D,
                name + ": " + actual + " vs " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
