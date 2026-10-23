package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatStats;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.UUID;

/**
 * Pure-Java checks for the mobs that bring their own rules: the Enderman's
 * teleport, the Cave Spider's poison, the Blaze's river and frost rules, the
 * Drowned on the river and the Vex that dies when it strikes. Units stand
 * still unless the test walks them, so each rule is seen on its own.
 */
public final class NewMobsSmokeTest {
    private NewMobsSmokeTest() {
    }

    public static void run() {
        enderSnubsSpellsAndShots();
        enderDoesNotDodgeBlows();
        caveSpiderPoisons();
        blazeCrossesTheRiver();
        blazeIsHurtByFrostAndWater();
        drownedDeploysOnTheRiver();
        vexStrikesAgainAndFades();
        System.out.println("RoyalArena new mobs smoke tests: PASS");
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

    private static void enderSnubsSpellsAndShots() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit ender = unit(engine, BattleCard.ENDERMAN, TeamSide.BLUE,
                0.5D, 1.0D);
        // A spell lands on it: it steps out instead of taking the damage.
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, 0.5D, 1.0D);
        TickResult first = engine.tick();
        check(first.teleports().size() == 1 &&
                first.teleports().get(0).unit().equals(ender.id()),
                "the Enderman teleports away from a Fireball");
        near(ender.health(), BattleCard.ENDERMAN.stats().health(), "and takes no damage");
        TickResult.Teleport to = first.teleports().get(0);
        double moved = Math.hypot(to.x() - 0.5D, to.z() - 1.0D);
        check(moved > 3.5D, "it steps several blocks away: " + moved);
        // The Fireball's circle is 2.5 blocks plus the Enderman's own radius.
        check(moved > 2.5D + 0.4D, "to a spot out of the circle");
        check(ender.teleportCooldown() > 0, "then it waits for the cooldown");

        // Another spell within the cooldown hits it.
        for (int i = 0; i < 10; i++)
            engine.tick();
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, ender.x(),
                ender.z());
        TickResult second = engine.tick();
        check(second.teleports().isEmpty(), "no second dodge during the cooldown");
        near(ender.health(), BattleCard.ENDERMAN.stats().health() - 25.0D,
                "the Fireball hits it: 40 - 25");

        // Once the cooldown is over it dodges again (the burn ticks on meanwhile).
        for (int i = 0; i < 160; i++)
            engine.tick();
        double before = ender.health();
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, ender.x(),
                ender.z());
        check(engine.tick().teleports().size() == 1,
                "after the cooldown it dodges again");
        near(ender.health(), before, "and this time nothing is lost");
    }

    private static void enderDoesNotDodgeBlows() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit ender = unit(engine, BattleCard.ENDERMAN, TeamSide.BLUE,
                -6.0D, 5.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -6.0D, 3.9D);
        TickResult result = engine.tick();
        check(result.attacks().stream().anyMatch(
                a -> zombie.id().equals(a.attackerUnit())),
                "the zombie strikes the Enderman");
        check(result.teleports().isEmpty(), "a blow is not dodged");
        check(ender.health() < BattleCard.ENDERMAN.stats().health(), "and it takes the damage");
    }

    private static void caveSpiderPoisons() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        // Out of every tower's reach (they would shoot the dummy too).
        CombatUnit spider = unit(engine, BattleCard.CAVE_SPIDER, TeamSide.BLUE,
                0.0D, 2.6D);
        // A dummy that strikes once for nothing, so it does not fight back.
        CombatUnit dummy = engine.add(UUID.randomUUID(), TeamSide.RED,
                new TroopStats(1, 200, 0.0D, 100000, 1.2D, 1.0D, 0.3D),
                0.0D, 1.5D);
        int hits = 0;
        for (int tick = 0; tick < 100; tick++) {
            for (TickResult.Attack a : engine.tick().landed()) {
                if (dummy.id().equals(a.victimUnit()) && a.damage() > 0.0D)
                    hits++;
            }
        }
        check(hits >= 4, "the Cave Spider strikes again and again: " + hits);
        double lost = 200.0D - dummy.health();
        double bite = BattleCard.CAVE_SPIDER.stats().damage();
        check(lost > hits * bite + 1.0D, "poison adds damage on top: lost " +
                lost + " from " + hits + " bites");
        near(engine.stats().card(TeamSide.BLUE, BattleCard.CAVE_SPIDER)
                .damage(), lost, "all of it counts for the Cave Spider");
        check(!spider.poisoned(), "the spider itself is not poisoned");
    }

    private static void blazeCrossesTheRiver() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit blaze = unit(engine, BattleCard.BLAZE, TeamSide.BLUE,
                0.5D, 10.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                0.5D, 9.0D);
        TickResult result = engine.tick();
        TickResult.Move blazeMove = null;
        TickResult.Move zombieMove = null;
        for (TickResult.Move m : result.moves()) {
            if (m.unit().equals(blaze.id()))
                blazeMove = m;
            if (m.unit().equals(zombie.id()))
                zombieMove = m;
        }
        check(blazeMove != null && zombieMove != null, "both walk");
        ArenaPoint tower = match.arena().towerPoint(TowerId.RED_LEFT);
        near(blazeMove.x(), tower.x(), "the Blaze heads straight for its tower");
        near(blazeMove.z(), tower.z(), "over the river, not via the bridge");
        check(Math.abs(zombieMove.x() - 0.5D) > 1.0D,
                "the Zombie still walks to a bridge");
    }

    private static void blazeIsHurtByFrostAndWater() {
        // Arrow Storm's first volley: 7 on land, 1.5 x 7 on the river (the
        // three volleys, 21, kill a 20-health Blaze either way).
        double volley = BattleCard.ARROW_STORM.spell().damagePerPulse();
        MatchInstance land = battle();
        CombatEngine landEngine = new CombatEngine(land);
        CombatUnit blazeLand = unit(landEngine, BattleCard.BLAZE, TeamSide.BLUE,
                0.5D, 6.0D);
        landEngine.castSpell(TeamSide.RED, BattleCard.ARROW_STORM, 0.5D, 6.0D);
        landEngine.tick();
        double blaze = BattleCard.BLAZE.stats().health();
        near(blazeLand.health(), blaze - volley, "on land: one volley of 7");

        MatchInstance river = battle();
        CombatEngine riverEngine = new CombatEngine(river);
        CombatUnit blazeRiver = unit(riverEngine, BattleCard.BLAZE,
                TeamSide.BLUE, 0.5D, 0.5D);
        riverEngine.castSpell(TeamSide.RED, BattleCard.ARROW_STORM, 0.5D, 0.5D);
        riverEngine.tick();
        near(blazeRiver.health(), blaze - 1.5D * volley,
                "in the river: 1.5 x the volley");

        MatchInstance cold = battle();
        CombatEngine coldEngine = new CombatEngine(cold);
        CombatUnit blazeCold = unit(coldEngine, BattleCard.BLAZE, TeamSide.BLUE,
                0.5D, 6.0D);
        CombatUnit zombieCold = unit(coldEngine, BattleCard.ZOMBIE,
                TeamSide.BLUE, -6.0D, 6.0D);
        coldEngine.castSpell(TeamSide.RED, BattleCard.FREEZE, 0.5D, 6.0D);
        coldEngine.tick();
        near(blazeCold.health(), blaze - 16.0D, "Freeze does double to a Blaze");
        check(blazeCold.frozenTicks() == 160,
                "and freezes it twice as long: " + blazeCold.frozenTicks());
        check(zombieCold.frozenTicks() == 0, "nothing else is touched");
    }

    private static void drownedDeploysOnTheRiver() {
        ArenaLayout arena = ArenaLayout.standard(new ArenaPoint(0, 80, 0));
        check(arena.canPlay(TeamSide.BLUE, BattleCard.DROWNED, 0, 0),
                "a Drowned may be played on the river");
        check(!arena.canPlay(TeamSide.BLUE, BattleCard.ZOMBIE, 0, 0),
                "a Zombie may not");
        check(arena.canPlay(TeamSide.BLUE, BattleCard.DROWNED, 0, 5),
                "and a Drowned still plays on its own half");
        check(!arena.canPlay(TeamSide.BLUE, BattleCard.DROWNED, 0, -8),
                "but not on the enemy half");
        check(!arena.canPlay(TeamSide.BLUE, BattleCard.DROWNED, 40, 0),
                "nor outside the arena");
    }

    private static void vexStrikesAgainAndFades() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit vex = unit(engine, BattleCard.VEX, TeamSide.BLUE, -6.0D, 5.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -6.0D, 3.6D);
        TickResult result = engine.tick();
        check(result.attacks().stream().anyMatch(
                a -> vex.id().equals(a.attackerUnit())),
                "the Vex strikes");
        near(zombie.health(), 20.0D - BattleCard.VEX.stats().damage() *
                        BattleCard.ZOMBIE.stats().armorFactor(),
                "the blow is the Vex's damage, less the zombie's armour");
        check(vex.alive(), "and it does not die with the blow");
        // Alone, a Vex fades after its time, and not before.
        CombatEngine alone = new CombatEngine(battle());
        CombatUnit lone = unit(alone, BattleCard.VEX, TeamSide.BLUE, -6.0D,
                5.0D);
        for (int t = 0; t < BattleCard.VEX_TICKS - 2; t++)
            alone.tick();
        check(lone.alive(), "a Vex lives for its time");
        alone.tick();
        alone.tick();
        check(!lone.alive(), "then it fades");
        check(alone.stats().card(TeamSide.BLUE, BattleCard.VEX).kills() == 0,
                "a Vex that fades kills nothing");
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
