package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.ChildSpawn;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.UUID;

/**
 * Batch 1 of the vanilla-behaviour cards: the Cat keeps Creepers and
 * Phantoms away, the Frog swallows the smallest slimes, the Piglets only
 * go for towers, and Milk Bucket takes every effect off both sides.
 */
public final class MobsWave3SmokeTest {
    private MobsWave3SmokeTest() {
    }

    public static void run() {
        creepersKeepAwayFromCats();
        frogsSwallowTheSmallestSlimes();
        pigletsGoForTowers();
        milkClearsEveryEffectOnBothSides();
        System.out.println("RoyalArena wave 3 mobs smoke tests: PASS");
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

    private static void creepersKeepAwayFromCats() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit cat = unit(engine, BattleCard.CAT, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit creeper = unit(engine, BattleCard.CREEPER, TeamSide.RED,
                0.0D, 1.0D);
        TickResult first = engine.tick();
        check(first.attacks().stream()
                        .noneMatch(a -> creeper.id().equals(a.attackerUnit())),
                "a Creeper next to a Cat does not blow up");
        TickResult.Move move = first.moves().stream()
                .filter(m -> m.unit().equals(creeper.id())).findFirst()
                .orElse(null);
        check(move != null && move.z() < creeper.z(),
                "it backs away from the Cat: " + move);
        check(creeper.target() == null, "and drops its target");

        // A Zombie has no fear of cats.
        CombatEngine other = new CombatEngine(battle());
        unit(other, BattleCard.CAT, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit zombie = unit(other, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, 1.0D);
        other.tick();
        check(zombie.target() != null, "a Zombie still goes for the Cat");
    }

    private static void frogsSwallowTheSmallestSlimes() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit frog = unit(engine, BattleCard.FROG, TeamSide.BLUE, 0.0D,
                3.0D);
        TroopStats slime = BattleCard.BIG_SLIME.stats();
        int smallest = slime.split().generations();
        CombatUnit tiny = engine.add(UUID.randomUUID(), new ChildSpawn(
                TeamSide.RED, slime, smallest, 9.0D, 1.0D, 0.3D, 0.0D, 1.5D, 0,
                null));
        TickResult first = engine.tick();
        check(first.attacks().stream()
                        .anyMatch(a -> frog.id().equals(a.attackerUnit())),
                "the Frog licks");
        check(!tiny.alive(), "and the smallest slime is gone in one lick");

        CombatEngine big = new CombatEngine(battle());
        unit(big, BattleCard.FROG, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit whole = unit(big, BattleCard.BIG_SLIME, TeamSide.RED,
                0.0D, 1.0D);
        big.tick();
        check(whole.alive() && whole.health() >= whole.maxHealth() - 3.0D,
                "a big slime only takes a lick");
    }

    private static void pigletsGoForTowers() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit pig = unit(engine, BattleCard.PIGLETS, TeamSide.BLUE,
                -6.0D, -2.0D);
        unit(engine, BattleCard.ZOMBIE, TeamSide.RED, -6.0D, -3.5D);
        engine.tick();
        check(pig.target() == null, "Piglets ignore troops");
        check(BattleCard.PIGLETS.stats().count() == 4 &&
                        BattleCard.PIGLETS.modelScale() < 1.0F,
                "four small pigs");
    }

    private static void milkClearsEveryEffectOnBothSides() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit own = unit(engine, BattleCard.IRON_GOLEM, TeamSide.BLUE,
                0.0D, 2.0D);
        CombatUnit enemy = unit(engine, BattleCard.RAVAGER, TeamSide.RED,
                0.0D, -2.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.SWIFTNESS_POTION, 0.0D,
                2.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.FIREBALL, 0.0D, -2.0D);
        engine.castSpell(TeamSide.RED, BattleCard.COBWEB, 0.0D, 2.0D);
        engine.tick();
        check(own.hasted() && own.webbed() && enemy.burnTicks() > 0,
                "set up: haste and web on BLUE, burning on RED");
        // Engine spells keep pulsing; drop them so only the milk acts.
        engine.clearSpellsAndShots();
        engine.castSpell(TeamSide.BLUE, BattleCard.MILK_BUCKET, 0.0D, 0.0D);
        engine.tick();
        check(!own.hasted() && !own.webbed(),
                "milk takes the good and the bad off its own side");
        check(enemy.burnTicks() == 0, "and off the enemy too");
        check(BattleCard.MILK_BUCKET.cost() == 1, "for 1 elixir");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
