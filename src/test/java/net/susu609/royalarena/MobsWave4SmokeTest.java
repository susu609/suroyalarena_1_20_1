package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.UUID;

/**
 * Batch 2 of the vanilla-behaviour cards: the Zoglin's rage, the Killer
 * Rabbits' armour, the Chicken Jockey's two lives, the Panda's sneeze, the
 * Turtle's swim and the Camel's dash.
 */
public final class MobsWave4SmokeTest {
    private MobsWave4SmokeTest() {
    }

    public static void run() {
        zoglinRagesAsItBleeds();
        killerRabbitsAreArmouredBunnies();
        chickenJockeyHasTwoLives();
        pandaSneezesSmallEnemiesAway();
        turtleSwimsFast();
        camelDashesOnce();
        System.out.println("RoyalArena wave 4 mobs smoke tests: PASS");
    }

    private static MatchInstance battle() {
        MatchInstance match = MatchInstance.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ArenaLayout.standard(new ArenaPoint(0, 80, 0)));
        match.startBattle();
        return match;
    }

    private static CombatUnit unit(CombatEngine engine, BattleCard card,
            TeamSide side, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side, card.stats(), x, z);
    }

    /** Ticks between the first two attacks of a lone attacker on a tower. */
    private static int attackGap(CombatEngine engine, CombatUnit attacker) {
        int first = -1;
        for (int t = 0; t < 400; t++) {
            TickResult r = engine.tick();
            boolean hit = r.attacks().stream()
                    .anyMatch(a -> attacker.id().equals(a.attackerUnit()));
            if (hit) {
                if (first >= 0)
                    return t - first;
                first = t;
            }
        }
        return -1;
    }

    private static void zoglinRagesAsItBleeds() {
        MatchInstance match = battle();
        ArenaPoint tower = match.arena().towerPoint(
                net.susu609.royalarena.tower.TowerId.RED_LEFT);
        CombatEngine fresh = new CombatEngine(match);
        CombatUnit whole = unit(fresh, BattleCard.ZOGLIN, TeamSide.BLUE,
                tower.x(), tower.z() + 2.5D);
        int calm = attackGap(fresh, whole);
        check(calm == BattleCard.ZOGLIN.stats().attackTicks(),
                "a healthy Zoglin swings at its own pace: " + calm);
        // Hurt (a Fireball, then the tower's shots), it swings faster.
        MatchInstance hurtMatch = battle();
        CombatEngine hurt = new CombatEngine(hurtMatch);
        CombatUnit angry = unit(hurt, BattleCard.ZOGLIN, TeamSide.BLUE,
                tower.x(), tower.z() + 2.5D);
        hurt.castSpell(TeamSide.RED, BattleCard.FIREBALL, angry.x(), angry.z());
        // The swing it started before the Fireball keeps its pace; time
        // the next one.
        int seen = 0;
        int last = -1;
        int gap = -1;
        for (int t = 0; t < 400 && gap < 0; t++) {
            TickResult r = Sim.step(hurt, hurtMatch.arena());
            if (r.attacks().stream()
                    .anyMatch(a -> angry.id().equals(a.attackerUnit()))) {
                if (++seen == 3)
                    gap = t - last;
                last = t;
            }
        }
        check(gap > 0 && gap < calm, "a hurt Zoglin rages: " + gap + " < " +
                calm);
        check(BattleCard.ZOGLIN.stats().traits().buildingsOnly() &&
                        BattleCard.ZOGLIN.fireImmune(),
                "it only goes for towers, and never burns");
        check(BattleCard.ZOGLIN.rages() && !BattleCard.HOGLIN.rages(),
                "the Hoglin has no rage");
    }

    private static void killerRabbitsAreArmouredBunnies() {
        check(BattleCard.KILLER_RABBITS.stats().count() == 3 &&
                        BattleCard.KILLER_RABBITS.stats().armor() == 8.0D,
                "three rabbits with vanilla's 8 armour");
    }

    private static void chickenJockeyHasTwoLives() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit jockey = unit(engine, BattleCard.CHICKEN_JOCKEY,
                TeamSide.BLUE, 0.0D, 6.0D);
        engine.castSpell(TeamSide.RED, BattleCard.ZAP, 0.0D, 6.0D);
        TickResult r = engine.tick();
        check(!jockey.alive(), "a Zap downs the chicken (4 health)");
        TickResult.Death death = r.deaths().stream()
                .filter(d -> d.unit().equals(jockey.id())).findFirst()
                .orElse(null);
        check(death != null && death.children().size() == 1 &&
                        death.children().get(0).stats() ==
                                BattleCard.ZOMBIE.stats(),
                "and its baby zombie fights on");
    }

    private static void pandaSneezesSmallEnemiesAway() {
        CombatEngine engine = new CombatEngine(battle());
        // In the middle, out of the towers' reach, out of both their melee
        // reach and inside the sneeze (1.35 blocks between the bodies; this
        // test does not walk the units).
        CombatUnit panda = unit(engine, BattleCard.PANDA, TeamSide.BLUE, 0.0D,
                1.15D);
        CombatUnit fox = unit(engine, BattleCard.FOX, TeamSide.RED, 0.0D,
                -1.15D);
        boolean sneezed = false;
        boolean pushed = false;
        for (int t = 0; t < BattleCard.SNEEZE_TICKS + 2; t++) {
            TickResult r = engine.tick();
            sneezed |= r.tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.SNEEZE &&
                            e.unit().equals(panda.id()));
            pushed |= r.pushes().stream()
                    .anyMatch(p -> p.unit().equals(fox.id()));
        }
        check(sneezed, "the Panda sneezes within 4 s");
        check(pushed, "and blows the Fox back");
    }

    private static void turtleSwimsFast() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit land = unit(engine, BattleCard.TURTLE, TeamSide.BLUE,
                -6.0D, 8.0D);
        double landSpeed = engine.tick().moves().stream()
                .filter(m -> m.unit().equals(land.id())).findFirst()
                .orElseThrow().speed();
        MatchInstance match = battle();
        CombatEngine river = new CombatEngine(match);
        int riverZ = match.arena().center().z();
        CombatUnit swim = unit(river, BattleCard.TURTLE, TeamSide.BLUE, -6.0D,
                riverZ + 0.5D);
        double riverSpeed = river.tick().moves().stream()
                .filter(m -> m.unit().equals(swim.id())).findFirst()
                .orElseThrow().speed();
        check(Math.abs(riverSpeed - landSpeed * BattleCard.RIVER_SWIM) < 1.0E-6D,
                "three times as fast on the river: " + landSpeed + " / " +
                        riverSpeed);
    }

    private static void camelDashesOnce() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit camel = unit(engine, BattleCard.CAMEL, TeamSide.BLUE, 0.0D,
                3.0D);
        CombatUnit golem = unit(engine, BattleCard.IRON_GOLEM, TeamSide.RED,
                0.0D, 1.5D);
        double[] hits = new double[2];
        int n = 0;
        // The dash knocks the Golem back, so the units walk (Sim.step).
        for (int t = 0; t < 200 && n < 2; t++) {
            TickResult r = Sim.step(engine, match.arena());
            for (TickResult.Attack a : r.attacks()) {
                if (camel.id().equals(a.attackerUnit()))
                    hits[n++] = a.damage();
                if (n == 2)
                    break;
            }
        }
        check(n == 2, "two blows");
        check(Math.abs(hits[0] - hits[1] * BattleCard.DASH_DAMAGE) < 1.0E-6D,
                "the first is a dash, twice as hard: " + hits[0] + " / " +
                        hits[1]);
        check(golem.alive(), "the Golem survives a dash");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
