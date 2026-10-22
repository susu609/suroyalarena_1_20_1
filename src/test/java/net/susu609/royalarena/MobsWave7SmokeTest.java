package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.CostFormula;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.combat.ChildSpawn;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.combat.TickResult.TribeEvent.Kind;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Batch 5: Giant, Villager, Sniffer, Wandering Trader, and the Anvil,
 * Dragon's Breath and Bell spells. Units stand still (only
 * {@code engine.tick()}), out of every tower's reach.
 */
public final class MobsWave7SmokeTest {
    private MobsWave7SmokeTest() {
    }

    public static void run() {
        giantSplashes();
        villagersCallTheirGolem();
        zombiesGoForVillagersAndTurnThem();
        snifferDrawsFireAndHeals();
        traderBringsLlamasAndHides();
        dragonsBreathBlocksHealing();
        bellStunsLuresAndReveals();
        abilitiesCountInTheCost();
        System.out.println("RoyalArena wave 7 mobs smoke tests: PASS");
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

    /** A still shooter of no card, 6 blocks of range. */
    private static CombatUnit shooter(CombatEngine engine, TeamSide side,
            double damage, int interval, double x, double z)
    {
        return engine.add(UUID.randomUUID(), side,
                new TroopStats(1, 200, damage, interval, 6.0D, 0.0D, 0.3D),
                x, z);
    }

    private static boolean has(TickResult r, Kind kind) {
        return r.tribeEvents().stream().anyMatch(e -> e.kind() == kind);
    }

    private static void giantSplashes() {
        CombatEngine engine = new CombatEngine(battle());
        unit(engine, BattleCard.GIANT, TeamSide.BLUE, 0.0D, -3.0D);
        CombatUnit a = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, -0.4D,
                -4.6D);
        CombatUnit b = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.4D,
                -4.6D);
        for (int t = 0; t < 3; t++)
            engine.tick();
        double hit = 12.0D * a.stats().armorFactor();
        check(Math.abs(a.health() - (20.0D - hit)) < 0.01D &&
                Math.abs(b.health() - (20.0D - hit)) < 0.01D,
                "one Giant slam hits both zombies: " + a.health() + ", " +
                        b.health());
    }

    private static void villagersCallTheirGolem() {
        CombatEngine engine = new CombatEngine(battle());
        for (int i = 0; i < 3; i++)
            unit(engine, BattleCard.VILLAGER, TeamSide.BLUE, -1.0D + i, 2.2D);
        ChildSpawn golem = null;
        boolean called = false;
        for (int t = 0; t < BattleCard.VILLAGER_CALL_TICKS + 2; t++) {
            TickResult r = engine.tick();
            called |= has(r, Kind.GOLEM_CALL);
            for (ChildSpawn spawn : r.spawns())
                golem = spawn;
        }
        check(called && golem != null &&
                golem.stats() == BattleCard.IRON_GOLEM.stats() &&
                Math.abs(golem.health() - 100.0D *
                        BattleCard.CALLED_GOLEM_HEALTH) < 0.01D &&
                golem.side() == TeamSide.BLUE,
                "three standing Villagers call a half-health Iron Golem");
        CombatUnit added = engine.add(UUID.randomUUID(), golem);
        engine.tick();
        check(added.alive() && added.maxHealth() < 100.0D, "the Golem stands");

        // With only one Villager left, nobody comes.
        CombatEngine lonely = new CombatEngine(battle());
        List<CombatUnit> three = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            three.add(unit(lonely, BattleCard.VILLAGER, TeamSide.BLUE,
                    -1.0D + i, 2.2D));
        lonely.remove(three.get(0).id());
        lonely.remove(three.get(1).id());
        boolean came = false;
        for (int t = 0; t < BattleCard.VILLAGER_CALL_TICKS + 2; t++)
            came |= !lonely.tick().spawns().isEmpty();
        check(!came, "a lone Villager calls no Golem");
    }

    private static void zombiesGoForVillagersAndTurnThem() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.9D,
                -2.2D);
        CombatUnit archer = unit(engine, BattleCard.SKELETON_ARCHER,
                TeamSide.BLUE, 1.5D, -2.2D);
        CombatUnit villager = unit(engine, BattleCard.VILLAGER, TeamSide.BLUE,
                0.0D, -2.2D);
        engine.tick();
        check(villager.id().equals(zombie.target()),
                "the Zombie goes for the Villager, not the nearer archer");
        engine.remove(archer.id());
        ChildSpawn risen = null;
        boolean turned = false;
        for (int t = 0; t < 200 && villager.alive(); t++) {
            TickResult r = engine.tick();
            turned |= has(r, Kind.CONVERT);
            for (ChildSpawn spawn : r.spawns())
                risen = spawn;
        }
        check(!villager.alive() && turned && risen != null &&
                risen.side() == TeamSide.RED &&
                risen.stats() == BattleCard.ZOMBIE_VILLAGER.stats(),
                "a Villager a Zombie kills rises as a RED Zombie Villager");

        // A Villager killed by anything else stays dead.
        CombatEngine other = new CombatEngine(battle());
        CombatUnit v = unit(other, BattleCard.VILLAGER, TeamSide.BLUE, 0.0D,
                2.2D);
        shooter(other, TeamSide.RED, 30.0D, 20, 0.0D, -2.2D);
        boolean anyRise = false;
        for (int t = 0; t < 60 && v.alive(); t++)
            anyRise |= !other.tick().spawns().isEmpty();
        check(!v.alive() && !anyRise, "an arrow's Villager does not rise");
    }

    private static void snifferDrawsFireAndHeals() {
        CombatEngine engine = new CombatEngine(battle());
        unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE, 1.0D, 2.2D);
        CombatUnit sniffer = unit(engine, BattleCard.SNIFFER, TeamSide.BLUE,
                -2.0D, 2.2D);
        CombatUnit enemy = shooter(engine, TeamSide.RED, 1.0D, 400, 0.0D,
                -2.2D);
        engine.tick();
        check(sniffer.id().equals(enemy.target()),
                "the shooter goes for the Sniffer, not the nearer Zombie");

        CombatEngine heal = new CombatEngine(battle());
        CombatUnit golem = unit(heal, BattleCard.IRON_GOLEM, TeamSide.BLUE,
                3.0D, 2.2D);
        unit(heal, BattleCard.SNIFFER, TeamSide.BLUE, 4.6D, 2.2D);
        heal.castSpell(TeamSide.RED, BattleCard.ANVIL, 3.0D, 2.2D);
        heal.tick();
        double crushed = 45.0D * (0.5D + 0.5D * golem.armorFactor());
        check(Math.abs(golem.health() - (100.0D - crushed)) < 0.01D &&
                golem.stunned(), "the Anvil crushes the Golem through half its armour and stuns it: " +
                        golem.health());
        boolean flower = false;
        for (int t = 1; t < BattleCard.SNIFFER.stats().attackTicks(); t++)
            flower |= has(heal.tick(), Kind.FLOWER);
        check(flower && Math.abs(golem.health() -
                        (100.0D - crushed + 4.0D)) < 0.01D,
                "the Sniffer's flower heals the Golem by 4: " + golem.health());
    }

    private static void traderBringsLlamasAndHides() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit trader = unit(engine, BattleCard.WANDERING_TRADER,
                TeamSide.BLUE, 0.0D, 2.2D);
        TickResult first = engine.tick();
        long llamas = first.spawns().stream()
                .filter(s -> s.stats() == BattleCard.TRADER_LLAMA).count();
        check(llamas == BattleCard.TRADER_LLAMAS,
                "the Wandering Trader brings two Trader Llamas: " + llamas);
        CombatUnit enemy = shooter(engine, TeamSide.RED, 2.0D, 10, 0.0D,
                -2.2D);
        boolean hid = false;
        for (int t = 0; t < 30 && !hid; t++)
            hid = has(engine.tick(), Kind.HIDE);
        check(hid && trader.hidden() && trader.alive(),
                "hurt once, the Trader drinks invisibility");
        engine.tick();
        check(enemy.target() == null, "nobody can pick the invisible Trader");
        engine.castSpell(TeamSide.RED, BattleCard.BELL, 0.0D, 2.2D);
        boolean revealed = false;
        for (int t = 0; t < 2; t++)
            revealed |= has(engine.tick(), Kind.REVEAL);
        check(revealed && !trader.hidden(), "a Bell shows the Trader again");
    }

    private static void dragonsBreathBlocksHealing() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit golem = unit(engine, BattleCard.IRON_GOLEM, TeamSide.BLUE,
                0.0D, 2.2D);
        engine.castSpell(TeamSide.RED, BattleCard.DRAGONS_BREATH, 0.0D, 2.2D);
        engine.tick();
        check(golem.healBlocked(), "inside Dragon's Breath nothing heals");
        engine.castSpell(TeamSide.BLUE, BattleCard.HEALING_POTION, 0.0D, 2.2D);
        double before = golem.health();
        engine.tick();
        check(golem.health() <= before, "the Healing Potion does nothing in the breath");
        for (int t = 0; t < 120; t++)
            engine.tick();
        double total = 10 * 2.0D * (0.5D + 0.5D * golem.armorFactor());
        check(Math.abs(golem.health() - (100.0D - total)) < 0.5D,
                "ten pulses through half the armour: " + golem.health());
    }

    private static void bellStunsLuresAndReveals() {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 2.0D,
                -3.0D);
        CombatUnit warden = unit(engine, BattleCard.WARDEN, TeamSide.RED, 10.0D,
                -6.0D);
        CombatUnit far = unit(engine, BattleCard.ZOMBIE, TeamSide.RED, -12.0D,
                -6.0D);
        engine.castSpell(TeamSide.BLUE, BattleCard.BELL, 0.0D, -3.0D);
        engine.tick();
        check(zombie.stunned(), "the Bell stuns the Zombie in its circle");
        check(zombie.lured() && warden.lured() && !far.lured(),
                "the Zombie and the Warden come to the bell, a far Zombie does not");
        for (int t = 0; t < BattleCard.BELL.spell().stunTicks(); t++)
            engine.tick();
        TickResult r = engine.tick();
        boolean walks = r.moves().stream()
                .anyMatch(m -> m.unit().equals(zombie.id()) && m.x() < 2.0D);
        check(walks && zombie.target() == null,
                "after the stun the Zombie walks to the bell, attacking nothing");
    }

    private static void abilitiesCountInTheCost() {
        check(CostFormula.raw(BattleCard.VILLAGER) >
                        CostFormula.raw(BattleCard.VILLAGER.stats()),
                "the called Golem counts in the Villager's cost");
        check(CostFormula.raw(BattleCard.WANDERING_TRADER) > 1.0D,
                "the Trader's llamas make its cost");
        check(CostFormula.raw(BattleCard.SNIFFER) >
                        CostFormula.raw(BattleCard.SNIFFER.stats()),
                "drawing fire counts in the Sniffer's cost");
        for (BattleCard spell : new BattleCard[] {BattleCard.ANVIL,
                BattleCard.DRAGONS_BREATH, BattleCard.BELL})
            check(CostFormula.raw(spell) > CostFormula.raw(spell.spell()),
                    spell.id() + "'s rule counts in its cost");
    }

    private static void check(boolean condition, String name) {
        if (!condition)
            throw new AssertionError(name);
    }
}
