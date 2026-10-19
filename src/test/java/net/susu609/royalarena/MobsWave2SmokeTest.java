package net.susu609.royalarena;

import net.susu609.royalarena.arena.ArenaLayout;
import net.susu609.royalarena.arena.ArenaPoint;
import net.susu609.royalarena.card.BattleCard;
import net.susu609.royalarena.card.Body;
import net.susu609.royalarena.card.CardFilter;
import net.susu609.royalarena.card.CardTactic;
import net.susu609.royalarena.card.TroopStats;
import net.susu609.royalarena.card.TroopTraits;
import net.susu609.royalarena.combat.CombatEngine;
import net.susu609.royalarena.combat.CombatUnit;
import net.susu609.royalarena.combat.TickResult;
import net.susu609.royalarena.match.MatchInstance;
import net.susu609.royalarena.match.TeamSide;
import net.susu609.royalarena.tower.TowerId;

import java.util.UUID;

/**
 * Pure-Java checks for the second wave of mobs: the Polar Bear shrugs off
 * frost, Evoker fangs bite along a line, the Guardian locks on before its
 * laser and hits harder from the river, a Bee stings again and again,
 * poison, the Phantom flies straight for a tower, and every card has
 * hand-written tactics.
 */
public final class MobsWave2SmokeTest {
    private MobsWave2SmokeTest() {
    }

    public static void run() {
        polarBearIgnoresFrost();
        evokerFangsBiteAlongALine();
        guardianLocksOnFirst();
        guardianHitsHarderFromTheRiver();
        beeStingsAgainAndPoisons();
        phantomGoesStraightForATower();
        everyCardHasATactic();
        libraryFiltersAndPages();
        snowballsAreFrost();
        netherMobsDoNotBurn();
        poisonDependsOnTheBody();
        spiderJockeyLeavesItsArcher();
        foxesAreAFastCheapPack();
        skeletonRiderIgnoresTroops();
        striderLeavesAFireTrail();
        llamaSpitsAndSlowsALittle();
        piglinFiresTwoThenReloads();
        System.out.println("RoyalArena wave 2 mobs smoke tests: PASS");
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

    /** A target that never hits back and stands still. */
    private static CombatUnit dummy(CombatEngine engine, TeamSide side,
            double x, double z)
    {
        return engine.add(UUID.randomUUID(), side,
                new TroopStats(1, 200, 0.0D, 100000, 1.2D, 0.0D, 0.3D), x, z);
    }

    private static void piglinFiresTwoThenReloads() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit piglin = unit(engine, BattleCard.PIGLIN, TeamSide.BLUE,
                0.0D, 3.0D);
        dummy(engine, TeamSide.RED, 0.0D, -1.5D);
        java.util.List<Integer> shots = new java.util.ArrayList<>();
        for (int tick = 1; tick <= 140; tick++) {
            for (TickResult.Attack a : engine.tick().attacks()) {
                if (piglin.id().equals(a.attackerUnit()))
                    shots.add(tick);
            }
        }
        int gap = net.susu609.royalarena.card.TroopTraits.BURST_GAP_TICKS;
        int reload = BattleCard.PIGLIN.stats().attackTicks();
        check(shots.size() >= 4, "two bursts in 7 s: " + shots);
        check(shots.get(1) - shots.get(0) == gap,
                "the second bolt follows quickly: " + shots);
        check(shots.get(2) - shots.get(1) == reload,
                "then the crossbow reloads: " + shots);
        check(BattleCard.PIGLIN.fireImmune(), "a Nether mob");
    }

    private static void llamaSpitsAndSlowsALittle() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        unit(engine, BattleCard.LLAMA, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                0.0D, -1.5D);
        boolean spat = false;
        for (int i = 0; i < 20 && !zombie.slowed(); i++) {
            for (TickResult.Attack a : engine.tick().attacks())
                spat |= a.ranged();
        }
        check(spat, "the Llama spits across the river");
        check(zombie.slowed() && zombie.health() < zombie.maxHealth(),
                "the spit lands and slows");
        check(BattleCard.LLAMA.stats().traits().slowTicks() <
                        BattleCard.STRAY.stats().traits().slowTicks() &&
                        BattleCard.LLAMA.stats().range() >= 4.0D &&
                        BattleCard.LLAMA.stats().range() <= 5.0D,
                "spits from 4-5 blocks and slows a little, less than a Stray");
    }

    private static void striderLeavesAFireTrail() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit strider = unit(engine, BattleCard.STRIDER, TeamSide.BLUE,
                0.0D, 3.2D);
        CombatUnit victim = dummy(engine, TeamSide.RED, 0.3D, 3.2D);
        CombatUnit blaze = unit(engine, BattleCard.BLAZE, TeamSide.RED,
                -0.3D, 3.2D);
        CombatUnit friend = dummy(engine, TeamSide.BLUE, 0.0D, 3.6D);
        boolean burning = false;
        for (int i = 0; i < 12 && !burning; i++) {
            engine.tick();
            burning = victim.burnTicks() > 0;
        }
        check(burning && !engine.firePatches().isEmpty(),
                "the Strider drops a burning patch; an enemy on it burns");
        check(blaze.burnTicks() == 0, "a Nether mob does not burn on it");
        check(friend.burnTicks() == 0, "nor does its own side");
        check(BattleCard.STRIDER.fireImmune() &&
                        !BattleCard.STRIDER.stats().traits().crossesRiver(),
                "a fire-immune Nether mob that still needs the bridges");
        for (int i = 0; i < 60; i++)
            engine.tick();
        check(strider.alive(), "the dummy does not hit back");
    }

    private static void skeletonRiderIgnoresTroops() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit rider = unit(engine, BattleCard.SKELETON_RIDER,
                TeamSide.BLUE, -6.0D, -5.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.RED,
                -6.0D, -6.5D);
        boolean hitTroop = false;
        boolean hitTower = false;
        for (int i = 0; i < 200; i++) {
            for (TickResult.Attack a : engine.tick().attacks()) {
                if (!rider.id().equals(a.attackerUnit()))
                    continue;
                hitTroop |= a.victimUnit() != null;
                hitTower |= a.victimTower() != null;
            }
        }
        check(!hitTroop, "the Skeleton Rider never shoots a troop");
        check(hitTower, "it shoots the tower from its saddle");
        var s = BattleCard.SKELETON_RIDER.stats();
        check(s.range() < 7.0D && s.health() < BattleCard.HOGLIN.stats().health()
                        && BattleCard.SKELETON_RIDER.body() == Body.UNDEAD,
                "a fragile undead archer that stays out-ranged by towers");
    }

    private static void foxesAreAFastCheapPack() {
        TroopStats fox = BattleCard.FOX.stats();
        check(fox.count() == 2 && BattleCard.FOX.cost() == 2,
                "Fox: a pack of two (10 health each, was three of 6) for 2 elixir");
        for (BattleCard card : BattleCard.values()) {
            if (!card.isSpell() && card != BattleCard.FOX &&
                    !card.stats().traits().selfDestruct() &&
                    card.stats().traits().hover() == 0.0D)
                check(fox.speed() >= card.stats().speed(),
                        "foxes outrun every walking fighter: " + card.id());
        }
        check(fox.traits().equals(TroopTraits.NONE),
                "and have no special rule (no armour piercing)");
    }

    private static void spiderJockeyLeavesItsArcher() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit jockey = unit(engine, BattleCard.SPIDER_JOCKEY,
                TeamSide.BLUE, 0.0D, 6.0D);
        check(BattleCard.SPIDER_JOCKEY.body() == Body.LIVING,
                "the Spider Jockey is living flesh");
        TickResult.Death death = null;
        for (int cast = 0; cast < 3 && death == null; cast++) {
            engine.castSpell(TeamSide.RED, BattleCard.TNT, jockey.x(),
                    jockey.z());
            for (TickResult.Death d : engine.tick().deaths()) {
                if (d.unit().equals(jockey.id()))
                    death = d;
            }
        }
        check(death != null, "the Spider Jockey dies to TNT");
        check(death.children().size() == 1,
                "it leaves exactly one unit: " + death.children().size());
        var rider = death.children().get(0);
        check(rider.stats() == BattleCard.SKELETON_ARCHER.stats() &&
                BattleCard.forStats(rider.stats()) ==
                        BattleCard.SKELETON_ARCHER,
                "the rider is a Skeleton Archer");
        check(rider.health() == 20.0D && rider.side() == TeamSide.BLUE &&
                rider.generation() == 1 && rider.invulnerableTicks() > 0,
                "full health, same side, a short moment of safety");
        CombatUnit archer = engine.add(UUID.randomUUID(), rider);
        check(archer.stats().range() ==
                        BattleCard.SKELETON_ARCHER.stats().range() &&
                        archer.stats().split() == null,
                "the archer shoots like a Skeleton Archer and never splits again");
    }

    private static void polarBearIgnoresFrost() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit bear = unit(engine, BattleCard.POLAR_BEAR, TeamSide.BLUE,
                0.5D, 2.5D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                2.0D, 2.5D);
        engine.castSpell(TeamSide.RED, BattleCard.FREEZE, 1.0D, 2.5D);
        engine.tick();
        double freezeDamage = BattleCard.FREEZE.spell().damagePerPulse();
        near(bear.health(), BattleCard.POLAR_BEAR.stats().health() -
                freezeDamage, "Freeze still hurts the Polar Bear");
        check(!bear.frozen(), "but does not freeze it");
        check(zombie.frozen(), "a Zombie next to it is frozen");

        MatchInstance shot = battle();
        CombatEngine shotEngine = new CombatEngine(shot);
        CombatUnit target = unit(shotEngine, BattleCard.POLAR_BEAR,
                TeamSide.BLUE, -6.0D, 6.0D);
        unit(shotEngine, BattleCard.STRAY, TeamSide.RED, -6.0D, 1.5D);
        boolean hit = false;
        for (int i = 0; i < 60 && !hit; i++)
            hit = shotEngine.tick().landed().stream().anyMatch(
                    a -> target.id().equals(a.victimUnit()));
        check(hit, "the Stray's arrow lands");
        check(!target.slowed(), "and does not slow the Polar Bear");
    }

    private static void evokerFangsBiteAlongALine() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        // The Evoker on its half, three dummies in a row in front of it, one
        // to the side. Out of every tower's reach.
        unit(engine, BattleCard.EVOKER, TeamSide.BLUE, 0.0D, 3.0D);
        CombatUnit first = dummy(engine, TeamSide.RED, 0.0D, 0.5D);
        CombatUnit behind = dummy(engine, TeamSide.RED, 0.0D, -1.0D);
        CombatUnit beside = dummy(engine, TeamSide.RED, 2.5D, 0.5D);
        double bite = BattleCard.EVOKER.stats().damage();
        for (int i = 0; i < 5; i++)
            engine.tick();
        near(first.health(), 200.0D - bite, "the target is bitten");
        near(behind.health(), 200.0D - bite, "and so is the one behind it");
        near(beside.health(), 200.0D, "the one beside the line is not");
    }

    private static void guardianLocksOnFirst() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit guardian = unit(engine, BattleCard.GUARDIAN, TeamSide.BLUE,
                0.0D, 3.0D);
        dummy(engine, TeamSide.RED, 0.0D, -0.5D);
        int charge = BattleCard.GUARDIAN.stats().traits().chargeTicks();
        int firstShot = -1;
        for (int tick = 1; tick <= charge + 10 && firstShot < 0; tick++) {
            TickResult r = engine.tick();
            if (r.attacks().stream().anyMatch(
                    a -> guardian.id().equals(a.attackerUnit())))
                firstShot = tick;
            else if (tick > 1)
                check(guardian.charging(), "locking on (tick " + tick + ")");
        }
        check(firstShot >= charge, "the first laser waits for the charge: " +
                firstShot + " >= " + charge);
    }

    private static void guardianHitsHarderFromTheRiver() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        unit(engine, BattleCard.GUARDIAN, TeamSide.BLUE, 0.5D, 0.5D);
        CombatUnit target = dummy(engine, TeamSide.RED, 0.5D, -3.0D);
        for (int i = 0; i < 60; i++)
            engine.tick();
        near(target.health(), 200.0D - 1.5D * BattleCard.GUARDIAN.stats()
                .damage(), "one laser from the river: 1.5x damage");
    }

    private static void beeStingsAgainAndPoisons() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit bee = engine.add(UUID.randomUUID(), TeamSide.BLUE,
                BattleCard.BEE.stats(), 0.0D, 2.6D);
        CombatUnit target = dummy(engine, TeamSide.RED, 0.0D, 1.5D);
        TickResult first = engine.tick();
        check(first.attacks().stream().anyMatch(
                a -> bee.id().equals(a.attackerUnit())), "the Bee stings");
        check(bee.alive(), "and lives on (no more dying on the first sting)");
        check(target.poisoned(), "the sting poisons");
        int stings = 1;
        for (int i = 0; i < 100; i++) {
            stings += (int) engine.tick().attacks().stream().filter(
                    a -> bee.id().equals(a.attackerUnit())).count();
        }
        check(stings >= 5, "it keeps stinging, once a second: " + stings);
        check(target.poisoned(), "and keeps the target poisoned");
    }

    private static void phantomGoesStraightForATower() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit phantom = unit(engine, BattleCard.PHANTOM, TeamSide.BLUE,
                0.5D, 10.0D);
        unit(engine, BattleCard.ZOMBIE, TeamSide.RED, 0.5D, 8.5D);
        TickResult r = engine.tick();
        TickResult.Move move = r.moves().stream().filter(
                m -> m.unit().equals(phantom.id())).findFirst().orElse(null);
        check(move != null, "the Phantom flies on past the Zombie");
        ArenaPoint tower = match.arena().towerPoint(TowerId.RED_LEFT);
        near(move.x(), tower.x(), "straight for the tower");
        near(move.z(), tower.z(), "over the river");
    }

    private static void everyCardHasATactic() {
        for (BattleCard card : BattleCard.values())
            check(!card.tactics().isEmpty(), card.id() + " has a tactic");
        check(BattleCard.HEALING_POTION.tactics().contains(CardTactic.SUPPORT),
                "a healing potion supports");
    }

    private static void snowballsAreFrost() {
        MatchInstance match = battle();
        CombatEngine engine = new CombatEngine(match);
        CombatUnit golem = unit(engine, BattleCard.SNOW_GOLEM, TeamSide.BLUE,
                0.0D, 4.0D);
        CombatUnit blaze = engine.add(UUID.randomUUID(), TeamSide.RED,
                BattleCard.BLAZE.stats(), 0.0D, -1.5D);
        CombatUnit zombie = dummy(engine, TeamSide.RED, 0.0D, -1.5D);
        TickResult.Attack shot = null;
        for (int i = 0; i < 40 && shot == null; i++) {
            for (TickResult.Attack a : engine.tick().landed()) {
                if (golem.id().equals(a.attackerUnit()))
                    shot = a;
            }
        }
        check(shot != null, "the Snow Golem's snowball lands");
        CombatUnit hit = blaze.id().equals(shot.victimUnit()) ? blaze : zombie;
        double bite = BattleCard.SNOW_GOLEM.stats().damage();
        double expected = hit == blaze ? 2.0D * bite : bite;
        double max = hit == blaze ? BattleCard.BLAZE.stats().health() : 200.0D;
        check(Math.abs(max - hit.health() - expected) < 1.0E-6D,
                "frost does double to a Blaze only: " + (max - hit.health()));
        check(hit.slowed(), "and slows");
        // Make sure the Blaze case is seen: shoot it directly.
        CombatEngine solo = new CombatEngine(battle());
        CombatUnit g2 = unit(solo, BattleCard.SNOW_GOLEM, TeamSide.BLUE,
                0.0D, 4.0D);
        CombatUnit b2 = solo.add(UUID.randomUUID(), TeamSide.RED,
                BattleCard.BLAZE.stats(), 0.0D, -1.5D);
        boolean landed = false;
        for (int i = 0; i < 40 && !landed; i++)
            landed = solo.tick().landed().stream().anyMatch(
                    a -> g2.id().equals(a.attackerUnit()));
        check(landed, "a snowball lands on the Blaze");
        check(BattleCard.BLAZE.stats().health() - b2.health() >=
                2.0D * bite - 1.0E-6D, "the Blaze takes double");
    }

    /** Damage a victim card really loses to one Bee sting and its poison. */
    private static double lostToBee(BattleCard victim) {
        CombatEngine engine = new CombatEngine(battle());
        CombatUnit bee = unit(engine, BattleCard.BEE, TeamSide.BLUE, 0.0D,
                2.6D);
        CombatUnit target = engine.add(UUID.randomUUID(), TeamSide.RED,
                victim.stats(), 0.0D, 1.5D);
        double before = target.health();
        engine.tick();
        // Take the Bee away after its first sting, so only that one counts.
        engine.remove(bee.id());
        for (int i = 0; i < 100; i++)
            engine.tick();
        return before - target.health();
    }

    private static void poisonDependsOnTheBody() {
        double sting = BattleCard.BEE.stats().damage();
        double poison = BattleCard.BEE.stats().traits().poisonTotal();
        // A Spider is flesh (x1.5), a Zombie undead (x1), a Golem a construct.
        near(lostToBee(BattleCard.SPIDER), sting + 1.5D * poison,
                "living flesh takes 1.5x the poison");
        near(lostToBee(BattleCard.ZOMBIE),
                sting * BattleCard.ZOMBIE.stats().armorFactor() + poison,
                "the undead take it as it is (the sting less the zombie's " +
                        "armour; poison goes through it)");
        near(lostToBee(BattleCard.IRON_GOLEM), sting,
                "a golem takes the sting and no poison");
        CombatEngine engine = new CombatEngine(battle());
        unit(engine, BattleCard.BEE, TeamSide.BLUE, 0.0D, 2.6D);
        CombatUnit golem = engine.add(UUID.randomUUID(), TeamSide.RED,
                BattleCard.IRON_GOLEM.stats(), 0.0D, 1.5D);
        engine.tick();
        check(!golem.poisoned(), "a golem is never poisoned");
    }

    private static void netherMobsDoNotBurn() {
        CombatEngine engine = new CombatEngine(battle());
        // A Hoglin (fire immune, 40 health): the Ghast now has 10 health
        // and a Fireball would kill it before it could show it does not burn.
        CombatUnit hoglin = unit(engine, BattleCard.HOGLIN, TeamSide.BLUE,
                -4.0D, 3.0D);
        CombatUnit zombie = unit(engine, BattleCard.ZOMBIE, TeamSide.BLUE,
                4.0D, 3.0D);
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, -4.0D, 3.0D);
        engine.castSpell(TeamSide.RED, BattleCard.FIREBALL, 4.0D, 3.0D);
        engine.tick();
        near(hoglin.health(), BattleCard.HOGLIN.stats().health() - 25.0D,
                "the Fireball still hurts a Hoglin");
        near(zombie.health(), 0.0D + Math.max(0.0D, 20.0D -
                        25.0D * BattleCard.ZOMBIE.stats().armorFactor()),
                "and the Zombie (less its armour, still a kill)");
        check(hoglin.burnTicks() == 0, "but the Hoglin does not burn");
        for (BattleCard card : BattleCard.values()) {
            if (card.fireImmune())
                check(!card.isSpell(), card.id() + " is a troop");
        }
    }

    private static void libraryFiltersAndPages() {
        var none = java.util.EnumSet.noneOf(CardFilter.Chip.class);
        var all = CardFilter.shown(CardFilter.Tab.ALL, none, false);
        check(all.size() == BattleCard.values().length, "All shows every card");
        check(CardFilter.shown(CardFilter.Tab.SPELLS, none, false).stream()
                .allMatch(BattleCard::isSpell), "Spells shows only spells");
        check(CardFilter.shown(CardFilter.Tab.BUILDINGS, none, false)
                .isEmpty(), "no building cards yet");
        var support = CardFilter.shown(CardFilter.Tab.ALL,
                java.util.EnumSet.of(CardFilter.Chip.SUPPORT), false);
        check(support.contains(BattleCard.HEALING_POTION) &&
                !support.contains(BattleCard.ZOMBIE), "the Support chip");
        var tankAttack = CardFilter.shown(CardFilter.Tab.TROOPS,
                java.util.EnumSet.of(CardFilter.Chip.ATTACK,
                        CardFilter.Chip.TANK), false);
        check(tankAttack.contains(BattleCard.RAVAGER) &&
                !tankAttack.contains(BattleCard.SPIDER),
                "chips must all match: " + tankAttack);
        var cheap = CardFilter.shown(CardFilter.Tab.ALL, none, true);
        for (int i = 1; i < cheap.size(); i++)
            check(cheap.get(i - 1).cost() <= cheap.get(i).cost(),
                    "sorted by elixir");
        int pages = CardFilter.pages(all.size());
        check(pages == (all.size() + 15) / 16, "16 cards a page");
        int seen = 0;
        for (int p = 0; p < pages; p++)
            seen += CardFilter.page(all, p).size();
        check(seen == all.size(), "the pages hold every card once");
        check(CardFilter.page(all, 99).equals(CardFilter.page(all, pages - 1)),
                "a page past the end shows the last");
        check(CardFilter.pages(0) == 1 &&
                CardFilter.page(java.util.List.of(), 0).isEmpty(),
                "an empty filter is one empty page");
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
