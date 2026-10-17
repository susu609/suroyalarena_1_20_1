package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.arena.ArenaWaypoint;
import net.susu609.royalarena.bot.BotBrain;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.Formation;
import net.susu609.royalarena.combat.Separation;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.MatchState;
import net.susu609.royalarena.match.PendingDeploy;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure-Java checks for the bot. The harness repeats what ServerMatchSession
 * and BattleField do each tick: match tick, spawn ready deploys, bots play,
 * combat tick, movement with separation, slime children.
 */
public final class BotSmokeTest {
    private BotSmokeTest() {
    }

    public static void run() {
        botPlaysByTheRules();
        botDefendsItsHalf();
        botIsReproducible();
        botThreatensAnIdleOpponent();
        botVersusBotPlaysAFullMatch();
        botSettingsChangeItsPace();
        System.out.println("RoyalArena bot smoke tests: PASS");
    }

    /** A match plus its engine, ticked like the server does. */
    private static final class Harness {
        final MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        final CombatEngine engine = new CombatEngine(match);
        final List<BotBrain> bots = new ArrayList<>();
        final List<Played> played = new ArrayList<>();
        int tick;
        int deaths;

        record Played(int tick, TeamSide side, BattleCard card, int x, int z) {
        }

        Harness() {
            match.startBattle();
        }

        void tick() {
            tick++;
            match.tick();
            for (PendingDeploy deploy : match.drainReadyDeploys())
                spawn(deploy);
            for (BotBrain bot : bots) {
                BotBrain.Play play = bot.tick(match, engine);
                if (play == null)
                    continue;
                BattleCard card = match.player(bot.side()).deck()
                        .cardAt(play.slot());
                int before = match.player(bot.side()).elixirTenths();
                var result = match.tryDeploy(
                        bot.side(), play.slot(), play.x(), play.z());
                check(result.accepted(), "the server accepts every bot play: "
                        + result.reason() + " at " + play);
                check(match.player(bot.side()).elixirTenths() ==
                        before - card.cost() * 10, "the bot pays the card");
                played.add(new Played(tick, bot.side(), card, play.x(),
                        play.z()));
            }
            TickResult result = engine.tick();
            move(result);
            for (TickResult.Death death : result.deaths()) {
                deaths++;
                for (var child : death.children())
                    engine.add(UUID.randomUUID(), child);
            }
        }

        private void spawn(PendingDeploy deploy) {
            if (deploy.card().isSpell()) {
                engine.castSpell(deploy.side(), deploy.card(),
                        deploy.x() + 0.5D, deploy.z() + 0.5D);
                return;
            }
            TroopStats stats = deploy.card().stats();
            int sign = ArenaLayout.sideSign(deploy.side());
            double[][] offsets = Formation.offsets(stats.count(),
                    Math.max(0.8D, stats.radius() * 2.0D + 0.3D));
            for (double[] o : offsets) {
                ArenaWaypoint spot = match.arena().clampSpawn(
                        deploy.x() + 0.5D + o[0], deploy.z() + 0.5D + o[1],
                        sign);
                engine.add(UUID.randomUUID(), deploy.side(), stats, spot.x(),
                        spot.z());
            }
        }

        private void move(TickResult result) {
            Map<UUID, double[]> walk = new HashMap<>();
            for (TickResult.Move move : result.moves()) {
                CombatUnit unit = engine.unit(move.unit());
                if (unit == null)
                    continue;
                double dx = move.x() - unit.x();
                double dz = move.z() - unit.z();
                double distance = Math.hypot(dx, dz);
                if (distance < 1.0E-9D)
                    continue;
                double stride = Math.min(move.speed(), distance);
                walk.put(unit.id(), new double[] {dx / distance * stride,
                        dz / distance * stride});
            }
            List<CombatUnit> units = engine.units();
            double[][] motion = new double[units.size()][];
            for (int i = 0; i < units.size(); i++) {
                double[] own = walk.getOrDefault(units.get(i).id(),
                        new double[2]);
                double[] push =
                        Separation.offset(units.get(i), units, match.arena());
                motion[i] = new double[] {own[0] + push[0], own[1] + push[1]};
            }
            for (int i = 0; i < units.size(); i++) {
                units.get(i).setPosition(units.get(i).x() + motion[i][0],
                        units.get(i).z() + motion[i][1]);
            }
        }
    }

    // ------------------------------------------------------------- the tests

    private static void botPlaysByTheRules() {
        Harness h = new Harness();
        h.bots.add(new BotBrain(TeamSide.RED, 7L));
        for (int i = 0; i < 3600; i++)
            h.tick();
        check(h.played.size() >= 5,
                "the bot actually plays cards: " + h.played.size());
        check(h.played.size() <= 30, "the bot does not spam: " + h.played.size());
        int last = -1000;
        for (Harness.Played p : h.played) {
            check(p.side() == TeamSide.RED, "bot plays for its own side");
            check(h.match.arena().canDeploy(TeamSide.RED, p.x(), p.z()),
                    "bot deploys inside its own deploy area");
            check(p.tick() - last >= BotBrain.MIN_GAP_TICKS,
                    "bot waits between plays");
            last = p.tick();
        }
        check(h.match.player(TeamSide.RED).elixirTenths() >= 0,
                "elixir never goes negative");
        check(h.played.stream().map(Harness.Played::card).distinct().count() >= 3,
                "the bot uses several different cards");
        check(h.played.stream().noneMatch(p -> p.tick() < 10),
                "the bot waits for its first think");
    }

    private static void botDefendsItsHalf() {
        Harness h = new Harness();
        BotBrain bot = new BotBrain(TeamSide.RED, 3L);
        h.bots.add(bot);
        // A BLUE Iron Golem has walked onto RED's right-hand lane.
        CombatUnit intruder = h.engine.add(UUID.randomUUID(), TeamSide.BLUE,
                BattleCard.IRON_GOLEM.stats(), 6.0D, -6.0D);
        BattleCard best = null;
        double bestHealth = -1.0D;
        for (BattleCard card : h.match.player(TeamSide.RED).deck().hand()) {
            if (card.cost() > 5)
                continue;
            double health = card.stats().health() * card.stats().count();
            if (health > bestHealth) {
                best = card;
                bestHealth = health;
            }
        }
        for (int i = 0; i < 12 && h.played.isEmpty(); i++)
            h.tick();
        check(!h.played.isEmpty(), "the bot answers an intruder at once");
        Harness.Played first = h.played.get(0);
        check(first.tick() <= BotBrain.THINK_EVERY_TICKS,
                "the first think already defends");
        check(first.card() == best,
                "it picks the sturdiest affordable card: " + first.card() +
                        " vs " + best);
        check(Math.abs(first.x() - 6) <= 1, "defender goes to the same lane");
        check(first.z() <= -7 && first.z() >= -16,
                "defender stands behind the intruder on RED's half");
        check(intruder.alive(), "the test intruder is still there");
    }

    private static void botIsReproducible() {
        List<String> runs = new ArrayList<>();
        for (int run = 0; run < 2; run++) {
            Harness h = new Harness();
            h.bots.add(new BotBrain(TeamSide.RED, 99L));
            for (int i = 0; i < 1500; i++)
                h.tick();
            runs.add(h.played.toString());
        }
        check(runs.get(0).equals(runs.get(1)),
                "the same seed plays the same game");
        Harness other = new Harness();
        other.bots.add(new BotBrain(TeamSide.RED, 100L));
        for (int i = 0; i < 1500; i++)
            other.tick();
        check(!runs.get(0).equals(other.played.toString()),
                "a different seed plays differently");
    }

    private static void botThreatensAnIdleOpponent() {
        Harness h = new Harness();
        h.bots.add(new BotBrain(TeamSide.RED, 2L));
        for (int i = 0; i < MatchInstance.DEFAULT_BATTLE_TICKS; i++)
            h.tick();
        boolean princessFell =
                h.match.tower(TowerId.BLUE_LEFT).isDestroyed() ||
                        h.match.tower(TowerId.BLUE_RIGHT).isDestroyed();
        check(princessFell, "a bot that is never opposed takes a Princess tower");
    }

    private static void botVersusBotPlaysAFullMatch() {
        Harness h = new Harness();
        h.bots.add(new BotBrain(TeamSide.BLUE, 1L));
        h.bots.add(new BotBrain(TeamSide.RED, 2L));
        for (int i = 0; i < MatchInstance.DEFAULT_BATTLE_TICKS &&
                h.match.state() == MatchState.BATTLE; i++)
            h.tick();
        long blue = h.played.stream()
                .filter(p -> p.side() == TeamSide.BLUE).count();
        long red = h.played.stream()
                .filter(p -> p.side() == TeamSide.RED).count();
        check(blue >= 8 && red >= 8,
                "both bots keep playing for three minutes: " + blue + "/" + red);
        check(h.played.stream().anyMatch(p -> p.card().isSpell()),
                "the bots cast spells in a full match");
        check(h.deaths >= 10,
                "the bots' troops really fight each other: " + h.deaths +
                        " deaths");
        check(h.match.state() == MatchState.FINISHED ||
                h.match.ticksRemaining() >= 0, "the match stays consistent");
    }

    private static void botSettingsChangeItsPace() {
        int[] plays = new int[2];
        int[] attackElixir = {10, 3};
        for (int run = 0; run < 2; run++) {
            Harness h = new Harness();
            BotBrain bot = new BotBrain(TeamSide.RED, 11L);
            bot.setSettings(new net.susu609.royalarena.bot.BotSettings(
                    10, 40, attackElixir[run] * 10));
            h.bots.add(bot);
            for (int i = 0; i < 2400; i++)
                h.tick();
            plays[run] = h.played.size();
        }
        check(plays[1] > plays[0],
                "a lower attack threshold makes the bot play more: " +
                        plays[0] + " vs " + plays[1]);
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
