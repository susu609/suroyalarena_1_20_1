package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CostFormula;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.UUID;

/**
 * Batch 3: buildings (Shulker, End Crystal) and the Warden.
 */
public final class MobsWave5SmokeTest {
    private MobsWave5SmokeTest() {
    }

    public static void run() {
        buildingsStandStillAndDecay();
        towerTroopsGoForBuildings();
        shulkerLiftsAndHidesInItsShell();
        endCrystalBlowsUpBothSides();
        wardenHearsOnlySounds();
        abilitiesCountInTheCost();
        System.out.println("RoyalArena wave 5 mobs smoke tests: PASS");
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

    private static void buildingsStandStillAndDecay() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit shulker = unit(engine, BattleCard.SHULKER, TeamSide.BLUE,
                0.0D, 1.0D);
        for (int t = 0; t < BattleCard.BUILDING_LIFETIME_TICKS / 2; t++) {
            TickResult r = engine.tick();
            check(r.moves().stream().noneMatch(m ->
                            m.unit().equals(shulker.id()) && m.speed() > 0.0D),
                    "a building never walks");
        }
        check(Math.abs(shulker.health() - shulker.maxHealth() / 2.0D) < 0.01D,
                "half its health gone in half its lifetime: " +
                        shulker.health());
        for (int t = 0; t < BattleCard.BUILDING_LIFETIME_TICKS / 2 + 2; t++)
            engine.tick();
        check(!shulker.alive(), "gone after 30 s");
    }

    private static void towerTroopsGoForBuildings() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit hoglin = unit(engine, BattleCard.HOGLIN, TeamSide.BLUE,
                -6.0D, -1.0D);
        CombatUnit crystal = unit(engine, BattleCard.END_CRYSTAL, TeamSide.RED,
                -6.0D, -4.0D);
        unit(engine, BattleCard.ZOMBIE, TeamSide.RED, -5.0D, -2.0D);
        engine.tick();
        check(crystal.id().equals(hoglin.target()),
                "a Hoglin goes for the End Crystal, not the Zombie");
    }

    private static void shulkerLiftsAndHidesInItsShell() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit shulker = unit(engine, BattleCard.SHULKER, TeamSide.BLUE,
                0.0D, 4.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.0D,
                -0.5D);
        boolean lifted = false;
        for (int t = 0; t < 40 && !lifted; t++) {
            TickResult r = engine.tick();
            lifted = r.tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.LEVITATE &&
                            e.unit().equals(zombie.id()));
        }
        check(lifted && zombie.frozen(), "its bullet lifts the Zombie");

        // Shut: a Fireball takes off only 60% of its damage.
        CombatEngine shut = new CombatEngine(battle());
        CombatUnit closed = unit(shut, BattleCard.SHULKER, TeamSide.BLUE, 0.0D,
                4.0D);
        shut.castSpell(TeamSide.RED, BattleCard.FIREBALL, 0.0D, 4.0D);
        shut.tick();
        double lost = closed.maxHealth() - closed.health();
        check(lost > 25.0D * BattleCard.SHELL_FACTOR - 0.5D &&
                        lost < 25.0D * BattleCard.SHELL_FACTOR + 0.5D,
                "a shut Shulker takes 60% of a Fireball: " + lost);
    }

    private static void endCrystalBlowsUpBothSides() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit crystal = unit(engine, BattleCard.END_CRYSTAL, TeamSide.BLUE,
                0.0D, 4.0D);
        CombatUnit own = unit(engine, BattleCard.IRON_GOLEM, TeamSide.BLUE,
                1.5D, 4.0D);
        CombatUnit enemy = unit(engine, BattleCard.RAVAGER, TeamSide.RED,
                -1.5D, 4.0D);
        engine.castSpell(TeamSide.RED, BattleCard.ZAP, 0.0D, 4.0D);
        boolean blew = false;
        for (int t = 0; t < 3 && !blew; t++)
            blew = engine.tick().tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.BLAST);
        check(!crystal.alive() && blew, "a Zap breaks the crystal and it blows");
        check(enemy.health() < enemy.maxHealth() - 20.0D,
                "the blast hurts the enemy: " + enemy.health());
        check(own.health() < own.maxHealth() - 15.0D,
                "and its own side too: " + own.health());
    }

    private static void wardenHearsOnlySounds() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit warden = unit(engine, BattleCard.WARDEN, TeamSide.BLUE,
                -6.0D, 1.0D);
        CombatUnit turtle = unit(engine, BattleCard.TURTLE, TeamSide.RED,
                -6.0D, -0.5D);
        engine.tick();
        check(warden.target() == null,
                "a Turtle that has not bitten yet makes no sound");
        CombatEngine noisy = new CombatEngine(battle());
        CombatUnit w2 = unit(noisy, BattleCard.WARDEN, TeamSide.BLUE, -6.0D,
                1.0D);
        CombatUnit archer = unit(noisy, BattleCard.SKELETON_ARCHER,
                TeamSide.RED, -6.0D, -3.0D);
        boolean heard = false;
        boolean boomed = false;
        for (int t = 0; t < 60; t++) {
            TickResult r = noisy.tick();
            heard |= archer.id().equals(w2.target());
            boomed |= r.tribeEvents().stream().anyMatch(e ->
                    e.kind() == TickResult.TribeEvent.Kind.SONIC_BOOM);
        }
        check(heard, "an archer shooting at it is heard");
        check(boomed, "and gets a Sonic Boom");
        check(turtle.alive(), "(the turtle is fine)");
    }

    private static void abilitiesCountInTheCost() {
        check(CostFormula.abilityModifier(BattleCard.WARDEN) > 1.3D &&
                        CostFormula.elixir(BattleCard.WARDEN) == 8,
                "the Warden's Sonic Boom and hearing make it 8 elixir");
        check(CostFormula.abilityModifier(BattleCard.ZOMBIE) == 1.0D,
                "plain cards have no ability modifier");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
